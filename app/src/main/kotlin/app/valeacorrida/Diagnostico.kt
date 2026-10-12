package app.valeacorrida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * As últimas leituras da tela, guardadas só no aparelho e só quando a pessoa liga essa opção.
 *
 * Serve para consertar o leitor: quando uma oferta não é reconhecida, a pessoa compartilha
 * o texto lido e ele vira um teste novo em `LeitorDeOfertaTest`.
 */
object Diagnostico {
    private const val LIMITE = 20
    private const val LINHAS_POR_LEITURA = 80

    class Leitura(
        val quando: Long,
        val aplicativo: String,
        /** "texto" (acessibilidade) ou "imagem" (texto reconhecido na captura da tela). */
        val modo: String,
        val reconhecida: Boolean,
        val linhas: List<String>,
    )

    private var leituras: MutableList<Leitura>? = null

    @Synchronized
    fun listar(contexto: Context): List<Leitura> = carregar(contexto).toList()

    /** Guarda a leitura, a não ser que seja igual à anterior (a mesma tela é lida várias vezes). */
    @Synchronized
    fun registrar(contexto: Context, leitura: Leitura) {
        val lista = carregar(contexto)
        if (lista.firstOrNull()?.linhas == leitura.linhas) return
        lista.add(0, leitura)
        while (lista.size > LIMITE) lista.removeAt(lista.lastIndex)
        gravar(contexto, lista)
    }

    @Synchronized
    fun apagar(contexto: Context) {
        leituras = mutableListOf()
        arquivo(contexto).delete()
    }

    /** O texto que a pessoa compartilha. */
    fun texto(leituras: List<Leitura>, versao: String): String = buildString {
        appendLine("Vale a Corrida $versao - leituras para diagnóstico")
        appendLine("Antes de enviar, apague endereços e nomes que não queira mostrar.")
        val hora = SimpleDateFormat("dd/MM HH:mm:ss", Locale.forLanguageTag("pt-BR"))
        for (leitura in leituras) {
            appendLine()
            val resultado = if (leitura.reconhecida) "reconhecida" else "NÃO reconhecida"
            appendLine("--- ${hora.format(Date(leitura.quando))} | ${leitura.aplicativo} | por ${leitura.modo} | $resultado")
            leitura.linhas.forEach(::appendLine)
        }
    }

    private fun arquivo(contexto: Context) = File(contexto.filesDir, "leituras.json")

    private fun carregar(contexto: Context): MutableList<Leitura> {
        leituras?.let { return it }
        val lista = mutableListOf<Leitura>()
        try {
            val itens = JSONArray(arquivo(contexto).readText())
            for (i in 0 until itens.length()) {
                val item = itens.getJSONObject(i)
                val linhas = item.getJSONArray("linhas")
                lista += Leitura(
                    quando = item.getLong("quando"),
                    aplicativo = item.getString("aplicativo"),
                    modo = item.getString("modo"),
                    reconhecida = item.getBoolean("reconhecida"),
                    linhas = List(linhas.length(), linhas::getString),
                )
            }
        } catch (_: Exception) {
            // Sem arquivo, ou arquivo estragado: começa do zero.
        }
        leituras = lista
        return lista
    }

    private fun gravar(contexto: Context, lista: List<Leitura>) {
        val itens = JSONArray()
        for (leitura in lista) {
            itens.put(
                JSONObject()
                    .put("quando", leitura.quando)
                    .put("aplicativo", leitura.aplicativo)
                    .put("modo", leitura.modo)
                    .put("reconhecida", leitura.reconhecida)
                    .put("linhas", JSONArray(leitura.linhas.take(LINHAS_POR_LEITURA))),
            )
        }
        try {
            arquivo(contexto).writeText(itens.toString())
        } catch (_: Exception) {
            // Sem espaço no aparelho: o diagnóstico é opcional, a leitura segue.
        }
    }
}
