package app.valeacorrida.nucleo

import java.time.LocalDate
import java.time.YearMonth

/** Os tipos de gasto de quem roda com aplicativo. A ordem é a que aparece na tela. */
enum class Categoria(val rotulo: String, val exemplos: String) {
    COMBUSTIVEL("Combustível", "gasolina, etanol, GNV, recarga"),
    ALIMENTACAO("Alimentação", "lanche, almoço, água"),
    MANUTENCAO("Manutenção", "óleo, pneus, peças, oficina"),
    LAVAGEM("Lavagem", "lava-rápido, aspirador"),
    PEDAGIO("Pedágio e vaga", "pedágio, estacionamento"),
    CARRO("Aluguel ou parcela", "aluguel do carro, financiamento"),
    DOCUMENTOS("Seguro e impostos", "seguro, IPVA, licenciamento, multa"),
    CELULAR("Celular e internet", "plano, recarga, suporte"),
    OUTROS("Outros", "o que não coube nos outros"),
}

/** Um gasto anotado. O dinheiro fica em centavos para a soma não perder centavo no caminho. */
data class Gasto(
    val id: Long,
    val dia: LocalDate,
    val categoria: Categoria,
    val centavos: Long,
    val nota: String = "",
)

/** Quanto uma categoria pesou no mês. `parte` vai de 0 a 1. */
data class Fatia(val categoria: Categoria, val centavos: Long, val parte: Double, val lancamentos: Int)

data class DiaDeGasto(val dia: LocalDate, val centavos: Long)

/** As contas de um mês de gastos: é o que a tela de relatório mostra e o que a pessoa compartilha. */
class RelatorioDoMes(
    val mes: YearMonth,
    /** Os gastos do mês, do dia mais recente para o mais antigo. */
    val gastos: List<Gasto>,
    /** O total do mês anterior, ou nulo se nada foi anotado nele. */
    val totalAnterior: Long?,
) {
    val total: Long = gastos.sumOf { it.centavos }

    /** As categorias que tiveram gasto, da que mais pesou para a que menos pesou. */
    val fatias: List<Fatia> = gastos.groupBy { it.categoria }
        .map { (categoria, itens) ->
            val soma = itens.sumOf { it.centavos }
            Fatia(categoria, soma, if (total > 0) soma.toDouble() / total else 0.0, itens.size)
        }
        .sortedWith(compareByDescending<Fatia> { it.centavos }.thenBy { it.categoria.ordinal })

    /** Os dias em que houve gasto, do mais recente para o mais antigo. */
    val dias: List<DiaDeGasto> = gastos.groupBy { it.dia }
        .map { (dia, itens) -> DiaDeGasto(dia, itens.sumOf { it.centavos }) }
        .sortedByDescending { it.dia }

    /** A média conta só os dias com gasto anotado: dia de folga não entra na divisão. */
    val mediaPorDia: Long = if (dias.isEmpty()) 0 else Math.round(total.toDouble() / dias.size)

    val diaMaisCaro: DiaDeGasto? = dias.maxWithOrNull(compareBy<DiaDeGasto> { it.centavos }.thenBy { it.dia })

    val maior: Gasto? = gastos.maxWithOrNull(compareBy<Gasto> { it.centavos }.thenBy { it.dia })

    /** Positivo quando o mês saiu mais caro que o anterior. */
    val diferenca: Long? = totalAnterior?.let { total - it }

    val vazio: Boolean get() = gastos.isEmpty()
}

object Relatorios {

    fun doMes(todos: List<Gasto>, mes: YearMonth): RelatorioDoMes {
        val anterior = mes.minusMonths(1)
        val doAnterior = todos.filter { YearMonth.from(it.dia) == anterior }
        return RelatorioDoMes(
            mes = mes,
            gastos = todos.filter { YearMonth.from(it.dia) == mes }
                .sortedWith(compareByDescending<Gasto> { it.dia }.thenByDescending { it.id }),
            totalAnterior = doAnterior.takeIf { it.isNotEmpty() }?.sumOf { it.centavos },
        )
    }

    /** "R$ 184,50 a mais que em setembro", ou nulo quando não há mês anterior para comparar. */
    fun comparacao(relatorio: RelatorioDoMes): String? {
        val diferenca = relatorio.diferenca ?: return null
        val anterior = Datas.nomeDoMes(relatorio.mes.minusMonths(1))
        return when {
            diferenca > 0 -> "${Formato.centavos(diferenca)} a mais que em $anterior"
            diferenca < 0 -> "${Formato.centavos(-diferenca)} a menos que em $anterior"
            else -> "igual a $anterior"
        }
    }

    /** "64%", ou "menos de 1%" para a fatia que existe mas arredondaria para zero. */
    fun porcento(parte: Double): String {
        val inteiro = Math.round(parte * 100).toInt()
        return if (inteiro == 0 && parte > 0) "menos de 1%" else "$inteiro%"
    }

    /** O relatório em texto, para mandar por mensagem. Sem colunas: mensagem não tem letra de largura fixa. */
    fun texto(relatorio: RelatorioDoMes): String = buildString {
        appendLine("Gastos de ${Datas.mes(relatorio.mes)}")
        appendLine()
        if (relatorio.vazio) {
            append("Nenhum gasto anotado neste mês.")
            return@buildString
        }
        appendLine("Total: ${Formato.centavos(relatorio.total)} em ${lancamentos(relatorio.gastos.size)}")
        comparacao(relatorio)?.let { appendLine("No mês anterior: ${Formato.centavos(relatorio.totalAnterior!!)} ($it)") }
        appendLine()
        appendLine("Para onde foi")
        for (fatia in relatorio.fatias) {
            appendLine("${fatia.categoria.rotulo}: ${Formato.centavos(fatia.centavos)} (${porcento(fatia.parte)})")
        }
        appendLine()
        appendLine("Dias com gasto: ${relatorio.dias.size}")
        appendLine("Média nesses dias: ${Formato.centavos(relatorio.mediaPorDia)}")
        relatorio.diaMaisCaro?.let { appendLine("Dia mais caro: ${Datas.dia(it.dia)}, ${Formato.centavos(it.centavos)}") }
        relatorio.maior?.let { append("Maior gasto: ${maior(it)}") }
        appendLine()
        appendLine()
        append("Anotado no Vale a Corrida")
    }

    /** "Manutenção, R$ 250,00 em 14/10 (troca de óleo)". */
    fun maior(gasto: Gasto): String = buildString {
        append("${gasto.categoria.rotulo}, ${Formato.centavos(gasto.centavos)} em ${Datas.dia(gasto.dia)}")
        if (gasto.nota.isNotBlank()) append(" (${gasto.nota})")
    }

    fun lancamentos(quantos: Int): String = if (quantos == 1) "1 lançamento" else "$quantos lançamentos"
}
