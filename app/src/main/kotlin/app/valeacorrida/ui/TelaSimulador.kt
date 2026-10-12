package app.valeacorrida.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.valeacorrida.Textos
import app.valeacorrida.leitura.Simulador
import app.valeacorrida.nucleo.Avaliador
import app.valeacorrida.nucleo.Formato
import app.valeacorrida.nucleo.Oferta
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.nucleo.Percurso
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** O jeito de escrever a oferta. Cada aplicativo escreve de um jeito, e o leitor tem que entender os dois. */
enum class Estilo(val rotulo: String) { UBER("Jeito da Uber"), NOVENTA_E_NOVE("Jeito da 99") }

/**
 * Uma oferta de mentira, para ver o cartão funcionando sem estar online num aplicativo de corrida.
 *
 * Os controles escrevem o número depois do nome ("Minutos de viagem: 14") de propósito:
 * se escrevessem "14 min", o leitor tomaria o controle por um trecho da corrida.
 */
@Composable
fun TelaSimulador(estado: Estado) {
    var estilo by rememberSaveable { mutableStateOf(Estilo.UBER) }
    var valor by rememberSaveable { mutableFloatStateOf(12.5f) }
    var buscaMinutos by rememberSaveable { mutableFloatStateOf(6f) }
    var buscaKm by rememberSaveable { mutableFloatStateOf(2.1f) }
    var viagemMinutos by rememberSaveable { mutableFloatStateOf(14f) }
    var viagemKm by rememberSaveable { mutableFloatStateOf(7.4f) }
    var semTexto by rememberSaveable { mutableStateOf(false) }

    // O serviço só lê o próprio Vale a Corrida enquanto esta tela está aberta.
    LifecycleResumeEffect(Unit) {
        Simulador.naTela = true
        onPauseOrDispose { Simulador.naTela = false }
    }

    val busca = Percurso(buscaMinutos.toDouble(), buscaKm.toDouble()).takeIf { buscaKm > 0f }
    val oferta = Oferta(
        Origem.SIMULADOR,
        (valor * 100.0).roundToLong(),
        busca,
        Percurso(viagemMinutos.toDouble(), viagemKm.toDouble()),
    )
    val esperado = Avaliador.avaliar(oferta, estado.criterios)

    Pagina("Simulador") {
        if (!estado.leituraLigada) {
            Bloco { Apoio("A leitura está desligada. O cartão só aparece por cima com ela ligada, na aba Início.") }
        }

        OfertaDeMentira(estilo, oferta, semTexto)
        Apoio("O cartão deve aparecer por cima desta tela com \"${Textos.titulo(esperado)}\", ${Textos.porKm(esperado)} e ${Textos.porHora(esperado)}.")

        Bloco("Mude a oferta") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (opcao in Estilo.entries) {
                    FilterChip(selected = estilo == opcao, onClick = { estilo = opcao }, label = { Text(opcao.rotulo) })
                }
            }
            Ajuste("Valor da corrida: ${Formato.numero(valor.toDouble(), 2)}", valor, 5f..120f) { valor = (it * 2).roundToInt() / 2f }
            Ajuste("Minutos até o passageiro: ${buscaMinutos.roundToInt()}", buscaMinutos, 1f..25f) { buscaMinutos = it.roundToInt().toFloat() }
            Ajuste("Quilômetros até o passageiro: ${Formato.numero(buscaKm.toDouble())}", buscaKm, 0f..12f) { buscaKm = (it * 10).roundToInt() / 10f }
            Ajuste("Minutos de viagem: ${viagemMinutos.roundToInt()}", viagemMinutos, 3f..90f) { viagemMinutos = it.roundToInt().toFloat() }
            Ajuste("Quilômetros de viagem: ${Formato.numero(viagemKm.toDouble())}", viagemKm, 1f..60f) { viagemKm = (it * 10).roundToInt() / 10f }
        }

        Bloco("Teste da leitura por imagem") {
            Chave(
                "Esconder o texto da oferta",
                semTexto,
                { semTexto = it },
                apoio = "Imita o aplicativo que não entrega o texto à acessibilidade. O cartão tem que aparecer mesmo assim, lendo a imagem da tela.",
            )
        }
    }
}

@Composable
private fun Ajuste(rotulo: String, valor: Float, faixa: ClosedFloatingPointRange<Float>, aoMudar: (Float) -> Unit) {
    Column {
        Text(rotulo, style = MaterialTheme.typography.bodyLarge)
        Slider(value = valor, onValueChange = aoMudar, valueRange = faixa)
    }
}

/** O cartão de oferta, parecido com o que os aplicativos de corrida mostram. */
@Composable
private fun OfertaDeMentira(estilo: Estilo, oferta: Oferta, semTexto: Boolean) {
    val uber = estilo == Estilo.UBER
    val linhaDaBusca = oferta.busca?.let { trecho(uber, it) + if (uber) " de distância" else "" }
    val linhaDaViagem = if (uber) "Viagem de " + trecho(true, oferta.viagem) else trecho(false, oferta.viagem)

    Surface(
        color = Color(0xFF0E1013),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (semTexto) Modifier.clearAndSetSemantics { } else Modifier),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (uber) "UberX" else "99Pop", style = MaterialTheme.typography.labelLarge)
            Text(Formato.reais(oferta.reais), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text("★ 4,95", style = MaterialTheme.typography.bodyMedium)
            if (linhaDaBusca != null) {
                Text(linhaDaBusca, style = MaterialTheme.typography.titleMedium)
                Text("Rua das Flores, 120, Centro", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA7B0B8))
            }
            Text(linhaDaViagem, style = MaterialTheme.typography.titleMedium)
            Text("Av. Brasil, 900, Catolé", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA7B0B8))
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (uber) "Selecionar" else "Aceitar", color = Color(0xFF0E1013), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** "6 min (2.1 km)" no jeito da Uber, "2,1 km · 6 min" no jeito da 99. */
private fun trecho(uber: Boolean, percurso: Percurso): String {
    val minutos = (percurso.minutos ?: 0.0).roundToInt()
    return if (uber) {
        "$minutos min (${String.format(Locale.US, "%.1f", percurso.km)} km)"
    } else {
        "${Formato.numero(percurso.km)} km · $minutos min"
    }
}
