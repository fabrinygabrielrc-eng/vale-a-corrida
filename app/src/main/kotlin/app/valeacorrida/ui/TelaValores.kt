package app.valeacorrida.ui

import androidx.compose.runtime.Composable
import app.valeacorrida.nucleo.Avaliador

@Composable
fun TelaValores(estado: Estado) {
    val criterios = estado.criterios
    Pagina("Meus valores") {
        Apoio("O cartão compara cada corrida com estes números. Deixe em branco o que não quiser usar.")

        Bloco("Como fica") {
            Cartao(Avaliador.avaliar(OFERTA_DE_EXEMPLO, criterios))
            Apoio("Exemplo: corrida de R$ 15,00, com 1,8 km até o passageiro e 6,2 km de viagem, em 19 minutos ao todo.")
        }

        Bloco("Por quilômetro") {
            CampoDeValor("Ruim abaixo de", criterios.porKmMinimo, { estado.mudarCriterios(criterios.copy(porKmMinimo = it)) }, passo = 0.05, unidade = "R$/km")
            CampoDeValor("Boa a partir de", criterios.porKmBom, { estado.mudarCriterios(criterios.copy(porKmBom = it)) }, passo = 0.05, unidade = "R$/km")
        }

        Bloco("Por hora") {
            CampoDeValor("Ruim abaixo de", criterios.porHoraMinimo, { estado.mudarCriterios(criterios.copy(porHoraMinimo = it)) }, passo = 1.0, unidade = "R$/h")
            CampoDeValor("Boa a partir de", criterios.porHoraBom, { estado.mudarCriterios(criterios.copy(porHoraBom = it)) }, passo = 1.0, unidade = "R$/h")
        }

        Bloco("Limites") {
            CampoDeValor("Valor mínimo", criterios.valorMinimo, { estado.mudarCriterios(criterios.copy(valorMinimo = it)) }, passo = 1.0, unidade = "R$")
            CampoDeValor("Passageiro a no máximo", criterios.buscaMaximaKm, { estado.mudarCriterios(criterios.copy(buscaMaximaKm = it)) }, passo = 0.5, unidade = "km")
            CampoDeValor("Nota mínima", criterios.notaMinima, { estado.mudarCriterios(criterios.copy(notaMinima = it.coerceAtMost(5.0))) }, passo = 0.05)
            Apoio("Corrida que passa de um limite é ruim, por melhor que seja a taxa.")
        }

        Bloco("Seu custo") {
            CampoDeValor(
                "Custo por km rodado",
                criterios.custoPorKm,
                { estado.mudarCriterios(criterios.copy(custoPorKm = it)) },
                passo = 0.05,
                unidade = "R$/km",
                apoio = "Combustível e desgaste. Com ele, o cartão mostra quanto sobra.",
            )
            Chave(
                "Contar o caminho até o passageiro",
                criterios.contarBusca,
                { estado.mudarCriterios(criterios.copy(contarBusca = it)) },
                apoio = "Esse trecho também gasta combustível e tempo.",
            )
        }
    }
}
