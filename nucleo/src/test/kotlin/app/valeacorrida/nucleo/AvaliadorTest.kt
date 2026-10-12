package app.valeacorrida.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvaliadorTest {

    private fun oferta(reais: Double, busca: Percurso?, viagem: Percurso, nota: Double? = null) =
        Oferta(Origem.UBER, Math.round(reais * 100), busca, viagem, nota)

    private val padrao = Criterios() // por km 1,50 e 2,00; por hora 25 e 35

    @Test fun `faz as contas somando a busca`() {
        val a = Avaliador.avaliar(oferta(20.0, Percurso(5.0, 2.0), Percurso(15.0, 8.0)), padrao)
        assertEquals(10.0, a.km, 0.001)
        assertEquals(20.0, a.minutos!!, 0.001)
        assertEquals(2.0, a.porKm, 0.001)
        assertEquals(60.0, a.porHora!!, 0.001)
        assertEquals(Veredito.BOA, a.veredito)
        assertEquals(emptyList<Motivo>(), a.motivos)
        assertNull(a.sobra)
    }

    @Test fun `sem contar a busca a conta e so a viagem`() {
        val a = Avaliador.avaliar(
            oferta(20.0, Percurso(5.0, 2.0), Percurso(15.0, 8.0)),
            padrao.copy(contarBusca = false),
        )
        assertEquals(8.0, a.km, 0.001)
        assertEquals(15.0, a.minutos!!, 0.001)
        assertEquals(2.5, a.porKm, 0.001)
    }

    @Test fun `entre o minimo e o bom e media`() {
        val a = Avaliador.avaliar(oferta(17.0, null, Percurso(20.0, 10.0)), padrao) // 1,70/km e 51/h
        assertEquals(Veredito.MEDIA, a.veredito)
        assertEquals(listOf(Motivo.POR_KM_MEDIO), a.motivos)
    }

    @Test fun `abaixo do minimo por km e ruim`() {
        val a = Avaliador.avaliar(oferta(12.0, null, Percurso(20.0, 10.0)), padrao) // 1,20/km
        assertEquals(Veredito.RUIM, a.veredito)
        assertEquals(listOf(Motivo.POR_KM_BAIXO), a.motivos)
    }

    @Test fun `transito pesado derruba o ganho por hora`() {
        val a = Avaliador.avaliar(oferta(12.0, null, Percurso(40.0, 5.0)), padrao) // 2,40/km e 18/h
        assertEquals(Veredito.RUIM, a.veredito)
        assertEquals(listOf(Motivo.POR_HORA_BAIXO), a.motivos)
    }

    @Test fun `limites extras`() {
        val criterios = padrao.copy(valorMinimo = 10.0, buscaMaximaKm = 3.0, notaMinima = 4.7)
        val a = Avaliador.avaliar(oferta(8.0, Percurso(8.0, 3.5), Percurso(4.0, 0.5), nota = 4.5), criterios)
        assertEquals(Veredito.RUIM, a.veredito)
        assertEquals(listOf(Motivo.VALOR_BAIXO, Motivo.BUSCA_LONGA, Motivo.NOTA_BAIXA), a.motivos)
    }

    @Test fun `criterio zerado nao conta`() {
        val livre = Criterios(porKmMinimo = 0.0, porKmBom = 0.0, porHoraMinimo = 0.0, porHoraBom = 0.0)
        assertEquals(Veredito.BOA, Avaliador.avaliar(oferta(5.0, null, Percurso(60.0, 30.0)), livre).veredito)
    }

    @Test fun `sem tempo na tela nao ha ganho por hora`() {
        val a = Avaliador.avaliar(oferta(20.0, Percurso(null, 2.0), Percurso(null, 8.0)), padrao)
        assertNull(a.minutos)
        assertNull(a.porHora)
        assertEquals(Veredito.BOA, a.veredito)
    }

    @Test fun `sobra depois do custo por km`() {
        val a = Avaliador.avaliar(oferta(20.0, Percurso(5.0, 2.0), Percurso(15.0, 8.0)), padrao.copy(custoPorKm = 0.60))
        assertEquals(14.0, a.sobra!!, 0.001)
    }
}

class FormatoTest {

    @Test fun `escreve do jeito brasileiro`() {
        assertEquals("R$ 2,13", Formato.reais(2.134))
        assertEquals("R$ 1.240,00", Formato.reais(1240.0))
        assertEquals("9,5", Formato.numero(9.5))
        assertEquals("45 min", Formato.minutos(45.2))
        assertEquals("1 h 25 min", Formato.minutos(85.0))
    }

    @Test fun `le o que a pessoa digita`() {
        assertEquals(2.5, Formato.ler("2,5")!!, 0.001)
        assertEquals(2.5, Formato.ler("2.5")!!, 0.001)
        assertEquals(1240.0, Formato.ler("R$ 1.240,00")!!, 0.001)
        assertNull(Formato.ler("abc"))
        assertNull(Formato.ler(""))
    }
}
