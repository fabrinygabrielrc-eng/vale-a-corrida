package app.valeacorrida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * O caminho de cada leitura, sem o conteúdo da tela: se o serviço está rodando, se o aplicativo
 * de corrida avisa quando a tela muda, quanto texto chegou e o que a leitura por imagem devolveu.
 *
 * Existe para responder "em que ponto a oferta se perdeu?" quando o cartão não aparece e nenhuma
 * leitura foi guardada. Só é escrito com "Guardar as últimas leituras" ligado, e só no aparelho.
 */
object Rastro {
    private const val CONEXOES = 10
    private const val PASSOS = 40

    /** O serviço lê a tela a cada segundo: gravar o arquivo a cada passo gastaria o aparelho à toa. */
    private const val ENTRE_GRAVACOES_MS = 10_000L

    /** Uma leitura. `texto` e `imagem` dizem quanto chegou por cada caminho, nunca o que estava escrito. */
    class Passo(
        val quando: Long,
        val aplicativo: String,
        val texto: String,
        val imagem: String,
        val reconhecida: Boolean,
        /** Quantas leituras seguidas deram exatamente isto, e a hora da última. */
        var vezes: Int = 1,
        var ate: Long = quando,
    ) {
        fun igual(outro: Passo) = aplicativo == outro.aplicativo && texto == outro.texto &&
            imagem == outro.imagem && reconhecida == outro.reconhecida
    }

    private class Avisos(var total: Long = 0, var ultimo: Long = 0)

    private class Dados(
        val conexoes: MutableList<Long> = mutableListOf(),
        val avisos: MutableMap<String, Avisos> = linkedMapOf(),
        val passos: MutableList<Passo> = mutableListOf(),
    )

    private var dados: Dados? = null
    private var gravadoEm = 0L

    /** O serviço de acessibilidade acabou de ser ligado pelo Android. */
    @Synchronized
    fun servicoLigou(contexto: Context, quando: Long) {
        val d = carregar(contexto)
        if (d.conexoes.firstOrNull() == quando) return
        d.conexoes.add(0, quando)
        while (d.conexoes.size > CONEXOES) d.conexoes.removeAt(d.conexoes.lastIndex)
        gravar(contexto)
    }

    /** O aplicativo de corrida avisou que a tela mudou. */
    @Synchronized
    fun aviso(contexto: Context, aplicativo: String, quando: Long) {
        val avisos = carregar(contexto).avisos.getOrPut(aplicativo, ::Avisos)
        avisos.total++
        avisos.ultimo = quando
        gravarSeJaPassou(contexto, quando)
    }

    @Synchronized
    fun passo(contexto: Context, passo: Passo) {
        val passos = carregar(contexto).passos
        val ultimo = passos.firstOrNull()
        if (ultimo != null && ultimo.igual(passo)) {
            ultimo.vezes++
            ultimo.ate = passo.quando
        } else {
            passos.add(0, passo)
            while (passos.size > PASSOS) passos.removeAt(passos.lastIndex)
        }
        gravarSeJaPassou(contexto, passo.quando)
    }

    @Synchronized
    fun vazio(contexto: Context): Boolean = carregar(contexto).let { it.conexoes.isEmpty() && it.avisos.isEmpty() && it.passos.isEmpty() }

    @Synchronized
    fun apagar(contexto: Context) {
        dados = Dados()
        arquivo(contexto).delete()
    }

    /** Esquece o que está na memória: a próxima consulta lê o arquivo, como depois de o aplicativo fechar. */
    @Synchronized
    internal fun soltar() {
        dados = null
    }

