package app.valeacorrida.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GastosTest {
    private val outubro = YearMonth.of(2026, 10)
    private var proximo = 0L

    private fun gasto(dia: String, categoria: Categoria, reais: Double, nota: String = "") =
        Gasto(++proximo, LocalDate.parse(dia), categoria, Math.round(reais * 100), nota)

    private val gastos = listOf(
        gasto("2026-09-28", Categoria.COMBUSTIVEL, 100.0),
        gasto("2026-09-30", Categoria.ALIMENTACAO, 20.0),
        gasto("2026-10-01", Categoria.COMBUSTIVEL, 150.0),
        gasto("2026-10-01", Categoria.ALIMENTACAO, 18.5, "lanche"),
        gasto("2026-10-14", Categoria.MANUTENCAO, 250.0, "troca de óleo"),
        gasto("2026-10-14", Categoria.COMBUSTIVEL, 120.0),
        gasto("2026-10-31", Categoria.ALIMENTACAO, 11.5),
        gasto("2026-11-01", Categoria.LAVAGEM, 40.0),
    )

    @Test fun `soma so os gastos do mes`() {
        val relatorio = Relatorios.doMes(gastos, outubro)

        assertEquals(55000L, relatorio.total)
        assertEquals(5, relatorio.gastos.size)
        assertEquals(LocalDate.parse("2026-10-31"), relatorio.gastos.first().dia)
        assertEquals(LocalDate.parse("2026-10-01"), relatorio.gastos.last().dia)
    }

    @Test fun `no mesmo dia o ultimo anotado vem primeiro`() {
        val doDia = Relatorios.doMes(gastos, outubro).gastos.filter { it.dia == LocalDate.parse("2026-10-14") }

        assertEquals(listOf(Categoria.COMBUSTIVEL, Categoria.MANUTENCAO), doDia.map { it.categoria })
    }

    @Test fun `ordena as categorias pelo peso`() {
        val fatias = Relatorios.doMes(gastos, outubro).fatias

        assertEquals(listOf(Categoria.COMBUSTIVEL, Categoria.MANUTENCAO, Categoria.ALIMENTACAO), fatias.map { it.categoria })
        assertEquals(27000L, fatias[0].centavos)
        assertEquals(2, fatias[0].lancamentos)
        assertEquals(1.0, fatias.sumOf { it.parte }, 1e-9)
        assertEquals("49%", Relatorios.porcento(fatias[0].parte))
    }

    @Test fun `categorias empatadas saem na ordem da tela`() {
        val empate = listOf(
            gasto("2026-10-02", Categoria.LAVAGEM, 30.0),
            gasto("2026-10-02", Categoria.ALIMENTACAO, 30.0),
        )

        assertEquals(listOf(Categoria.ALIMENTACAO, Categoria.LAVAGEM), Relatorios.doMes(empate, outubro).fatias.map { it.categoria })
    }

    @Test fun `conta os dias, a media, o dia mais caro e o maior gasto`() {
        val relatorio = Relatorios.doMes(gastos, outubro)

        assertEquals(3, relatorio.dias.size)
        assertEquals(18333L, relatorio.mediaPorDia)
        assertEquals(DiaDeGasto(LocalDate.parse("2026-10-14"), 37000L), relatorio.diaMaisCaro)
        assertEquals(Categoria.MANUTENCAO, relatorio.maior?.categoria)
        assertEquals("Manutenção, R$ 250,00 em 14/10 (troca de óleo)", Relatorios.maior(relatorio.maior!!))
    }

    @Test fun `compara com o mes anterior`() {
        val relatorio = Relatorios.doMes(gastos, outubro)

        assertEquals(12000L, relatorio.totalAnterior)
        assertEquals(43000L, relatorio.diferenca)
        assertEquals("R$ 430,00 a mais que em setembro", Relatorios.comparacao(relatorio))
        assertEquals("R$ 510,00 a menos que em outubro", Relatorios.comparacao(Relatorios.doMes(gastos, YearMonth.of(2026, 11))))
    }

    @Test fun `sem mes anterior nao ha comparacao`() {
        val setembro = Relatorios.doMes(gastos, YearMonth.of(2026, 9))

        assertNull(setembro.totalAnterior)
        assertNull(Relatorios.comparacao(setembro))
    }

    @Test fun `janeiro compara com dezembro do ano anterior`() {
        val virada = listOf(
            gasto("2026-12-30", Categoria.COMBUSTIVEL, 80.0),
            gasto("2027-01-02", Categoria.COMBUSTIVEL, 80.0),
        )

        assertEquals("igual a dezembro", Relatorios.comparacao(Relatorios.doMes(virada, YearMonth.of(2027, 1))))
    }

    @Test fun `mes sem gasto fica vazio e nao divide por zero`() {
        val relatorio = Relatorios.doMes(gastos, YearMonth.of(2026, 3))

        assertTrue(relatorio.vazio)
        assertEquals(0L, relatorio.total)
        assertEquals(0L, relatorio.mediaPorDia)
        assertNull(relatorio.diaMaisCaro)
        assertNull(relatorio.maior)
        assertTrue("Nenhum gasto anotado" in Relatorios.texto(relatorio))
    }

    @Test fun `escreve o relatorio para compartilhar`() {
        val texto = Relatorios.texto(Relatorios.doMes(gastos, outubro))

        assertEquals(
            """
            Gastos de outubro de 2026

            Total: R$ 550,00 em 5 lançamentos
            No mês anterior: R$ 120,00 (R$ 430,00 a mais que em setembro)

            Para onde foi
            Combustível: R$ 270,00 (49%)
            Manutenção: R$ 250,00 (45%)
            Alimentação: R$ 30,00 (5%)

            Dias com gasto: 3
            Média nesses dias: R$ 183,33
            Dia mais caro: 14/10, R$ 370,00
            Maior gasto: Manutenção, R$ 250,00 em 14/10 (troca de óleo)

            Anotado no Vale a Corrida
            """.trimIndent(),
            texto.replace("\r\n", "\n"),
        )
    }

    @Test fun `fatia pequena nao vira zero por cento`() {
        assertEquals("menos de 1%", Relatorios.porcento(0.004))
        assertEquals("0%", Relatorios.porcento(0.0))
        assertEquals("100%", Relatorios.porcento(1.0))
    }

    @Test fun `escreve as datas por extenso`() {
        val hoje = LocalDate.parse("2026-10-12")

        assertEquals("outubro de 2026", Datas.mes(outubro))
        assertEquals("Hoje", Datas.diaPorExtenso(hoje, hoje))
        assertEquals("Ontem", Datas.diaPorExtenso(hoje.minusDays(1), hoje))
        assertEquals("quinta, 08/10", Datas.diaPorExtenso(LocalDate.parse("2026-10-08"), hoje))
        assertEquals("domingo, 04/10", Datas.diaPorExtenso(LocalDate.parse("2026-10-04"), hoje))
    }

    @Test fun `le o valor digitado em centavos`() {
        assertEquals(6200L, Formato.lerCentavos("62"))
        assertEquals(6250L, Formato.lerCentavos("62,5"))
        assertEquals(6250L, Formato.lerCentavos("62.50"))
        assertEquals(125000L, Formato.lerCentavos("1.250,00"))
        assertEquals(1999L, Formato.lerCentavos("19,99"))
        assertNull(Formato.lerCentavos(""))
        assertNull(Formato.lerCentavos("0"))
        assertNull(Formato.lerCentavos(","))
        assertNull(Formato.lerCentavos("1000000"))
        assertEquals("R$ 1.250,00", Formato.centavos(125000))
    }
}
