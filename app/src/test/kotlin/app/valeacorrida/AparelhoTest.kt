package app.valeacorrida

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import app.valeacorrida.leitura.LeitorDeTela
import app.valeacorrida.leitura.areaDeLeitura
import app.valeacorrida.nucleo.Categoria
import app.valeacorrida.nucleo.Criterios
import app.valeacorrida.nucleo.LeitorDeOferta
import app.valeacorrida.nucleo.Linhas
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Percurso
import app.valeacorrida.ui.Estado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** O que depende do Android, conferido num Android de mentira (Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AparelhoTest {
    private val contexto get() = RuntimeEnvironment.getApplication()

    private fun no(texto: String?, esquerda: Int, topo: Int, largura: Int, altura: Int, descricao: String? = null) =
        AccessibilityNodeInfo.obtain().apply {
            text = texto
            contentDescription = descricao
            isVisibleToUser = true
            setBoundsInScreen(Rect(esquerda, topo, esquerda + largura, topo + altura))
        }

    @Test fun `le a oferta da arvore de acessibilidade`() {
        val cartao = no(null, 0, 500, 1080, 900, descricao = "repete o texto dos filhos: R$ 99,99 em 1 min (1.0 km)")
        val raiz = no(null, 0, 0, 1080, 2400)
        shadowOf(raiz).addChild(no("R$ 152,30", 400, 80, 280, 60))
        shadowOf(raiz).addChild(cartao)
        // O valor vem em duas caixas, e o tempo separado da distância: o leitor tem que juntar.
        shadowOf(cartao).addChild(no("R$", 60, 620, 70, 50))
        shadowOf(cartao).addChild(no("18,70", 140, 600, 260, 96))
        shadowOf(cartao).addChild(no("4 min", 60, 760, 120, 48))
        shadowOf(cartao).addChild(no("(1.2 km) de distância", 190, 760, 420, 48))
        shadowOf(cartao).addChild(no("Viagem de 22 min (9.6 km)", 60, 900, 560, 48))
        shadowOf(cartao).addChild(no("invisível: 50 min (30 km)", 60, 1000, 560, 48).apply { isVisibleToUser = false })

        val oferta = LeitorDeOferta.ler(Linhas.agrupar(LeitorDeTela.fragmentos(raiz)), Origem.UBER)!!

        assertEquals(1870L, oferta.centavos)
        assertEquals(Percurso(4.0, 1.2), oferta.busca)
        assertEquals(Percurso(22.0, 9.6), oferta.viagem)
    }

    @Test fun `guarda e devolve os valores da pessoa`() {
        val preferencias = Preferencias(contexto)
        assertEquals(Criterios(), preferencias.criterios)
        assertEquals(Origem.DE_CORRIDA.toSet(), preferencias.acompanhados)

        val novos = Criterios(1.8, 2.35, 28.0, 40.0, 8.0, 3.5, 4.75, 0.65, contarBusca = false)
        preferencias.criterios = novos
        preferencias.acompanhados = setOf(Origem.UBER)
        preferencias.posicao = Posicao.BASE

        val relido = Preferencias(contexto)
        assertEquals(novos, relido.criterios)
        assertEquals(setOf(Origem.UBER), relido.acompanhados)
        assertEquals(Posicao.BASE, relido.posicao)
    }

    @Test fun `diagnostico guarda as ultimas leituras sem repetir`() {
        Diagnostico.apagar(contexto)
        fun leitura(texto: String) = Diagnostico.Leitura(1, "Uber", "texto", false, listOf("R$ 10,00", texto))
        Diagnostico.registrar(contexto, leitura("a"))
        Diagnostico.registrar(contexto, leitura("a"))
        repeat(30) { Diagnostico.registrar(contexto, leitura("oferta $it")) }

        val guardadas = Diagnostico.listar(contexto)
        assertEquals(20, guardadas.size)
        assertEquals("oferta 29", guardadas.first().linhas.last())
        assertTrue("NÃO reconhecida" in Diagnostico.texto(guardadas, "0.1.0"))
        Diagnostico.apagar(contexto)
        assertEquals(0, Diagnostico.listar(contexto).size)
    }

    @Test fun `rastro junta as leituras iguais, conta os avisos e volta depois de reabrir`() {
        Rastro.apagar(contexto)
        assertTrue(Rastro.vazio(contexto))

        Rastro.servicoLigou(contexto, 1_000)
        Rastro.servicoLigou(contexto, 1_000)
        repeat(3) { Rastro.aviso(contexto, "Uber", 2_000L + it) }
        fun passo(quando: Long, imagem: String) = Rastro.Passo(quando, "Uber", "janelas 1 de 3, 0 pedaços", imagem, false)
        Rastro.passo(contexto, passo(3_000, "0 linhas"))
        Rastro.passo(contexto, passo(4_000, "0 linhas"))
        Rastro.passo(contexto, passo(5_000, "12 linhas (valor sim, trecho sim)"))
        Rastro.gravar(contexto)
        Rastro.soltar()

        val parado = Rastro.linhas(contexto, servicoDesde = null)
        assertTrue(parado.first().startsWith("O serviço de leitura não está rodando"))
        assertTrue(parado.any { it.startsWith("Uber: 3 avisos de mudança de tela") })
        assertEquals(2, parado.count { " Uber | texto: janelas 1 de 3, 0 pedaços | imagem: " in it })
        assertTrue(parado.any { "imagem: 0 linhas | NÃO reconhecida | 2 vezes" in it })
        assertTrue(Rastro.linhas(contexto, servicoDesde = 1_000).first().startsWith("O serviço de leitura está rodando desde"))
        assertEquals(1, Rastro.linhas(contexto, null, quantosPassos = 1).count { " Uber | texto: " in it })

        // Sem nenhuma leitura guardada, o texto compartilhado ainda leva o aparelho e o rastro.
        val texto = Diagnostico.texto(emptyList(), "0.2.1", "Marca Modelo, Android 15 (API 35)", parado)
        assertTrue("Aparelho: Marca Modelo, Android 15 (API 35)" in texto)
        assertTrue("Uber: 3 avisos de mudança de tela" in texto)
        assertTrue("Nenhuma tela com cara de oferta foi guardada." in texto)

        Rastro.apagar(contexto)
        assertTrue(Rastro.vazio(contexto))
    }

    @Test fun `da captura so se le o pedaco do aplicativo de corrida`() {
        assertEquals(Rect(0, 0, 1080, 2400), areaDeLeitura(1080, 2400, null))
        assertEquals(Rect(0, 600, 1080, 2400), areaDeLeitura(1080, 2400, Rect(0, 600, 1080, 2600)))
        // O botão flutuante por cima de outro aplicativo: nada a ler, em vez da tela inteira.
        assertNull(areaDeLeitura(1080, 2400, Rect(900, 1000, 1060, 1160)))
        assertNull(areaDeLeitura(1080, 2400, Rect(2000, 3000, 2500, 3500)))
    }

    private val arquivoDeGastos get() = File(contexto.filesDir, "gastos.json")

    @Test fun `guarda os gastos e os devolve depois de reabrir`() {
        val livro = LivroDeGastos(contexto)
        val posto = livro.anotar(LocalDate.parse("2026-10-12"), Categoria.COMBUSTIVEL, 12000, "  posto da avenida  ")!!
        val lanche = livro.anotar(LocalDate.parse("2026-10-12"), Categoria.ALIMENTACAO, 1850)!!

        assertEquals("posto da avenida", posto.nota)
        assertEquals(posto.id + 1, lanche.id)
        assertEquals(listOf(posto, lanche), LivroDeGastos(contexto).listar())

        LivroDeGastos(contexto).apagar(posto.id)
        assertEquals(listOf(lanche), LivroDeGastos(contexto).listar())
    }

    @Test fun `arquivo de gastos ilegivel e guardado antes de comecar do zero`() {
        val estragado = "{ isto não é o arquivo de gastos"
        arquivoDeGastos.writeText(estragado)

        val livro = LivroDeGastos(contexto)
        assertEquals(0, livro.listar().size)
        livro.anotar(LocalDate.parse("2026-10-12"), Categoria.LAVAGEM, 3500)

        assertEquals(estragado, File(contexto.filesDir, "gastos-ilegivel.json").readText())
        assertEquals(1, LivroDeGastos(contexto).listar().size)
    }

    @Test fun `categoria que esta versao nao conhece vira Outros`() {
        arquivoDeGastos.writeText(
            """{"versao":2,"gastos":[{"id":7,"dia":"2026-10-12","categoria":"PNEU","centavos":40000,"nota":"dois pneus"}]}""",
        )

        val gasto = LivroDeGastos(contexto).listar().single()

        assertEquals(Categoria.OUTROS, gasto.categoria)
        assertEquals(40000L, gasto.centavos)
        assertEquals("dois pneus", gasto.nota)
    }

    @Test fun `avisa do relatorio do mes que fechou ate ele ser aberto`() {
        val hoje = LocalDate.parse("2026-10-12")
        val setembro = YearMonth.of(2026, 9)
        fun estado() = Estado(Preferencias(contexto), LivroDeGastos(contexto), hoje = { hoje })

        val estado = estado()
        assertNull(estado.relatorioPronto)

        assertTrue(estado.anotarGasto(LocalDate.parse("2026-09-20"), Categoria.COMBUSTIVEL, 10000, ""))
        assertEquals(setembro, estado.relatorioPronto)

        // Abrir o relatório do mês corrente não conta: ele ainda vai mudar.
        estado.verRelatorio(true)
        assertEquals(setembro, estado.relatorioPronto)

        estado.abrirRelatorio(setembro)
        assertNull(estado.relatorioPronto)
        assertNull(estado().relatorioPronto)
    }
}
