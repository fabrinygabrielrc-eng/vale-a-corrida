package app.valeacorrida.nucleo

import java.util.Locale
import kotlin.math.roundToInt

/** Números do jeito que se escreve no Brasil: vírgula nos centavos, ponto no milhar. */
object Formato {
    private val BRASIL: Locale = Locale.forLanguageTag("pt-BR")

    fun reais(valor: Double): String = "R$ " + numero(valor, 2)

    fun numero(valor: Double, casas: Int = 1): String = String.format(BRASIL, "%,.${casas}f", valor)

    fun minutos(minutos: Double): String {
        val total = minutos.roundToInt()
        return if (total < 60) "$total min" else "${total / 60} h ${total % 60} min"
    }

    /** Lê o que a pessoa digitou ("2,5", "2.5", "R$ 2,50"). Devolve nulo se não for número. */
    fun ler(texto: String): Double? {
        val limpo = texto.replace("R$", "").trim()
        // Com vírgula, o ponto é separador de milhar; sem vírgula, o ponto é o decimal.
        val numero = if (',' in limpo) limpo.replace(".", "").replace(',', '.') else limpo
        return numero.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
