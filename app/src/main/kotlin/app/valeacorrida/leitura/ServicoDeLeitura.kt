package app.valeacorrida.leitura

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.valeacorrida.Diagnostico
import app.valeacorrida.Preferencias
import app.valeacorrida.Rastro
import app.valeacorrida.Registro
import app.valeacorrida.nucleo.Avaliador
import app.valeacorrida.nucleo.Fragmento
import app.valeacorrida.nucleo.LeitorDeOferta
import app.valeacorrida.nucleo.Linhas
import app.valeacorrida.nucleo.Oferta
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Trecho
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * O serviço de acessibilidade: quando um aplicativo de corrida muda a tela, lê o que está
 * escrito, procura uma oferta e mostra o cartão com as contas.
 *
 * Só olha os aplicativos que a pessoa escolheu. Não toca em nada: não aceita, não recusa.
 */
class ServicoDeLeitura : AccessibilityService() {
    private val principal = Handler(Looper.getMainLooper())
    private val emSegundoPlano: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var preferencias: Preferencias
    private lateinit var sobreposicao: Sobreposicao
    private var leitorDeImagem: LeitorDeImagem? = null

    private var pacotes: Set<String> = emptySet()
    private var pacoteDaVez: String? = null
    private var leituraMarcada = false
    private val vezDaImagem = VezDaImagem(principal, PRAZO_DA_CAPTURA_MS, PRAZO_DA_IMAGEM_MS)

    /** Leituras por imagem seguidas que o Android deixou sem resposta. */
    private var capturasPerdidas = 0
    private var ultimaLeitura = 0L
    private var ultimoAviso = 0L
    private var leiturasSemOferta = 0

    /** "Guardar as últimas leituras" está ligado: escreve o rastro e faz a ronda. */
    private var guardando = false
    private var rondando = false

    /** A leitura da vez foi puxada pela ronda, sem aviso do aplicativo de corrida. */
    private var semAviso = false

    /**
     * Aplicativos em que o texto da acessibilidade já entregou uma oferta. Neles a imagem da tela
     * só é lida de vez em quando, para o caso de algum tipo de oferta vir sem texto.
     */
    private val bastaOTexto = mutableSetOf<String>()
    private var leiturasSemImagem = 0

