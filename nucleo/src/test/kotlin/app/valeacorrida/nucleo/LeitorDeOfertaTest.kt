package app.valeacorrida.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Cada teste é uma tela, linha por linha, do jeito que a leitura entrega.
 * Quando o Vale a Corrida não reconhecer uma oferta de verdade, a leitura enviada pelo
 * motorista (tela Diagnóstico) entra aqui como um teste novo.
 */
class LeitorDeOfertaTest {

    private fun ler(vararg linhas: String, origem: Origem = Origem.UBER): Oferta? =
        LeitorDeOferta.ler(linhas.map { Trecho(it) }, origem)

    private fun Oferta?.confere(reais: Double, busca: Percurso?, viagem: Percurso) {
        assertNotNull("a oferta não foi reconhecida", this)
        assertEquals("valor", reais, this!!.reais, 0.001)
        assertEquals("busca", busca, this.busca)
        assertEquals("viagem", viagem, this.viagem)
    }

    // ------------------------------------------------------------ Uber

    @Test fun `uber com distancia e viagem`() {
        ler(
            "UberX",
            "R$ 12,48",
            "★ 4,95",
            "6 min (2.1 km) de distância",
            "Rua das Flores, 120, Centro",
            "Viagem de 14 min (7.4 km)",
            "Av. Brasil, 900, Catolé",
            "Selecionar",
        ).confere(12.48, Percurso(6.0, 2.1), Percurso(14.0, 7.4))
    }

    @Test fun `uber com minutos por extenso e viagem no fim da linha`() {
        ler(
            "Comfort",
            "R$ 31,90",
            "9 minutos (3.8 km) de distância",
            "25 minutos (12.4 km) de viagem",
        ).confere(31.90, Percurso(9.0, 3.8), Percurso(25.0, 12.4))
    }

    /**
     * A primeira oferta de verdade que chegou: Uber, Envios Moto, outubro de 2026, lida da
     * acessibilidade de um Xiaomi com Android 15. Os pedaços e os lugares são os que vieram,
     * com o espaço que não quebra linha em "R$ 4,20" e em "17 min (4.5 km)". Só os endereços
     * foram trocados por inventados. A Uber mostra um trecho só, o total, sem separar a busca.
     */
    @Test fun `uber envios moto como veio do celular`() {
        val linhas = Linhas.agrupar(
            listOf(
                Fragmento("Envios Moto", esquerda = 176, topo = 1182, largura = 277, altura = 90),
                Fragmento("R$ 4,20", esquerda = 88, topo = 1272, largura = 411, altura = 154),
                Fragmento("4,85 (3816)", esquerda = 176, topo = 1426, largura = 251, altura = 90),
                Fragmento("Verificado", esquerda = 537, topo = 1426, largura = 235, altura = 90),
                Fragmento("17 min (4.5 km)", esquerda = 176, topo = 1585, largura = 355, altura = 77),
                Fragmento("Rua das Flores, 658 - Centro, Campina Grande - PB, 58400-000, Brasil", esquerda = 176, topo = 1709, largura = 816, altura = 176),
                Fragmento("Rua do Sol, 465, Campina Grande, Campina Grande", esquerda = 176, topo = 1885, largura = 816, altura = 121),
                Fragmento("Selecionar", esquerda = 88, topo = 2050, largura = 904, altura = 132),
            ),
        )
        LeitorDeOferta.ler(linhas, Origem.UBER).confere(4.20, null, Percurso(17.0, 4.5))
    }

    @Test fun `ganhos do dia no topo nao sao o valor da corrida`() {
        val oferta = LeitorDeOferta.ler(
            listOf(
                Trecho("R$ 152,30", altura = 40),
                Trecho("UberX", altura = 36),
                Trecho("R$ 18,70", altura = 84),
                Trecho("4 min (1.2 km) de distância", altura = 40),
                Trecho("Viagem de 22 min (9.6 km)", altura = 40),
            ),
            Origem.UBER,
        )
        oferta.confere(18.70, Percurso(4.0, 1.2), Percurso(22.0, 9.6))
    }

