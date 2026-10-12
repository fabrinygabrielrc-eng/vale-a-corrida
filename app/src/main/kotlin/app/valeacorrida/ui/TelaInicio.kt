package app.valeacorrida.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.valeacorrida.nucleo.Avaliador
import app.valeacorrida.nucleo.Datas
import app.valeacorrida.nucleo.Formato
import app.valeacorrida.nucleo.Oferta
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Percurso
import app.valeacorrida.nucleo.Relatorios
import java.time.YearMonth

/** A corrida usada para mostrar o cartão nas telas: R$ 15,00, 8 km ao todo, 19 minutos. */
val OFERTA_DE_EXEMPLO = Oferta(Origem.SIMULADOR, 1500, Percurso(5.0, 1.8), Percurso(14.0, 6.2), nota = 4.9)

@Composable
fun TelaInicio(estado: Estado, aoAbrirSimulador: () -> Unit, aoAbrirGastos: () -> Unit) {
    val contexto = LocalContext.current
    Pagina("Vale a Corrida", comMarca = true) {
        Apoio("Quanto cada corrida paga por km e por hora, antes de você aceitar.")

        estado.relatorioPronto?.let { fechado ->
            PlacaDeAviso("O relatório de ${Datas.nomeDoMes(fechado)} está pronto", "Ver para onde foi o dinheiro") {
                estado.abrirRelatorio(fechado)
                aoAbrirGastos()
            }
        }

        Bloco(if (estado.leituraLigada) "A leitura está ligada" else "A leitura está desligada") {
            if (estado.leituraLigada) {
                Apoio("Abra a Uber ou a 99 e fique online. Quando chegar uma oferta, o cartão aparece por cima dela.")
                BotaoSecundario("Testar no simulador", aoAbrirSimulador)
            } else {
                Apoio("Para ler a oferta, o Android pede que você ligue o Vale a Corrida em Acessibilidade. É a mesma permissão que os aplicativos pagos usam.")
                BotaoPrincipal("Ligar a leitura", { contexto.abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                Apoio("Se o Android disser que a configuração é restrita: abra os detalhes do aplicativo, toque nos três pontinhos no canto e escolha \"Permitir configurações restritas\". Acontece com todo aplicativo instalado fora da loja.")
                BotaoDeTexto("Abrir os detalhes do aplicativo", { contexto.abrir(detalhesDoAplicativo(contexto)) })
            }
        }

        Bloco("Como aparece") {
            Cartao(Avaliador.avaliar(OFERTA_DE_EXEMPLO, estado.criterios))
            Apoio("Verde é boa, amarelo é média e vermelho é ruim, pelos valores que você escolher na aba Valores.")
        }

        Bloco("Gastos de hoje") {
            val hoje = estado.hoje()
            val doMes = remember(estado.gastos, hoje) { Relatorios.doMes(estado.gastos, YearMonth.from(hoje)) }
            val deHoje = doMes.dias.firstOrNull { it.dia == hoje }?.centavos ?: 0L
            Column {
                Text(Formato.centavos(deHoje), style = MaterialTheme.typography.headlineSmall)
                Apoio(
                    if (doMes.vazio) "Anote o combustível, o lanche e a manutenção. No fim do mês você vê para onde o dinheiro foi."
                    else "${Formato.centavos(doMes.total)} em ${Datas.nomeDoMes(doMes.mes)}, em ${Relatorios.lancamentos(doMes.gastos.size)}.",
                )
            }
            BotaoSecundario("Anotar gasto", {
                estado.abrirMes(YearMonth.from(hoje))
                estado.verRelatorio(false)
                aoAbrirGastos()
            })
        }

        Bloco("Aplicativos acompanhados") {
            for (origem in Origem.DE_CORRIDA) {
                Chave(origem.rotulo, origem in estado.acompanhados, { estado.acompanhar(origem, it) })
            }
            Apoio("O Vale a Corrida só lê a tela enquanto um destes está aberto.")
        }
    }
}

private fun detalhesDoAplicativo(contexto: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))

/** Abre outra tela do Android. Se o aparelho não tiver essa tela, não faz nada. */
fun Context.abrir(pedido: Intent) {
    try {
        startActivity(pedido)
    } catch (_: Exception) {
    }
}
