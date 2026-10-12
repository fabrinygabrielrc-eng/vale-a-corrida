package app.valeacorrida.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.valeacorrida.leitura.CartaoView
import app.valeacorrida.nucleo.Avaliacao
import app.valeacorrida.nucleo.Formato

/** Uma tela: título em cima, blocos embaixo, rolando quando não cabe. */
@Composable
fun Pagina(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        conteudo()
    }
}

@Composable
fun Bloco(titulo: String? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            conteudo()
        }
    }
}

/** Texto de explicação, mais apagado que o principal. */
@Composable
fun Apoio(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Uma opção de ligar e desligar. A linha inteira responde ao toque. */
@Composable
fun Chave(rotulo: String, ligado: Boolean, aoMudar: (Boolean) -> Unit, apoio: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = ligado, role = Role.Switch, onValueChange = aoMudar)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(rotulo, style = MaterialTheme.typography.bodyLarge)
            if (apoio != null) Apoio(apoio)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = ligado, onCheckedChange = null)
    }
}

/** O mesmo cartão que aparece por cima da oferta, dentro de uma tela. */
@Composable
fun Cartao(avaliacao: Avaliacao, modifier: Modifier = Modifier) {
    AndroidView(factory = { CartaoView(it) }, update = { it.mostrar(avaliacao) }, modifier = modifier)
}

/**
 * Um número que a pessoa digita ou ajusta pelos botões. Vazio vale zero, que em todos os
 * critérios quer dizer "não usar".
 */
@Composable
fun CampoDeValor(
    rotulo: String,
    valor: Double,
    aoMudar: (Double) -> Unit,
    passo: Double,
    unidade: String? = null,
    apoio: String? = null,
) {
    var texto by remember { mutableStateOf(escrever(valor)) }
    // Só reescreve o que a pessoa digitou quando o valor mudou por fora (pelos botões).
    LaunchedEffect(valor) {
        if ((Formato.ler(texto) ?: 0.0) != valor) texto = escrever(valor)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = { digitado ->
                texto = digitado.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
                aoMudar((Formato.ler(texto) ?: 0.0).coerceIn(0.0, 9999.0))
            },
            label = { Text(rotulo) },
            placeholder = { Text("não usar") },
            suffix = unidade?.let { { Text(it) } },
            supportingText = apoio?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        Passo("−", "Diminuir: $rotulo") { aoMudar(arredondar((valor - passo).coerceAtLeast(0.0))) }
        Passo("+", "Aumentar: $rotulo") { aoMudar(arredondar(valor + passo)) }
    }
}

@Composable
private fun Passo(simbolo: String, descricao: String, aoTocar: () -> Unit) {
    FilledTonalIconButton(
        onClick = aoTocar,
        modifier = Modifier.size(48.dp).semantics { contentDescription = descricao },
    ) {
        Text(simbolo, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleLarge)
    }
}

private fun escrever(valor: Double): String = if (valor == 0.0) "" else Formato.numero(valor, 2)

private fun arredondar(valor: Double): Double = Math.round(valor * 100) / 100.0