    @Test fun `sem tamanho de letra vale o valor logo acima do primeiro trecho`() {
        ler(
            "R$ 152,30",
            "Você está online",
            "R$ 18,70",
            "4 min (1.2 km) de distância",
            "Viagem de 22 min (9.6 km)",
        ).confere(18.70, Percurso(4.0, 1.2), Percurso(22.0, 9.6))
    }

    @Test fun `taxa por km e adicional incluido nao sao o valor`() {
        ler(
            "R$ 24,00",
            "R$ 1,85/km",
            "+R$ 4,00 incluído",
            "7 min (2.9 km) de distância",
            "Viagem de 19 min (10.1 km)",
        ).confere(24.00, Percurso(7.0, 2.9), Percurso(19.0, 10.1))
    }

    @Test fun `viagem longa com horas`() {
        ler(
            "R$ 142,00",
            "12 min (5.0 km) de distância",
            "Viagem de 1 h 25 min (96 km)",
        ).confere(142.00, Percurso(12.0, 5.0), Percurso(85.0, 96.0))
    }

    @Test fun `horas no formato compacto`() {
        ler("R$ 142,00", "Viagem de 1h05 (70 km)").confere(142.00, null, Percurso(65.0, 70.0))
    }

    @Test fun `hora sem minutos nao engole a distancia`() {
        ler("R$ 90,00", "Viagem de 1 h 62 km").confere(90.00, null, Percurso(60.0, 62.0))
    }

    @Test fun `nota do passageiro`() {
        assertEquals(4.95, ler("R$ 12,48", "★ 4,95", "6 min (2.1 km)", "14 min (7.4 km)")!!.nota!!, 0.001)
        assertEquals(4.8, ler("R$ 12,48", "4,80", "6 min (2.1 km)", "14 min (7.4 km)")!!.nota!!, 0.001)
        assertNull(ler("R$ 12,48", "6 min (2.1 km)", "14 min (7.4 km)")!!.nota)
    }

    // ------------------------------------------------------------ 99

    @Test fun `99 com distancia antes do tempo`() {
        ler(
            "99Pop",
            "R$ 15,30",
            "1,2 km · 4 min",
            "Rua Treze de Maio, 45",
            "8,0 km · 18 min",
            "Rua João Pessoa, 300",
            "Aceitar",
            origem = Origem.NOVENTA_E_NOVE,
        ).confere(15.30, Percurso(4.0, 1.2), Percurso(18.0, 8.0))
    }

    @Test fun `rotulos acima dos trechos`() {
        ler(
            "R$ 22,10",
            "Viagem",
            "21 min · 11,3 km",
            "Até o passageiro",
            "5 min · 900 m",
            origem = Origem.NOVENTA_E_NOVE,
        ).confere(22.10, Percurso(5.0, 0.9), Percurso(21.0, 11.3))
    }

    @Test fun `um trecho so e a viagem`() {
        ler("R$ 9,80", "12 min (4,5 km)", origem = Origem.NOVENTA_E_NOVE)
            .confere(9.80, null, Percurso(12.0, 4.5))
    }

    @Test fun `tela so com distancias`() {
        ler("R$ 19,00", "1,5 km", "Rua A", "9,2 km", "Rua B", origem = Origem.INDRIVE)
            .confere(19.00, Percurso(null, 1.5), Percurso(null, 9.2))
    }

    @Test fun `paradas no caminho somam na viagem`() {
        ler(
            "R$ 40,00",
            "3 min (1.0 km)",
            "10 min (4.0 km)",
            "12 min (6.5 km)",
        ).confere(40.00, Percurso(3.0, 1.0), Percurso(22.0, 10.5))
    }

    // ------------------------------------------------------------ leitura por imagem

    @Test fun `tropecos da leitura por imagem`() {
        ler(
            "UberX",
            "RS 12,48",
            "6 min (2.1 krn) de distancia",
            "Viagem de 14 min (7.4 KM)",
        ).confere(12.48, Percurso(6.0, 2.1), Percurso(14.0, 7.4))
    }

    @Test fun `tempo e distancia em linhas vizinhas`() {
        ler(
            "R$ 12,48",
            "6 min",
            "2.1 km",
            "Rua das Flores",
            "14 min",
            "7.4 km",
        ).confere(12.48, Percurso(6.0, 2.1), Percurso(14.0, 7.4))
    }

