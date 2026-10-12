package app.valeacorrida.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.valeacorrida.R
import app.valeacorrida.leitura.CartaoView
import app.valeacorrida.nucleo.Avaliacao
import app.valeacorrida.nucleo.Categoria
import app.valeacorrida.nucleo.Formato

/** Uma tela: título em cima, blocos embaixo, rolando quando não cabe. */
@Composable
fun Pagina(titulo: String, comMarca: Boolean = false, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (comMarca) Marca()
            Text(titulo, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        }
        conteudo()
    }
}

/**
 * Uma parte da tela. Os blocos não são caixas: ficam direto no asfalto, separados pela faixa
 * tracejada, como as pistas de uma rua.
 */
@Composable
fun Bloco(titulo: String? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Faixa()
        if (titulo != null) {
            Text(titulo, Modifier.padding(top = 2.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        }
        conteudo()
    }
}

/** A faixa tracejada do meio da pista. */
@Composable
fun Faixa(modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            color = cor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 9.dp.toPx())),
        )
    }
}

/** Texto de explicação, mais apagado que o principal. */
@Composable
fun Apoio(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** O sinal do aplicativo: a estrada em forma de "certo". */
@Composable
fun Marca(lado: Dp = 40.dp) {
    Box(
        Modifier.size(lado).clip(RoundedCornerShape(lado * 0.24f)).background(Color(0xFF178A47)),
        contentAlignment = Alignment.Center,
    ) {
        // O desenho do ícone tem a folga que o Android exige em volta; aqui ela é cortada.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(lado * 1.5f))
    }
}

/** A ação principal da tela, com cara de placa: fundo azul e filete branco por dentro. */
@Composable
fun BotaoPrincipal(texto: String, aoTocar: () -> Unit, modifier: Modifier = Modifier, ligado: Boolean = true) {
    Box(
        modifier
            .heightIn(min = 52.dp)
            .alpha(if (ligado) 1f else 0.4f)
            .clip(CantoDePlaca)
            .background(AzulDePlaca)
            .clickable(enabled = ligado, role = Role.Button, onClick = aoTocar)
            .padding(4.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotaoSecundario(texto: String, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = aoTocar,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CantoDePlaca,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(texto, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BotaoDeTexto(texto: String, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = aoTocar, modifier = modifier, shape = CantoDePlaca) { Text(texto) }
}

/** Um aviso que leva a algum lugar, desenhado como placa de estrada. */
@Composable
fun PlacaDeAviso(titulo: String, acao: String, aoTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CantoDePlaca)
            .background(AzulDePlaca)
            .clickable(role = Role.Button, onClick = aoTocar)
            .padding(4.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(acao, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
    }
}

/** A placa de serviço de uma categoria de gasto: azul, com o desenho escuro no quadro branco. */
@Composable
fun PlacaDeServico(categoria: Categoria, lado: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(lado).background(AzulDePlaca, RoundedCornerShape(lado * 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(lado * 0.74f).background(Faixa, RoundedCornerShape(lado * 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Pictogramas.de(categoria), contentDescription = null, tint = Asfalto, modifier = Modifier.size(lado * 0.56f))
        }
    }
}

/** Escolha entre duas ou três vistas da mesma tela. */
@Composable
fun Alternador(opcoes: List<String>, escolhida: Int, aoEscolher: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, CantoDePlaca)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opcoes.forEachIndexed { indice, texto ->
            val ativa = indice == escolhida
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (ativa) AzulDePlaca else Color.Transparent)
                    .selectable(selected = ativa, role = Role.Tab, onClick = { aoEscolher(indice) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    texto,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (ativa) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Uma linha de "nome ... valor", como numa nota de posto. */
@Composable
fun LinhaDeNumero(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(rotulo, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
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
            Text(rotulo, style = MaterialTheme.typography.titleMedium)
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
            textStyle = MaterialTheme.typography.titleMedium,
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
        shape = CantoDePlaca,
        modifier = Modifier.size(48.dp).semantics { contentDescription = descricao },
    ) {
        Text(simbolo, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleLarge)
    }
}

private fun escrever(valor: Double): String = if (valor == 0.0) "" else Formato.numero(valor, 2)

private fun arredondar(valor: Double): Double = Math.round(valor * 100) / 100.0