    /**
     * O rastro em frases. `servicoDesde` é a hora em que o serviço que está rodando agora foi
     * ligado, ou nulo se ele não está rodando. `quantosPassos` limita as leituras listadas.
     */
    @Synchronized
    fun linhas(contexto: Context, servicoDesde: Long?, quantosPassos: Int = PASSOS): List<String> = buildList {
        val d = carregar(contexto)
        add(
            if (servicoDesde != null) "O serviço de leitura está rodando desde ${hora(servicoDesde)}."
            else "O serviço de leitura não está rodando agora.",
        )
        if (d.conexoes.size > 1) {
            add("Ele foi ligado ${d.conexoes.size} vezes: ${d.conexoes.joinToString(", ", transform = ::hora)}.")
        }
        if (d.avisos.isEmpty()) {
            add("Nenhum aviso de mudança de tela dos aplicativos de corrida.")
        } else {
            for ((aplicativo, avisos) in d.avisos) {
                val quantos = if (avisos.total == 1L) "1 aviso" else "${avisos.total} avisos"
                add("$aplicativo: $quantos de mudança de tela, o último em ${hora(avisos.ultimo)}.")
            }
        }
        for (passo in d.passos.take(quantosPassos)) {
            val resultado = if (passo.reconhecida) "reconhecida" else "NÃO reconhecida"
            val repeticao = if (passo.vezes > 1) " | ${passo.vezes} vezes, até ${hora(passo.ate)}" else ""
            add("${hora(passo.quando)} ${passo.aplicativo} | texto: ${passo.texto} | imagem: ${passo.imagem} | $resultado$repeticao")
        }
    }

    /** Escreve o arquivo agora. O serviço chama ao ser desligado, para não perder o fim do rastro. */
    @Synchronized
    fun gravar(contexto: Context) {
        val d = dados ?: return
        val passos = JSONArray()
        for (p in d.passos) {
            passos.put(
                JSONObject().put("quando", p.quando).put("aplicativo", p.aplicativo).put("texto", p.texto)
                    .put("imagem", p.imagem).put("reconhecida", p.reconhecida).put("vezes", p.vezes).put("ate", p.ate),
            )
        }
        val avisos = JSONObject()
        for ((aplicativo, a) in d.avisos) avisos.put(aplicativo, JSONObject().put("total", a.total).put("ultimo", a.ultimo))
        val tudo = JSONObject().put("conexoes", JSONArray(d.conexoes)).put("avisos", avisos).put("passos", passos)
        try {
            arquivo(contexto).writeText(tudo.toString())
            gravadoEm = System.currentTimeMillis()
        } catch (_: Exception) {
            // Sem espaço no aparelho: o rastro é opcional, a leitura segue.
        }
    }

    private fun gravarSeJaPassou(contexto: Context, agora: Long) {
        if (agora - gravadoEm >= ENTRE_GRAVACOES_MS || agora < gravadoEm) gravar(contexto)
    }

    private fun hora(quando: Long): String =
        SimpleDateFormat("dd/MM HH:mm:ss", Locale.forLanguageTag("pt-BR")).format(Date(quando))

    private fun arquivo(contexto: Context) = File(contexto.filesDir, "rastro.json")

    private fun carregar(contexto: Context): Dados {
        dados?.let { return it }
        val d = Dados()
        try {
            val tudo = JSONObject(arquivo(contexto).readText())
            val conexoes = tudo.getJSONArray("conexoes")
            for (i in 0 until conexoes.length()) d.conexoes += conexoes.getLong(i)
            val avisos = tudo.getJSONObject("avisos")
            for (aplicativo in avisos.keys()) {
                val a = avisos.getJSONObject(aplicativo)
                d.avisos[aplicativo] = Avisos(a.getLong("total"), a.getLong("ultimo"))
            }
            val passos = tudo.getJSONArray("passos")
            for (i in 0 until passos.length()) {
                val p = passos.getJSONObject(i)
                d.passos += Passo(
                    p.getLong("quando"), p.getString("aplicativo"), p.getString("texto"), p.getString("imagem"),
                    p.getBoolean("reconhecida"), p.getInt("vezes"), p.getLong("ate"),
                )
            }
        } catch (_: Exception) {
            // Sem arquivo, ou arquivo estragado: começa do zero.
        }
        dados = d
        return d
    }
}
