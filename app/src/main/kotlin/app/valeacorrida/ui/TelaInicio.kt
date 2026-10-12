package app.valeacorrida.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import app.valeacorrida.nucleo.Avaliador
import app.valeacorrida.nucleo.Oferta
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Percurso

/** A corrida usada para mostrar o cartão nas telas: R$ 15,00, 8 km ao todo, 19 minutos. */
val OFERTA_DE_EXEMPLO = Oferta(Origem.SIMULADOR, 1500, Percurso(5.0, 1.8), Percurso(14.0, 6.2), nota = 4.9)

@Composable
fun TelaInicio(estado: Estado, aoAbrirSimulador: () -> Unit) {
    val contexto = LocalContext.current
    Pagina("Vale a Corrida") {
        Apoio("Mostra quanto cada corrida paga por km e por hora, antes de você aceitar.")

        Bloco {
            if (estado.leituraLigada) {
                Text("A leitura está ligada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Apoio("Abra a Uber ou a 99 e fique online. Quando chegar uma oferta, o cartão aparece por cima dela.")
                OutlinedButton(onClick = aoAbrirSimulador) { Text("Testar no simulador") }
            } else {
                Text("A leitura está desligada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Apoio("Para ler a oferta, o Android pede que você ligue o Vale a Corrida em Acessibilidade. É a mesma permissão que os aplicativos pagos usam.")
                Button(onClick = { contexto.abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Ligar a leitura") }
                Apoio("Se o Android disser que a configuração é restrita: abra os detalhes do aplicativo, toque nos três pontinhos no canto e escolha \"Permitir configurações restritas\". Acontece com todo aplicativo instalado fora da loja.")
                TextButton(onClick = { contexto.abrir(detalhesDoAplicativo(contexto)) }) { Text("Abrir os detalhes do aplicativo") }
            }
        }

        Bloco("Como aparece") {
            Cartao(Avaliador.avaliar(OFERTA_DE_EXEMPLO, estado.criterios))
            Apoio("Verde é boa, amarelo é média e vermelho é ruim, pelos valores que você escolher na aba Valores.")
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
