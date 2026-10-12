package app.valeacorrida.leitura

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import app.valeacorrida.Posicao
import app.valeacorrida.nucleo.Avaliacao

/** Põe e tira o cartão por cima da tela. O cartão não recebe toque: nunca cobre um botão. */
class Sobreposicao(private val contexto: Context) {
    private val janelas = contexto.getSystemService(WindowManager::class.java)
    private var cartao: CartaoView? = null
    private var posicaoAtual: Posicao? = null

    val visivel: Boolean get() = cartao != null

    fun mostrar(avaliacao: Avaliacao, posicao: Posicao) {
        if (cartao != null && posicaoAtual != posicao) esconder()
        val vista = cartao ?: CartaoView(contexto).also { nova ->
            try {
                janelas.addView(nova, parametros(posicao))
            } catch (_: Exception) {
                return // o serviço está sendo desligado
            }
            cartao = nova
            posicaoAtual = posicao
        }
        vista.mostrar(avaliacao)
    }

    fun esconder() {
        val vista = cartao ?: return
        cartao = null
        try {
            janelas.removeView(vista)
        } catch (_: Exception) {
            // já tinha saído da tela
        }
    }

    /** Onde o cartão está, para a leitura por imagem não ler o próprio cartão. */
    fun area(): Rect? {
        val vista = cartao?.takeIf { it.isAttachedToWindow && it.width > 0 } ?: return null
        val canto = IntArray(2).also(vista::getLocationOnScreen)
        return Rect(canto[0], canto[1], canto[0] + vista.width, canto[1] + vista.height)
    }

    private fun parametros(posicao: Posicao): WindowManager.LayoutParams {
        val densidade = contexto.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or when (posicao) {
                Posicao.TOPO -> Gravity.TOP
                Posicao.MEIO -> Gravity.CENTER_VERTICAL
                Posicao.BASE -> Gravity.BOTTOM
            }
            // Em cima, livra a barra de status; embaixo, a barra de navegação.
            y = when (posicao) {
                Posicao.TOPO -> (44 * densidade).toInt()
                Posicao.MEIO -> 0
                Posicao.BASE -> (72 * densidade).toInt()
            }
        }
    }
}
