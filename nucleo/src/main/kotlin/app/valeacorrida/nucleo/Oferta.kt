package app.valeacorrida.nucleo

/** De qual aplicativo veio a oferta. `pacotes` são os nomes do aplicativo de motorista no Android. */
enum class Origem(val rotulo: String, val pacotes: List<String>) {
    UBER("Uber", listOf("com.ubercab.driver")),
    NOVENTA_E_NOVE("99", listOf("com.app99.driver")),
    INDRIVE("inDrive", listOf("sinet.startup.inDriver")),
    SIMULADOR("Simulador", emptyList()),
    ;

    companion object {
        /** Os aplicativos de corrida que o Vale a Corrida sabe acompanhar. */
        val DE_CORRIDA = listOf(UBER, NOVENTA_E_NOVE, INDRIVE)

        fun doPacote(pacote: String?): Origem? = DE_CORRIDA.firstOrNull { pacote in it.pacotes }
    }
}

/** Um trecho do caminho. `minutos` é nulo quando a tela só mostra a distância. */
data class Percurso(val minutos: Double?, val km: Double)

/** Uma oferta de corrida, como apareceu na tela do motorista. */
data class Oferta(
    val origem: Origem,
    /** Quanto a corrida paga, em centavos. */
    val centavos: Long,
    /** Do motorista até o passageiro. Nulo quando a tela não separa esse trecho. */
    val busca: Percurso?,
    /** Do embarque até o destino. */
    val viagem: Percurso,
    /** Nota do passageiro, quando aparece. */
    val nota: Double? = null,
) {
    val reais: Double get() = centavos / 100.0

    /** Identifica a oferta enquanto ela está na tela, para não avaliar a mesma duas vezes. */
    val assinatura: String
        get() = "${origem.name}|$centavos|${busca?.km}|${viagem.km}|${viagem.minutos}"
}