    private val aoMudarPreferencias = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> aplicarPreferencias() }
    private val lerAgora = Runnable(::ler)
    private val tirarCartao = Runnable { sobreposicao.esconder() }
    private val rondar = object : Runnable {
        override fun run() {
            if (!guardando && !Registro.ligado) {
                rondando = false
                return
            }
            conferirSemAviso()
            principal.postDelayed(this, RONDA_MS)
        }
    }

    /** Avisos de mudança de tela ainda não contados no registro, e quando saiu a última linha deles. */
    private var avisosPorContar = 0
    private var avisosContadosEm = 0L

    override fun onServiceConnected() {
        ServicoVivo.desde = System.currentTimeMillis()
        preferencias = Preferencias(this)
        sobreposicao = Sobreposicao(this)
        preferencias.escutar(aoMudarPreferencias)
        aplicarPreferencias()
        Registro.escrever {
            val info = serviceInfo
            "serviço ligado; escuta ${info?.packageNames?.joinToString() ?: "tudo"}; avisos ${info?.eventTypes}, opções ${info?.flags}"
        }
    }

    private fun aplicarPreferencias() {
        guardando = preferencias.guardarLeituras
        if (guardando) ServicoVivo.desde?.let { Rastro.servicoLigou(this, it) }
        // Com o registro pedido pelo cabo a ronda também roda: é ela que mostra o que o serviço
        // enxerga quando o aplicativo de corrida não avisa nada.
        if ((guardando || Registro.ligado) && !rondando) {
            rondando = true
            principal.postDelayed(rondar, RONDA_MS)
        }
        // O próprio Vale a Corrida entra na lista por causa do simulador.
        pacotes = preferencias.acompanhados.flatMap { it.pacotes }.toSet() + packageName
        serviceInfo = serviceInfo?.apply { packageNames = pacotes.toTypedArray() } ?: return
    }

    /** No registro, os avisos saem contados, no máximo uma linha por segundo. */
    private fun contarAviso(evento: AccessibilityEvent, pacote: String) {
        if (!Registro.ligado) return
        avisosPorContar++
        val agora = SystemClock.uptimeMillis()
        if (agora - avisosContadosEm < 1_000L) return
        val quantos = avisosPorContar
        Registro.escrever { "$quantos avisos; o último de $pacote, tipo ${AccessibilityEvent.eventTypeToString(evento.eventType)}" }
        avisosPorContar = 0
        avisosContadosEm = agora
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val pacote = evento?.packageName?.toString() ?: return
        contarAviso(evento, pacote)
        if (pacote !in pacotes) return
        if (pacote == packageName && !Simulador.naTela) return
        if (guardando && pacote != packageName) {
            Rastro.aviso(this, Origem.doPacote(pacote)?.rotulo ?: pacote, System.currentTimeMillis())
        }
        pacoteDaVez = pacote
        semAviso = false
        ultimoAviso = SystemClock.uptimeMillis()
        marcarLeitura()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        ServicoVivo.desde = null
        if (guardando) Rastro.gravar(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ServicoVivo.desde = null
        principal.removeCallbacksAndMessages(null)
        if (::preferencias.isInitialized) {
            preferencias.pararDeEscutar(aoMudarPreferencias)
            if (guardando) Rastro.gravar(this)
        }
        if (::sobreposicao.isInitialized) sobreposicao.esconder()
        leitorDeImagem?.fechar()
        emSegundoPlano.shutdown()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- leitura

    /** A tela muda dezenas de vezes por segundo (o mapa anda). Uma leitura por intervalo basta. */
    private fun marcarLeitura() {
        if (leituraMarcada) return
        leituraMarcada = true
        val espera = (ultimaLeitura + INTERVALO_MS - SystemClock.uptimeMillis()).coerceAtLeast(ASSENTAR_MS)
        principal.postDelayed(lerAgora, espera)
    }

    /**
     * A ronda, só com "Guardar as últimas leituras" ligado: se o aplicativo de corrida está na tela
     * e ficou calado, lê por conta própria. Sem ela, um aplicativo que não avisa as mudanças de tela
     * nunca seria lido, e o rastro não teria como mostrar isso.
     */
    private fun conferirSemAviso() {
        if (leituraMarcada || SystemClock.uptimeMillis() - ultimoAviso < RONDA_MS) return
        val (pacote, naTela) = LeitorDeTela.aVista(this, pacotes - packageName)
        Registro.escrever { "ronda: ${pacote ?: "nenhum aplicativo de corrida"} à vista; janelas: ${LeitorDeTela.janelas(this)}" }
        if (pacote == null) {
            if (guardando) {
                Rastro.passo(
                    this,
                    Rastro.Passo(System.currentTimeMillis(), "Ronda", "nenhuma janela de aplicativo de corrida entre $naTela na tela", "não tentou", false),
                )
            }
            return
        }
        pacoteDaVez = pacote
        semAviso = true
        marcarLeitura()
    }

    private fun ler() {
        leituraMarcada = false
        ultimaLeitura = SystemClock.uptimeMillis()
        val pacote = pacoteDaVez ?: return
        val origem = if (pacote == packageName) Origem.SIMULADOR else Origem.doPacote(pacote) ?: return

        val tela = try {
            LeitorDeTela.ler(this, pacote)
        } catch (_: Exception) {
            LeitorDeTela.Tela(emptyList(), null)
        }
        // A imagem da tela só é lida com o aplicativo de corrida à vista: ou ele tem janela
        // aberta, ou acabou de avisar uma mudança. Nunca a tela de outro aplicativo.
        val aVista = tela.area != null || tela.fragmentos.isNotEmpty() ||
            SystemClock.uptimeMillis() - ultimoAviso < AVISO_RECENTE_MS
        val trechos = Linhas.agrupar(tela.fragmentos)
        val lido = if (guardando || Registro.ligado) {
            (if (semAviso) "sem aviso, " else "") +
                "janelas ${tela.janelas} de ${tela.naTela}, ${tela.fragmentos.size} pedaços${marcas(trechos)}"
        } else {
            ""
        }
        val oferta = LeitorDeOferta.ler(trechos, origem)
        if (oferta != null) {
            // O simulador fica de fora: nele a pessoa alterna entre texto e imagem para testar.
            if (origem != Origem.SIMULADOR) bastaOTexto += pacote
            concluir(origem, oferta, trechos, "texto", lido, "não precisou")
            return
        }
        val semImagem = porQueNaoLerImagem(pacote, aVista)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && semImagem == null) {
            lerPorImagem(origem, tela.area, trechos, lido)
        } else {
            concluir(origem, null, trechos, "texto", lido, semImagem ?: "não tentou")
        }
    }

    /** Nulo quando é para ler a imagem da tela; senão, o motivo de não ler, para o rastro. */
    private fun porQueNaoLerImagem(pacote: String, aVista: Boolean): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "este Android não captura a tela (anterior ao 11)"
        if (!preferencias.lerPorImagem) return "desligada na aba Mais"
        if (!aVista) return "o aplicativo não estava à vista"
        if (pacote !in bastaOTexto) return null
        leiturasSemImagem = (leiturasSemImagem + 1) % DE_VEZ_EM_QUANDO
        return if (leiturasSemImagem == 0) null else "pulada, o texto já tinha bastado antes"
    }

    /** Se as linhas têm valor em reais e tempo ou distância. Não diz o que está escrito nelas. */
    private fun marcas(trechos: List<Trecho>): String {
        if (!(guardando || Registro.ligado) || trechos.isEmpty()) return ""
        fun simNao(tem: Boolean) = if (tem) "sim" else "não"
        return " (valor ${simNao(LeitorDeOferta.temValor(trechos))}, trecho ${simNao(LeitorDeOferta.temTrecho(trechos))})"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun lerPorImagem(origem: Origem, area: Rect?, trechosDoTexto: List<Trecho>, lido: String) {
        val pedido = vezDaImagem.comecar { capturaPerdida(origem, trechosDoTexto, lido) }
        if (pedido == null) {
            Registro.escrever { "${origem.rotulo} | a leitura por imagem anterior ainda não terminou; esta ficou sem imagem" }
            return
        }
        val cartao = sobreposicao.area()

        fun terminar(fragmentos: List<Fragmento>?, falha: String) = principal.post {
            // Chegou depois do prazo: a leitura já foi dada por perdida e outra pode estar em curso.
            if (!vezDaImagem.terminar(pedido)) return@post
            capturasPerdidas = 0
            val trechos = fragmentos?.let(Linhas::agrupar)
            if (trechos == null) {
                concluir(origem, null, trechosDoTexto, "texto", lido, falha)
            } else {
                concluir(origem, LeitorDeOferta.ler(trechos, origem), trechos, "imagem", lido, "${trechos.size} linhas${marcas(trechos)}")
            }
        }

        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, emSegundoPlano, object : TakeScreenshotCallback {
                override fun onSuccess(captura: ScreenshotResult) {
                    pedido.capturaChegou()
                    if (!pedido.valendo) {
                        captura.hardwareBuffer.close()
                        return
                    }
                    val imagem = try {
                        recortar(captura, area, cartao)
                    } catch (_: Exception) {
                        null
                    }
                    if (imagem == null) {
                        terminar(null, "a captura veio sem área do aplicativo para ler")
                        return
                    }
                    val leitor = leitorDeImagem ?: LeitorDeImagem().also { leitorDeImagem = it }
                    leitor.ler(imagem) { fragmentos ->
                        imagem.recycle()
                        terminar(fragmentos, "o reconhecimento do texto falhou")
                    }
                }

                override fun onFailure(codigo: Int) {
                    pedido.capturaChegou()
                    terminar(null, "o Android recusou a captura (código $codigo)")
                }
            })
        } catch (_: Exception) {
            // O aparelho não deixou capturar a tela. Segue só com o texto.
            vezDaImagem.terminar(pedido)
            concluir(origem, null, trechosDoTexto, "texto", lido, "o aparelho não deixou capturar a tela")
        }
    }

    /**
     * O prazo da leitura por imagem venceu. Não conta como "a oferta sumiu": o cartão fica onde
     * está. E tenta de novo logo, sem esperar o próximo aviso de mudança de tela, que com a
     * oferta parada pode demorar mais do que ela dura.
     */
    private fun capturaPerdida(origem: Origem, trechosDoTexto: List<Trecho>, lido: String) {
        Registro.escrever { "${origem.rotulo} | texto: $lido | imagem: $SEM_RESPOSTA | vai tentar de novo: ${capturasPerdidas < TENTATIVAS}" }
        registrar(origem, null, trechosDoTexto, "texto", lido, SEM_RESPOSTA)
        if (capturasPerdidas++ < TENTATIVAS) marcarLeitura()
    }

    /** Fica só com a área do aplicativo de corrida e cobre o nosso próprio cartão. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun recortar(captura: ScreenshotResult, area: Rect?, cartao: Rect?): Bitmap? {
        val memoria = captura.hardwareBuffer
        val inteira = try {
            // A captura vem num formato que não dá para alterar: copia para poder cobrir o cartão.
            val original = Bitmap.wrapHardwareBuffer(memoria, captura.colorSpace)
            original?.copy(Bitmap.Config.ARGB_8888, true).also { original?.recycle() }
        } finally {
            memoria.close()
        }
        if (inteira == null) return null

        val recorte = areaDeLeitura(inteira.width, inteira.height, area)
        if (recorte == null) {
            inteira.recycle()
            return null
        }
        if (cartao != null) Canvas(inteira).drawRect(cartao, Paint().apply { color = Color.BLACK })
        if (recorte.width() == inteira.width && recorte.height() == inteira.height) return inteira
        return Bitmap.createBitmap(inteira, recorte.left, recorte.top, recorte.width(), recorte.height())
            .also { if (it !== inteira) inteira.recycle() }
    }

    // ---------------------------------------------------------------- resultado

    private fun concluir(origem: Origem, oferta: Oferta?, trechos: List<Trecho>, modo: String, lido: String, imagem: String) {
        Registro.escrever {
            "${origem.rotulo} | texto: $lido | imagem: $imagem | ${if (oferta != null) "reconhecida pelo modo $modo" else "NÃO reconhecida"}"
        }
        registrar(origem, oferta, trechos, modo, lido, imagem)
        principal.removeCallbacks(tirarCartao)
        if (oferta == null) {
            // Uma leitura falha no meio de uma animação não derruba o cartão; duas seguidas, sim.
            if (++leiturasSemOferta >= 2) {
                sobreposicao.esconder()
            } else if (sobreposicao.visivel) {
                principal.postDelayed(tirarCartao, SEM_NOTICIA_MS)
                marcarLeitura()
            }
            return
        }
        leiturasSemOferta = 0
        sobreposicao.mostrar(Avaliador.avaliar(oferta, preferencias.criterios), preferencias.posicao)
        // Com a oferta parada na tela não chega aviso de mudança: confere de novo sozinho,
        // para tirar o cartão quando ela sumir.
        principal.postDelayed(tirarCartao, SEM_NOTICIA_MS)
        marcarLeitura()
    }

    private fun registrar(origem: Origem, oferta: Oferta?, trechos: List<Trecho>, modo: String, lido: String, imagem: String) {
        if (!guardando) return
        // O simulador não entra no rastro: o que se quer saber é o caminho da oferta de verdade.
        if (origem != Origem.SIMULADOR) {
            Rastro.passo(this, Rastro.Passo(System.currentTimeMillis(), origem.rotulo, lido, imagem, oferta != null))
        }
        // O texto da tela só é guardado quando ela tem cara de oferta: a reconhecida, e a que tem
        // valor em reais junto com tempo ou distância. A tela parada, só com os ganhos do dia, não entra.
        if (oferta == null && !LeitorDeOferta.pareceOferta(trechos)) return
        Diagnostico.registrar(
            this,
            Diagnostico.Leitura(System.currentTimeMillis(), origem.rotulo, modo, oferta != null, trechos.map { it.texto }),
        )
    }

    private companion object {
        /** Tempo mínimo entre duas leituras. */
        const val INTERVALO_MS = 1_200L

        /** Espera a tela assentar depois de uma mudança, antes de ler. */
        const val ASSENTAR_MS = 250L

        /** Sem leitura nova nesse tempo, o cartão sai. */
        const val SEM_NOTICIA_MS = 6_000L

        /** Um aviso de mudança mais novo que isto prova que o aplicativo de corrida está à vista. */
        const val AVISO_RECENTE_MS = 2_000L

        /** Onde o texto já bastou, lê a imagem uma vez a cada tantas leituras sem oferta. */
        const val DE_VEZ_EM_QUANDO = 5

        /** De quanto em quanto tempo a ronda confere a tela, quando o aplicativo de corrida não avisa nada. */
        const val RONDA_MS = 3_000L

        /** A captura da tela costuma chegar em centésimos de segundo. Passou disto, o Android a perdeu. */
        const val PRAZO_DA_CAPTURA_MS = 800L

        /** A leitura por imagem inteira, com o reconhecimento do texto (o primeiro carrega o modelo). */
        const val PRAZO_DA_IMAGEM_MS = 4_000L

        /** Quantas vezes seguidas tentar de novo, sozinho, depois de uma captura sem resposta. */
        const val TENTATIVAS = 3

        const val SEM_RESPOSTA = "o Android não respondeu ao pedido de captura"
    }
}

/**
 * Que pedaço da captura pode ser lido: só o que o aplicativo de corrida ocupa (`area`). Sem essa
 * informação, a tela inteira. Devolve nulo quando o pedaço é pequeno demais para ter uma oferta,
 * como o botão flutuante por cima de outro aplicativo: melhor não ler nada do que ler a tela alheia.
 */
internal fun areaDeLeitura(largura: Int, altura: Int, area: Rect?): Rect? {
    val recorte = Rect(0, 0, largura, altura)
    if (area == null) return recorte
    if (!recorte.intersect(area) || recorte.width() < 200 || recorte.height() < 200) return null
    return recorte
}

/** A tela de teste do próprio aplicativo. O serviço só lê o Vale a Corrida quando ela está aberta. */
object Simulador {
    @Volatile
    var naTela: Boolean = false
}

/** Desde quando o serviço de leitura está rodando neste momento; nulo quando o Android o desligou. */
object ServicoVivo {
    @Volatile
    var desde: Long? = null
}
