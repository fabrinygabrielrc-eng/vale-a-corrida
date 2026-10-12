package app.valeacorrida.leitura

import android.accessibilityservice.AccessibilityService
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
    private var lendoImagem = false
    private var ultimaLeitura = 0L
    private var ultimoAviso = 0L
    private var leiturasSemOferta = 0

    /**
     * Aplicativos em que o texto da acessibilidade já entregou uma oferta. Neles a imagem da tela
     * só é lida de vez em quando, para o caso de algum tipo de oferta vir sem texto.
     */
    private val bastaOTexto = mutableSetOf<String>()
    private var leiturasSemImagem = 0

    private val aoMudarPreferencias = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> aplicarPreferencias() }
    private val lerAgora = Runnable(::ler)
    private val tirarCartao = Runnable { sobreposicao.esconder() }

    override fun onServiceConnected() {
        preferencias = Preferencias(this)
        sobreposicao = Sobreposicao(this)
        preferencias.escutar(aoMudarPreferencias)
        aplicarPreferencias()
    }

    private fun aplicarPreferencias() {
        // O próprio Vale a Corrida entra na lista por causa do simulador.
        pacotes = preferencias.acompanhados.flatMap { it.pacotes }.toSet() + packageName
        serviceInfo = serviceInfo?.apply { packageNames = pacotes.toTypedArray() } ?: return
    }

    override fun onAccessibilityEvent(evento: AccessibilityEvent?) {
        val pacote = evento?.packageName?.toString() ?: return
        if (pacote !in pacotes) return
        if (pacote == packageName && !Simulador.naTela) return
        pacoteDaVez = pacote
        ultimoAviso = SystemClock.uptimeMillis()
        marcarLeitura()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        principal.removeCallbacksAndMessages(null)
        if (::preferencias.isInitialized) preferencias.pararDeEscutar(aoMudarPreferencias)
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
        val oferta = LeitorDeOferta.ler(trechos, origem)
        when {
            oferta != null -> {
                // O simulador fica de fora: nele a pessoa alterna entre texto e imagem para testar.
                if (origem != Origem.SIMULADOR) bastaOTexto += pacote
                concluir(origem, oferta, trechos, "texto")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && aVista && querLerImagem(pacote) ->
                lerPorImagem(origem, tela.area, trechos)
            else -> concluir(origem, null, trechos, "texto")
        }
    }

    private fun querLerImagem(pacote: String): Boolean {
        if (!preferencias.lerPorImagem) return false
        if (pacote !in bastaOTexto) return true
        leiturasSemImagem = (leiturasSemImagem + 1) % DE_VEZ_EM_QUANDO
        return leiturasSemImagem == 0
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun lerPorImagem(origem: Origem, area: Rect?, trechosDoTexto: List<Trecho>) {
        if (lendoImagem) return
        lendoImagem = true
        val cartao = sobreposicao.area()

        fun terminar(fragmentos: List<Fragmento>?) = principal.post {
            lendoImagem = false
            val trechos = fragmentos?.let(Linhas::agrupar)
            if (trechos == null) {
                concluir(origem, null, trechosDoTexto, "texto")
            } else {
                concluir(origem, LeitorDeOferta.ler(trechos, origem), trechos, "imagem")
            }
        }

        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, emSegundoPlano, object : TakeScreenshotCallback {
                override fun onSuccess(captura: ScreenshotResult) {
                    val imagem = try {
                        recortar(captura, area, cartao)
                    } catch (_: Exception) {
                        null
                    }
                    if (imagem == null) {
                        terminar(null)
                        return
                    }
                    val leitor = leitorDeImagem ?: LeitorDeImagem().also { leitorDeImagem = it }
                    leitor.ler(imagem) { fragmentos ->
                        imagem.recycle()
                        terminar(fragmentos)
                    }
                }

                override fun onFailure(codigo: Int) {
                    terminar(null)
                }
            })
        } catch (_: Exception) {
            // O aparelho não deixou capturar a tela. Segue só com o texto.
            lendoImagem = false
            concluir(origem, null, trechosDoTexto, "texto")
        }
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

        if (cartao != null) Canvas(inteira).drawRect(cartao, Paint().apply { color = Color.BLACK })

        val recorte = Rect(0, 0, inteira.width, inteira.height)
        if (area == null || !recorte.intersect(area) || recorte.width() < 200 || recorte.height() < 200) return inteira
        if (recorte.width() == inteira.width && recorte.height() == inteira.height) return inteira
        return Bitmap.createBitmap(inteira, recorte.left, recorte.top, recorte.width(), recorte.height())
            .also { if (it !== inteira) inteira.recycle() }
    }

    // ---------------------------------------------------------------- resultado

    private fun concluir(origem: Origem, oferta: Oferta?, trechos: List<Trecho>, modo: String) {
        registrar(origem, oferta, trechos, modo)
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

    private fun registrar(origem: Origem, oferta: Oferta?, trechos: List<Trecho>, modo: String) {
        if (!preferencias.guardarLeituras) return
        // Só interessa a tela que tem cara de oferta: a reconhecida, e a que tem valor em reais
        // junto com tempo ou distância. A tela parada, só com os ganhos do dia, não entra.
        if (oferta == null) {
            val temValor = trechos.any { TEM_VALOR.containsMatchIn(it.texto) }
            val temTrecho = trechos.any { TEM_TRECHO.containsMatchIn(it.texto) }
            if (!temValor || !temTrecho) return
        }
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

        /** "6 min", "2.1 km". */
        val TEM_TRECHO = Regex("\\d\\s?(?:min|km)", RegexOption.IGNORE_CASE)

        /** "R$ 12", e também "RS 12" e "R5 12", como a leitura por imagem às vezes entrega. */
        val TEM_VALOR = Regex("R\\s?[\$S5]\\s?\\d")
    }
}

/** A tela de teste do próprio aplicativo. O serviço só lê o Vale a Corrida quando ela está aberta. */
object Simulador {
    @Volatile
    var naTela: Boolean = false
}
