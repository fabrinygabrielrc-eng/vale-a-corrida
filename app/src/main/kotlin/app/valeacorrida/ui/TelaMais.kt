package app.valeacorrida.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.valeacorrida.BuildConfig
import app.valeacorrida.Diagnostico
import app.valeacorrida.Posicao
import app.valeacorrida.Rastro
import app.valeacorrida.leitura.ServicoVivo

const val ENDERECO_DO_PROJETO = "https://github.com/fabrinygabrielrc-eng/vale-a-corrida"

@Composable
fun TelaMais(estado: Estado) {
    val contexto = LocalContext.current
    var leituras by remember { mutableStateOf(Diagnostico.listar(contexto)) }
    // Na tela vai o resumo do rastro: o serviço, os avisos e a última leitura. O resto sai no texto compartilhado.
    var rastro by remember { mutableStateOf(Rastro.linhas(contexto, ServicoVivo.desde, quantosPassos = 1)) }
    var temRastro by remember { mutableStateOf(!Rastro.vazio(contexto)) }
    LifecycleResumeEffect(Unit) {
        leituras = Diagnostico.listar(contexto)
        rastro = Rastro.linhas(contexto, ServicoVivo.desde, quantosPassos = 1)
        temRastro = !Rastro.vazio(contexto)
        onPauseOrDispose { }
    }

    Pagina("Mais") {
        Bloco("Onde o cartão aparece") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (posicao in Posicao.entries) {
                    FilterChip(
                        selected = estado.posicao == posicao,
                        onClick = { estado.mudarPosicao(posicao) },
                        label = { Text(posicao.rotulo) },
                    )
                }
            }
            Apoio("O cartão não recebe toque: ele nunca fica na frente do botão de aceitar.")
        }

        Bloco("Leitura") {
            Chave(
                "Ler pela imagem da tela",
                estado.lerPorImagem,
                estado::mudarLerPorImagem,
                apoio = "Quando o aplicativo de corrida esconde o texto, o Vale a Corrida lê uma captura da tela, no próprio aparelho. Funciona do Android 11 em diante e gasta mais bateria.",
            )
        }

        Bloco("Quando uma oferta não é reconhecida") {
            Chave(
                "Guardar as últimas leituras",
                estado.guardarLeituras,
                estado::mudarGuardarLeituras,
                apoio = "Ficam só neste aparelho. Com elas dá para avisar o projeto e consertar o leitor. Ligada, o Vale a Corrida também anota o caminho de cada leitura e confere a tela de 3 em 3 segundos, o que gasta mais bateria.",
            )
            Apoio(
                when (leituras.size) {
                    0 -> "Nenhuma leitura guardada."
                    1 -> "1 leitura guardada."
                    else -> "${leituras.size} leituras guardadas."
                },
            )
            if (estado.guardarLeituras || temRastro) {
                Apoio("O caminho da leitura, sem o que estava escrito na tela:")
                for (linha in rastro) Apoio(linha)
            }
            if (leituras.isNotEmpty() || temRastro) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoPrincipal("Compartilhar", {
                        val aparelho = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                        val texto = Diagnostico.texto(leituras, BuildConfig.VERSION_NAME, aparelho, Rastro.linhas(contexto, ServicoVivo.desde))
                        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
                        contexto.abrir(Intent.createChooser(envio, "Compartilhar as leituras"))
                    })
                    BotaoDeTexto("Apagar", {
                        Diagnostico.apagar(contexto)
                        Rastro.apagar(contexto)
                        leituras = emptyList()
                        rastro = Rastro.linhas(contexto, ServicoVivo.desde, quantosPassos = 1)
                        temRastro = false
                    })
                }
                Apoio("As leituras guardadas trazem o que estava na tela, inclusive endereços. Confira antes de enviar.")
            }
        }

        Bloco("Privacidade") {
            Apoio("O Vale a Corrida não tem permissão de internet. O que ele lê da tela serve para fazer a conta e é descartado: nada sai do aparelho.")
            Apoio("Ele só lê a tela enquanto a Uber, a 99 ou o inDrive estão abertos, e só os que você deixou ligados.")
            Apoio("Ele não aceita nem recusa corridas. Quem decide é você.")
            Apoio("Os gastos que você anota também ficam só neste aparelho. Se desinstalar o aplicativo, eles se perdem: compartilhe o relatório do mês para guardar.")
        }

        Bloco("Antes de usar") {
            Apoio("As plataformas não gostam de aplicativos que leem as ofertas, e há notícia de restrição a contas que usavam ferramentas parecidas. Use sabendo desse risco.")
            Apoio("A conta depende do que o aplicativo de corrida mostra. Se ele mudar a tela, o cartão pode parar de aparecer até o leitor ser atualizado.")
        }

        Bloco("Código aberto") {
            Apoio("Versão ${BuildConfig.VERSION_NAME}. O código é público: qualquer pessoa pode conferir o que o aplicativo faz e ajudar a melhorar.")
            BotaoSecundario("Abrir o projeto no GitHub", { contexto.abrir(Intent(Intent.ACTION_VIEW, Uri.parse(ENDERECO_DO_PROJETO))) })
            Apoio("A letra do aplicativo é a Overpass, desenhada a partir das placas de estrada e de licença livre.")
        }
    }
}
