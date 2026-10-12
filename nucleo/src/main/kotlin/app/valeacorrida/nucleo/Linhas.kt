package app.valeacorrida.nucleo

/** Um pedaço de texto lido da tela, com o lugar que ele ocupa (em pixels). */
data class Fragmento(
    val texto: String,
    val esquerda: Int,
    val topo: Int,
    val largura: Int,
    val altura: Int,
) {
    val base: Int get() = topo + altura
    val centro: Double get() = topo + altura / 2.0
}

/** Uma linha de texto da tela. `altura` é 0 quando a leitura não informa o tamanho da letra. */
data class Trecho(val texto: String, val altura: Int = 0)

object Linhas {
    /** `maior` é o pedaço mais alto da linha: é ele que diz onde a linha começa e termina. */
    private class Linha(var maior: Fragmento, val partes: MutableList<Fragmento>)

    /**
     * Monta as linhas da tela, de cima para baixo, juntando os pedaços que estão lado a lado.
     *
     * Os aplicativos costumam desenhar "R$" e "12,34", ou "6 min" e "(2,1 km)", em caixas
     * separadas. Sem juntar, o leitor não reconhece o valor nem o trecho.
     */
    fun agrupar(fragmentos: List<Fragmento>): List<Trecho> {
        val soltos = fragmentos.flatMap(::separarLinhas).filter { it.texto.isNotBlank() }
        val linhas = mutableListOf<Linha>()
        for (f in soltos.sortedBy { it.centro }) {
            val atual = linhas.lastOrNull()
            if (atual != null && naMesmaLinha(f, atual.maior)) {
                atual.partes += f
                if (f.altura > atual.maior.altura) atual.maior = f
            } else {
                linhas += Linha(f, mutableListOf(f))
            }
        }
        return linhas.map { linha ->
            Trecho(linha.partes.sortedBy { it.esquerda }.joinToString(" ") { it.texto.trim() }, linha.maior.altura)
        }
    }

    /** Dois pedaços estão na mesma linha quando o meio de um cai dentro da altura do outro. */
    private fun naMesmaLinha(a: Fragmento, b: Fragmento): Boolean =
        a.centro >= b.topo && a.centro <= b.base || b.centro >= a.topo && b.centro <= a.base

    /** Uma caixa com várias linhas de texto vira uma caixa por linha. */
    private fun separarLinhas(f: Fragmento): List<Fragmento> {
        val partes = f.texto.split('\n')
        if (partes.size == 1) return listOf(f)
        val altura = f.altura / partes.size
        return partes.mapIndexed { i, texto -> f.copy(texto = texto, topo = f.topo + i * altura, altura = altura) }
    }
}
