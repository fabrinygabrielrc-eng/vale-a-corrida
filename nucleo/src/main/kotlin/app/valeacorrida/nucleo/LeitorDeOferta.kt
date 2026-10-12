package app.valeacorrida.nucleo

import java.text.Normalizer

/**
 * Acha a oferta de corrida no texto da tela.
 *
 * Não depende do desenho de um aplicativo específico: procura um valor em reais e os trechos
 * de "tempo + distância". Assim continua funcionando quando Uber ou 99 mudam o cartão de lugar,
 * e serve tanto para o texto lido pela acessibilidade quanto para o lido da imagem da tela.
 */
object LeitorDeOferta {

    fun ler(trechos: List<Trecho>, origem: Origem): Oferta? {
        val linhas = trechos.map { Linha(it.texto, normalizar(it.texto), it.altura) }
        val valores = valoresEmReais(linhas)
        if (valores.isEmpty()) return null

        val caminho = caminho(linhas) ?: return null
        val valor = escolherValor(valores, caminho.primeiraLinha)

        val oferta = Oferta(origem, valor.centavos, caminho.busca, caminho.viagem, nota(linhas))
        return oferta.takeIf(::plausivel)
    }

    // ---------------------------------------------------------------- texto

    private class Linha(val original: String, val texto: String, val altura: Int)

    private val ACENTOS = Regex("\\p{Mn}+")
    private val ESPACOS = Regex("[\\s\\u00a0\\u2007\\u2009\\u202f]+")
    private val KM_MAL_LIDO = Regex("(?<=\\d)\\s?(?:krn|kn|krm)\\b")
    private val CIFRAO_SEPARADO = Regex("\\br\\s+\\$")

    /** Minúsculas, sem acento, espaços simples e os tropeços comuns da leitura por imagem. */
    internal fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(ACENTOS, "")
            .replace(ESPACOS, " ")
            .replace(KM_MAL_LIDO) { " km" }
            .replace(CIFRAO_SEPARADO) { "r$" }
            .trim()

    // ---------------------------------------------------------------- valor

    private class Valor(val linha: Int, val centavos: Long, val altura: Int)

    // "r$ 12,34", "r$12.34", "r$ 1.234,56", "r$ 15". Na leitura por imagem o cifrão vira "rs" ou "r5".
    private val DINHEIRO = Regex("(?<![a-z0-9])(r\\$|rs|r5)\\s?(\\d{1,3}(?:\\.\\d{3})+|\\d{1,5})(?:[,.](\\d{1,2}))?(?!\\d)")
    private val E_TAXA = Regex("^\\s?(?:/|por (?:km|hora|min)|km\\b|h\\b)")
    private val E_ADICIONAL = Regex("inclu|bonus|gorjeta|promo")

    private fun valoresEmReais(linhas: List<Linha>): List<Valor> = buildList {
        linhas.forEachIndexed { indice, linha ->
            if (E_ADICIONAL.containsMatchIn(linha.texto)) return@forEachIndexed
            for (achado in DINHEIRO.findAll(linha.texto)) {
                val (cifrao, inteiro, decimal) = achado.destructured
                // "rs" e "r5" só valem como cifrão quando o número tem centavos.
                if (cifrao != "r$" && decimal.isEmpty()) continue
                // "R$ 1,85/km" é taxa (a do próprio aplicativo), não o valor da corrida.
                if (E_TAXA.containsMatchIn(linha.texto.substring(achado.range.last + 1))) continue
                // "+R$ 3,00" é adicional que já está somado no valor principal.
                if (linha.texto.substring(0, achado.range.first).trimEnd().endsWith("+")) continue
                val centavos = inteiro.replace(".", "").toLong() * 100 + decimal.padEnd(2, '0').toLong()
                add(Valor(indice, centavos, linha.altura))
            }
        }
    }

