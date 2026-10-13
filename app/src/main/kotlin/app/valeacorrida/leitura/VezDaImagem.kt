package app.valeacorrida.leitura

import android.os.Handler

/**
 * Uma leitura por imagem de cada vez, com prazo.
 *
 * O Android às vezes nunca responde ao pedido de captura da tela: a resposta se perde dentro do
 * sistema (no registro dele, "ScreenCaptureListenerWrapper consumer not alive"; visto num Xiaomi
 * com Android 15). Sem prazo, o serviço ficava esperando essa resposta para sempre e não lia
 * mais nada, até ser desligado e ligado de novo.
 *
 * São dois prazos: um curto para a captura chegar, que é o que se perde e normalmente leva
 * centésimos de segundo, e um comprido para a leitura inteira, com o reconhecimento do texto.
 *
 * `comecar` e `terminar` são chamados na thread de `relogio`, que é onde os prazos vencem.
 */
internal class VezDaImagem(
    private val relogio: Handler,
    private val prazoDaCapturaMs: Long,
    private val prazoTotalMs: Long,
) {
    /** Uma leitura em andamento. O que chegar depois de `valendo` cair é para jogar fora. */
    class Pedido internal constructor(internal val aoPerder: () -> Unit) {
        @Volatile
        var valendo = true
            internal set

        @Volatile
        internal var chegou = false

        internal var prazos: List<Runnable> = emptyList()

        /** O Android respondeu ao pedido de captura. Pode ser chamado de qualquer thread. */
        fun capturaChegou() {
            chegou = true
        }
    }

    private var daVez: Pedido? = null

    /**
     * Começa uma leitura. Devolve nulo se a anterior ainda está em andamento e dentro do prazo.
     * `aoPerder` roda se o prazo vencer antes de `terminar`.
     */
    fun comecar(aoPerder: () -> Unit): Pedido? {
        if (daVez != null) return null
        val pedido = Pedido(aoPerder)
        pedido.prazos = listOf(
            Runnable { if (!pedido.chegou) perder(pedido) },
            Runnable { perder(pedido) },
        )
        daVez = pedido
        relogio.postDelayed(pedido.prazos[0], prazoDaCapturaMs)
        relogio.postDelayed(pedido.prazos[1], prazoTotalMs)
        return pedido
    }

    /** A leitura acabou. Falso quando ela já tinha sido dada por perdida: o resultado não vale. */
    fun terminar(pedido: Pedido): Boolean {
        if (daVez !== pedido) return false
        soltar(pedido)
        return true
    }

    private fun perder(pedido: Pedido) {
        if (daVez !== pedido) return
        soltar(pedido)
        pedido.aoPerder()
    }

    private fun soltar(pedido: Pedido) {
        pedido.valendo = false
        pedido.prazos.forEach(relogio::removeCallbacks)
        daVez = null
    }
}
