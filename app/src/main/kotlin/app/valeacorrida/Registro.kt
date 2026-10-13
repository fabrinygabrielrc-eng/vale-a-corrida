package app.valeacorrida

import android.os.SystemClock
import android.util.Log

/**
 * O que o serviço de leitura está fazendo, escrito no registro do Android (logcat), para quem
 * está com o celular no cabo. Nunca o que está escrito na tela: só contagens e decisões.
 *
 * Fica calado até alguém pedir pelo cabo, com `adb shell setprop log.tag.ValeACorrida DEBUG`,
 * e volta a calar quando o celular reinicia. O `Rastro` conta a mesma história para quem não
 * tem cabo; este serve para ver a leitura acontecendo, na hora.
 */
object Registro {
    private const val ETIQUETA = "ValeACorrida"

    /** Perguntar ao Android a cada aviso de mudança de tela sairia caro: a resposta vale por uns segundos. */
    private const val CONFERIR_A_CADA_MS = 5_000L

    @Volatile
    private var valeAte = 0L

    @Volatile
    private var resposta = false

    val ligado: Boolean
        get() {
            val agora = SystemClock.uptimeMillis()
            if (agora >= valeAte) {
                resposta = Log.isLoggable(ETIQUETA, Log.DEBUG)
                valeAte = agora + CONFERIR_A_CADA_MS
            }
            return resposta
        }

    /** A frase só é montada quando o registro está ligado. */
    fun escrever(frase: () -> String) {
        if (ligado) Log.d(ETIQUETA, frase())
    }
}