    @Test fun `texto do mapa nao atrapalha`() {
        ler(
            "Farmácia 24h",
            "BR 230 KM 12",
            "R$ 12,48",
            "6 min (2.1 km) de distância",
            "Viagem de 14 min (7.4 km)",
        ).confere(12.48, Percurso(6.0, 2.1), Percurso(14.0, 7.4))
    }

    @Test fun `valor com milhar e sem centavos`() {
        ler("R$ 1.240,00", "Viagem de 9 h 10 min (640 km)").confere(1240.00, null, Percurso(550.0, 640.0))
        ler("R$ 15", "5 min (1,8 km)", "11 min (6,0 km)").confere(15.00, Percurso(5.0, 1.8), Percurso(11.0, 6.0))
    }

    // ------------------------------------------------------------ o que não é oferta

    @Test fun `tela sem oferta`() {
        assertNull(ler("R$ 152,30", "Você está online", "Procurando viagens"))
        assertNull(ler("6 min (2.1 km) de distância", "Viagem de 14 min (7.4 km)"))
        assertNull(ler())
    }

    @Test fun `so a busca nao basta`() {
        assertNull(ler("R$ 12,48", "6 min (2.1 km) até o passageiro"))
    }

    @Test fun `valor ou distancia absurdos sao descartados`() {
        assertNull(ler("R$ 0,50", "6 min (2.1 km)", "14 min (7.4 km)"))
        assertNull(ler("R$ 12,48", "Viagem de 14 min (7400 km)"))
    }

    // ------------------------------------------------------------ cara de oferta (diagnóstico)

    private fun parece(vararg linhas: String) = LeitorDeOferta.pareceOferta(linhas.map { Trecho(it) })

    @Test fun `tela com valor e trecho tem cara de oferta mesmo sem ser reconhecida`() {
        assertEquals(true, parece("R$ 12,48", "linha que o leitor não entendeu", "a 6 min de você"))
        assertEquals(true, parece("RS 12,48", "2.1 km"))
    }

    @Test fun `o espaco que o Android poe entre o cifrao e o numero nao esconde o valor`() {
        // É como o Android escreve dinheiro em português: "R$", um espaço que não quebra linha, o número.
        assertEquals(true, parece("R$ 12,48", "6 min (2,1 km) de distância"))
        ler("R$ 12,48", "6 min (2,1 km) de distância", "Viagem de 14 min (7,4 km)")
            .confere(12.48, Percurso(6.0, 2.1), Percurso(14.0, 7.4))
    }

    @Test fun `tela parada nao tem cara de oferta`() {
        assertEquals(false, parece("R$ 152,30", "Você está online", "Procurando viagens"))
        assertEquals(false, parece("6 min (2.1 km) de distância"))
        assertEquals(false, parece())
    }
}

class LinhasTest {

    @Test fun `junta os pedacos da mesma linha`() {
        val linhas = Linhas.agrupar(
            listOf(
                Fragmento("12,48", esquerda = 140, topo = 600, largura = 220, altura = 84),
                Fragmento("R$", esquerda = 60, topo = 604, largura = 60, altura = 40),
                Fragmento("(2.1 km) de distância", esquerda = 190, topo = 720, largura = 400, altura = 40),
                Fragmento("6 min", esquerda = 60, topo = 720, largura = 110, altura = 40),
                Fragmento("Viagem de 14 min (7.4 km)", esquerda = 60, topo = 860, largura = 520, altura = 40),
            ),
        )
        assertEquals(
            listOf("R$ 12,48", "6 min (2.1 km) de distância", "Viagem de 14 min (7.4 km)"),
            linhas.map { it.texto },
        )
        assertEquals(84, linhas.first().altura)
        LeitorDeOferta.ler(linhas, Origem.UBER).let {
            assertEquals(1248L, it!!.centavos)
            assertEquals(Percurso(6.0, 2.1), it.busca)
        }
    }

    @Test fun `caixa com varias linhas vira uma linha por texto`() {
        val linhas = Linhas.agrupar(
            listOf(Fragmento("R$ 9,80\n12 min (4,5 km)", esquerda = 0, topo = 100, largura = 300, altura = 120)),
        )
        assertEquals(listOf("R$ 9,80", "12 min (4,5 km)"), linhas.map { it.texto })
    }
}
