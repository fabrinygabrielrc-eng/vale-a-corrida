package app.valeacorrida.nucleo

/**
 * Os valores que o motorista escolhe. Em todo campo, 0 quer dizer "não usar este critério".
 *
 * Abaixo do `minimo` a corrida é ruim; a partir do `bom`, é boa; no meio, é média.
 */
data class Criterios(
    val porKmMinimo: Double = 1.50,
    val porKmBom: Double = 2.00,
    val porHoraMinimo: Double = 25.0,
    val porHoraBom: Double = 35.0,
    /** Corrida que paga menos que isto é ruim, por melhor que seja a taxa. */
    val valorMinimo: Double = 0.0,
    /** Distância máxima até o passageiro, em km. */
    val buscaMaximaKm: Double = 0.0,
    val notaMinima: Double = 0.0,
    /** Combustível e desgaste, em reais por km. Com isto o cartão mostra também o que sobra. */
    val custoPorKm: Double = 0.0,
    /** Soma o caminho até o passageiro na conta. É o padrão: esse trecho também gasta. */
    val contarBusca: Boolean = true,
)

enum class Veredito { BOA, MEDIA, RUIM }

/** Por que a corrida não ficou boa. */
enum class Motivo { POR_KM_BAIXO, POR_HORA_BAIXO, VALOR_BAIXO, BUSCA_LONGA, NOTA_BAIXA, POR_KM_MEDIO, POR_HORA_MEDIO }

data class Avaliacao(
    val oferta: Oferta,
    val km: Double,
    /** Nulo quando a tela não informa o tempo. */
    val minutos: Double?,
    val porKm: Double,
    val porHora: Double?,
    /** O que sobra depois do custo por km. Nulo quando o motorista não informou o custo. */
    val sobra: Double?,
    val veredito: Veredito,
    val motivos: List<Motivo>,
)

object Avaliador {

    fun avaliar(oferta: Oferta, criterios: Criterios): Avaliacao {
        val busca = oferta.busca.takeIf { criterios.contarBusca }
        val km = oferta.viagem.km + (busca?.km ?: 0.0)
        val minutos = when {
            oferta.viagem.minutos == null -> null
            busca == null -> oferta.viagem.minutos
            busca.minutos == null -> null
            else -> oferta.viagem.minutos + busca.minutos
        }
        val porKm = oferta.reais / km
        val porHora = minutos?.takeIf { it > 0 }?.let { oferta.reais / (it / 60) }
        val sobra = criterios.custoPorKm.takeIf { it > 0 }?.let { oferta.reais - it * km }

        val ruins = buildList {
            if (criterios.porKmMinimo > 0 && porKm < criterios.porKmMinimo) add(Motivo.POR_KM_BAIXO)
            if (criterios.porHoraMinimo > 0 && porHora != null && porHora < criterios.porHoraMinimo) add(Motivo.POR_HORA_BAIXO)
            if (criterios.valorMinimo > 0 && oferta.reais < criterios.valorMinimo) add(Motivo.VALOR_BAIXO)
            val buscaKm = oferta.busca?.km
            if (criterios.buscaMaximaKm > 0 && buscaKm != null && buscaKm > criterios.buscaMaximaKm) add(Motivo.BUSCA_LONGA)
            if (criterios.notaMinima > 0 && oferta.nota != null && oferta.nota < criterios.notaMinima) add(Motivo.NOTA_BAIXA)
        }
        if (ruins.isNotEmpty()) return Avaliacao(oferta, km, minutos, porKm, porHora, sobra, Veredito.RUIM, ruins)

        val medios = buildList {
            if (criterios.porKmBom > 0 && porKm < criterios.porKmBom) add(Motivo.POR_KM_MEDIO)
            if (criterios.porHoraBom > 0 && porHora != null && porHora < criterios.porHoraBom) add(Motivo.POR_HORA_MEDIO)
        }
        val veredito = if (medios.isEmpty()) Veredito.BOA else Veredito.MEDIA
        return Avaliacao(oferta, km, minutos, porKm, porHora, sobra, veredito, medios)
    }
}
