package app.valeacorrida

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import app.valeacorrida.leitura.LeitorDeTela
import app.valeacorrida.nucleo.Criterios
import app.valeacorrida.nucleo.LeitorDeOferta
import app.valeacorrida.nucleo.Linhas
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Percurso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

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
}