    /**
     * O valor da corrida é o que aparece em letra maior. Sem essa pista, é o que está logo
     * acima do primeiro trecho (os ganhos do dia, no topo da tela, ficam mais longe).
     */
    private fun escolherValor(valores: List<Valor>, primeiraLinhaDoCaminho: Int): Valor {
        if (valores.size == 1) return valores.first()
        val maior = valores.maxOf { it.altura }
        if (maior > 0) {
            val segundo = valores.filter { it.altura < maior }.maxOfOrNull { it.altura } ?: 0
            val destaques = valores.filter { it.altura == maior }
            if (destaques.size == 1 && maior >= segundo * 1.2) return destaques.first()
        }
        return valores.lastOrNull { it.linha <= primeiraLinhaDoCaminho } ?: valores.first()
    }

    // ---------------------------------------------------------------- caminho

    private class Caminho(val busca: Percurso?, val viagem: Percurso, val primeiraLinha: Int)
    private enum class Medida { TEMPO, DISTANCIA }
    private class Marca(val medida: Medida, val linha: Int, val posicao: Int, val valor: Double)
    private enum class Tipo { BUSCA, VIAGEM, INDEFINIDO }
    private class Par(val percurso: Percurso, val linha: Int, val tipo: Tipo)

    // "1 h 5 min", "1 hora e 5 minutos", "2 h" e o compacto "1h05". Grupos: horas, minutos colados, minutos.
    private val HORAS = Regex("(?<![\\d.,])(\\d{1,2})\\s?(?:h(\\d{2})\\b|(?:h|hr|hrs|hora|horas)\\b(?:\\s?(?:e\\s)?(\\d{1,2})\\s?(?:min|mins|minuto|minutos)\\b)?)")
    private val MINUTOS = Regex("(?<![\\d.,])(\\d{1,3})\\s?(?:min|mins|minuto|minutos)\\b")
    private val DISTANCIA = Regex("(?<![\\d.,])(\\d{1,4}(?:[.,]\\d{1,2})?)\\s?(km|m)\\b")
    private val DICA_VIAGEM = Regex("viagem|destino|trajeto|corrida")
    private val DICA_BUSCA = Regex("distancia|passageiro|embarque|busca|retirada|coleta|ate voce|de voce")
    private val TEM_NUMERO = Regex("\\d")

    private fun caminho(linhas: List<Linha>): Caminho? {
        val marcas = linhas.flatMapIndexed { indice, linha -> marcas(indice, linha.texto) }
        var pares = pares(marcas, linhas)
        if (pares.isEmpty()) {
            // A tela só mostra distâncias: vale assim mesmo, sem o ganho por hora.
            pares = marcas.filter { it.medida == Medida.DISTANCIA }
                .map { Par(Percurso(null, it.valor), it.linha, tipo(linhas, it.linha, it.linha)) }
        }
        if (pares.isEmpty()) return null

        val viagens = pares.filter { it.tipo == Tipo.VIAGEM }
        val buscas = pares.filter { it.tipo == Tipo.BUSCA }
        val indefinidos = pares.filter { it.tipo == Tipo.INDEFINIDO }

        val busca: List<Par>
        val viagem: List<Par>
        when {
            viagens.isNotEmpty() -> {
                viagem = viagens
                // Sem a palavra que identifica a busca, ela é o trecho que vem antes da viagem.
                busca = buscas.ifEmpty { indefinidos.filter { it.linha < viagens.first().linha }.takeLast(1) }
            }
            buscas.isNotEmpty() -> {
                busca = buscas
                viagem = indefinidos
            }
            indefinidos.size >= 2 -> {
                busca = indefinidos.take(1)
                viagem = indefinidos.drop(1)
            }
            else -> {
                busca = emptyList()
                viagem = indefinidos
            }
        }
        if (viagem.isEmpty()) return null
        return Caminho(somar(busca), somar(viagem)!!, pares.minOf { it.linha })
    }

