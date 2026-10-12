package app.valeacorrida

import app.valeacorrida.nucleo.Avaliacao
import app.valeacorrida.nucleo.Formato
import app.valeacorrida.nucleo.Motivo
import app.valeacorrida.nucleo.Veredito

/** As frases do cartão. Ficam num lugar só para o cartão e as telas dizerem a mesma coisa. */
object Textos {

    /** "Boa", "Média: por km no limite", "Ruim: passageiro longe". */
    fun titulo(avaliacao: Avaliacao): String {
        val veredito = when (avaliacao.veredito) {
            Veredito.BOA -> "Boa"
            Veredito.MEDIA -> "Média"
            Veredito.RUIM -> "Ruim"
        }
        val motivo = avaliacao.motivos.firstOrNull() ?: return veredito
        return "$veredito: ${motivo(motivo)}"
    }

    private fun motivo(motivo: Motivo): String = when (motivo) {
        Motivo.POR_KM_BAIXO -> "paga pouco por km"
        Motivo.POR_HORA_BAIXO -> "paga pouco por hora"
        Motivo.VALOR_BAIXO -> "valor baixo"
        Motivo.BUSCA_LONGA -> "passageiro longe"
        Motivo.NOTA_BAIXA -> "nota baixa"
        Motivo.POR_KM_MEDIO -> "por km no limite"
        Motivo.POR_HORA_MEDIO -> "por hora no limite"
    }

    fun porKm(avaliacao: Avaliacao): String = Formato.reais(avaliacao.porKm) + "/km"

    /** Vazio quando a tela não informa o tempo da corrida. */
    fun porHora(avaliacao: Avaliacao): String =
        avaliacao.porHora?.let { "R$ " + Formato.numero(it, 0) + "/h" }.orEmpty()

    /** "9,5 km em 20 min. Sobram R$ 14,20." */
    fun detalhe(avaliacao: Avaliacao): String = buildString {
        append(Formato.numero(avaliacao.km))
        append(" km")
        avaliacao.minutos?.let { append(" em ").append(Formato.minutos(it)) }
        append('.')
        avaliacao.sobra?.let { append(if (it >= 0) " Sobram " else " Faltam ").append(Formato.reais(kotlin.math.abs(it))).append('.') }
    }
}