    private fun marcas(indice: Int, texto: String): List<Marca> {
        val achadas = mutableListOf<Marca>()
        val horas = HORAS.findAll(texto).toList()
        for (h in horas) {
            val resto = h.groupValues[2].toDoubleOrNull() ?: h.groupValues[3].toDoubleOrNull() ?: 0.0
            val minutos = h.groupValues[1].toDouble() * 60 + resto
            achadas += Marca(Medida.TEMPO, indice, h.range.first, minutos)
        }
        for (m in MINUTOS.findAll(texto)) {
            if (horas.any { m.range.first in it.range }) continue
            achadas += Marca(Medida.TEMPO, indice, m.range.first, m.groupValues[1].toDouble())
        }
        for (d in DISTANCIA.findAll(texto)) {
            val numero = d.groupValues[1].replace(',', '.').toDouble()
            achadas += Marca(Medida.DISTANCIA, indice, d.range.first, if (d.groupValues[2] == "m") numero / 1000 else numero)
        }
        // Um dia inteiro de viagem não é oferta: é "Farmácia 24h" no mapa.
        return achadas.filter { it.medida == Medida.DISTANCIA || it.valor <= 600 }.sortedBy { it.posicao }
    }

    /** Casa cada tempo com a distância vizinha: primeiro na mesma linha, depois na linha de baixo. */
    private fun pares(marcas: List<Marca>, linhas: List<Linha>): List<Par> {
        val pares = mutableListOf<Par>()
        val sobras = mutableListOf<Marca>()

        fun casar(candidatas: List<Marca>, podeCasar: (Marca, Marca) -> Boolean, resto: MutableList<Marca>) {
            var i = 0
            while (i < candidatas.size) {
                val a = candidatas[i]
                val b = candidatas.getOrNull(i + 1)
                if (b != null && a.medida != b.medida && podeCasar(a, b)) {
                    val tempo = if (a.medida == Medida.TEMPO) a else b
                    val distancia = if (a.medida == Medida.DISTANCIA) a else b
                    pares += Par(Percurso(tempo.valor, distancia.valor), a.linha, tipo(linhas, a.linha, b.linha))
                    i += 2
                } else {
                    resto += a
                    i += 1
                }
            }
        }

        casar(marcas, { a, b -> a.linha == b.linha }, sobras)
        casar(sobras, { a, b -> b.linha - a.linha == 1 }, mutableListOf())
        return pares.sortedBy { it.linha }
    }

    /** Lê a pista nas linhas do trecho e no rótulo logo acima ("Até o passageiro", "Viagem"). */
    private fun tipo(linhas: List<Linha>, de: Int, ate: Int): Tipo {
        var contexto = (de..ate).joinToString(" ") { linhas[it].texto }
        val acima = linhas.getOrNull(de - 1)?.texto
        if (acima != null && !TEM_NUMERO.containsMatchIn(acima)) contexto = "$acima $contexto"
        val viagem = DICA_VIAGEM.containsMatchIn(contexto)
        val busca = DICA_BUSCA.containsMatchIn(contexto)
        return when {
            viagem && !busca -> Tipo.VIAGEM
            busca && !viagem -> Tipo.BUSCA
            else -> Tipo.INDEFINIDO
        }
    }

    private fun somar(pares: List<Par>): Percurso? {
        if (pares.isEmpty()) return null
        val minutos = if (pares.any { it.percurso.minutos == null }) null else pares.sumOf { it.percurso.minutos!! }
        return Percurso(minutos, pares.sumOf { it.percurso.km })
    }

    // ---------------------------------------------------------------- nota

    private const val ESTRELAS = "[★⭐✭☆]"
    private val NOTA_COM_ESTRELA = Regex("$ESTRELAS\\s?([1-5][.,]\\d{1,2})|([1-5][.,]\\d{1,2})\\s?$ESTRELAS")
    private val NOTA_SOZINHA = Regex("^[45][.,]\\d{2}$")

    private fun nota(linhas: List<Linha>): Double? {
        for (linha in linhas) {
            val achado = NOTA_COM_ESTRELA.find(linha.original)
            val texto = achado?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
                ?: linha.texto.takeIf { NOTA_SOZINHA.matches(it) }
                ?: continue
            val nota = texto.replace(',', '.').toDouble()
            if (nota in 1.0..5.0) return nota
        }
        return null
    }

    // ---------------------------------------------------------------- conferência

    /** Descarta leitura que não pode ser corrida: valor ou distância fora do razoável. */
    private fun plausivel(oferta: Oferta): Boolean {
        val km = oferta.viagem.km + (oferta.busca?.km ?: 0.0)
        return oferta.centavos in 300L..300_000L && km in 0.1..1500.0
    }
}
