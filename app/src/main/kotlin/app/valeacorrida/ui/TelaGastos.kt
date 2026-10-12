package app.valeacorrida.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.valeacorrida.nucleo.Categoria
import app.valeacorrida.nucleo.Datas
import app.valeacorrida.nucleo.Formato
import app.valeacorrida.nucleo.Gasto
import app.valeacorrida.nucleo.RelatorioDoMes
import app.valeacorrida.nucleo.Relatorios
import java.time.LocalDate
import java.time.YearMonth

/**
 * Os gastos do trabalho: a pessoa anota o que saiu do bolso no dia e, no fim do mês, tem o
 * relatório de para onde o dinheiro foi.
 */
@Composable
fun TelaGastos(estado: Estado) {
    val hoje = estado.hoje()
    val mesDeHoje = YearMonth.from(hoje)
    val mes = estado.mesDosGastos
    val relatorio = remember(estado.gastos, mes) { Relatorios.doMes(estado.gastos, mes) }

    Pagina("Gastos") {
        estado.relatorioPronto?.takeIf { it != mes || !estado.vendoRelatorio }?.let { fechado ->
            PlacaDeAviso("O relatório de ${Datas.nomeDoMes(fechado)} está pronto", "Ver para onde foi o dinheiro") {
                estado.abrirRelatorio(fechado)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SeletorDeMes(mes, podeAvancar = mes < mesDeHoje, aoMudar = estado::abrirMes)
            Text(Formato.centavos(relatorio.total), style = MaterialTheme.typography.displayMedium)
            Apoio(resumo(relatorio, hoje))
        }

        Alternador(listOf("Anotar", "Relatório"), if (estado.vendoRelatorio) 1 else 0) { estado.verRelatorio(it == 1) }

        if (estado.vendoRelatorio) {
            Relatorio(relatorio)
        } else {
            Anotar(estado, mes, hoje)
            Anotados(estado, relatorio, hoje)
        }
    }
}

/** "R$ 62,00 hoje. R$ 184,50 a mais que em setembro." */
private fun resumo(relatorio: RelatorioDoMes, hoje: LocalDate): String {
    if (relatorio.vazio) return "Nada anotado em ${Datas.mes(relatorio.mes)}."
    val partes = mutableListOf(Relatorios.lancamentos(relatorio.gastos.size) + ".")
    relatorio.dias.firstOrNull { it.dia == hoje }?.let { partes += "${Formato.centavos(it.centavos)} hoje." }
    Relatorios.comparacao(relatorio)?.let { partes += it.comMaiuscula() + "." }
    return partes.joinToString(" ")
}

@Composable
private fun SeletorDeMes(mes: YearMonth, podeAvancar: Boolean, aoMudar: (YearMonth) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Seta(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") { aoMudar(mes.minusMonths(1)) }
        Text(
            Datas.mes(mes).comMaiuscula(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Seta(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mês seguinte", ligada = podeAvancar) { aoMudar(mes.plusMonths(1)) }
    }
}

@Composable
private fun Seta(
    icone: ImageVector,
    descricao: String,
    ligada: Boolean = true,
    aoTocar: () -> Unit,
) {
    FilledTonalIconButton(onClick = aoTocar, enabled = ligada, shape = CantoDePlaca) {
        Icon(icone, contentDescription = descricao)
    }
}

@Composable
private fun Anotar(estado: Estado, mes: YearMonth, hoje: LocalDate) {
    val foco = LocalFocusManager.current
    // Num mês passado, o gasto entra no último dia dele; no mês corrente, hoje.
    val ultimoDia = minOf(hoje, mes.atEndOfMonth())
    val primeiroDia = mes.atDay(1)

    var categoria by rememberSaveable { mutableStateOf(Categoria.COMBUSTIVEL) }
    var valor by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf("") }
    var diaEscolhido by rememberSaveable(mes) { mutableLongStateOf(ultimoDia.toEpochDay()) }
    var falhou by remember { mutableStateOf(false) }

    val dia = LocalDate.ofEpochDay(diaEscolhido)
    val centavos = Formato.lerCentavos(valor)

    fun anotar() {
        val quanto = centavos ?: return
        falhou = !estado.anotarGasto(dia, categoria, quanto, nota)
        if (!falhou) {
            valor = ""
            nota = ""
            foco.clearFocus()
        }
    }

    Bloco("Anotar gasto") {
        QuadroDeCategorias(categoria) { categoria = it }
        Apoio("${categoria.rotulo}: ${categoria.exemplos}.")

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = valor,
                onValueChange = { digitado -> valor = digitado.filter { it.isDigit() || it == ',' || it == '.' }.take(9) },
                label = { Text("Valor") },
                prefix = { Text("R$ ") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { anotar() }),
                modifier = Modifier.weight(1f),
            )
            Seta(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Dia anterior", ligada = dia > primeiroDia) { diaEscolhido -= 1 }
            Text(
                Datas.diaPorExtenso(dia, hoje).comMaiuscula(),
                Modifier.width(84.dp),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Seta(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Dia seguinte", ligada = dia < ultimoDia) { diaEscolhido += 1 }
        }

        OutlinedTextField(
            value = nota,
            onValueChange = { nota = it.take(80) },
            label = { Text("Observação (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { anotar() }),
            modifier = Modifier.fillMaxWidth(),
        )

        BotaoPrincipal("Anotar gasto", ::anotar, Modifier.fillMaxWidth(), ligado = centavos != null)
        if (falhou) {
            Text(
                "Não deu para guardar. Confira se o aparelho tem espaço livre e tente de novo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** As nove placas, três por linha, como o painel de serviços de uma estrada. */
@Composable
private fun QuadroDeCategorias(escolhida: Categoria, aoEscolher: (Categoria) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (linha in Categoria.entries.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (categoria in linha) {
                    val ativa = categoria == escolhida
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp)
                            .clip(CantoDePlaca)
                            .background(if (ativa) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (ativa) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                CantoDePlaca,
                            )
                            .selectable(selected = ativa, role = Role.RadioButton, onClick = { aoEscolher(categoria) })
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PlacaDeServico(categoria, 44.dp)
                        Text(
                            categoria.rotulo,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Anotados(estado: Estado, relatorio: RelatorioDoMes, hoje: LocalDate) {
    var aApagar by remember { mutableStateOf<Gasto?>(null) }

    Bloco("Anotado em ${Datas.nomeDoMes(relatorio.mes)}") {
        if (relatorio.vazio) {
            Apoio("Nada ainda. Escolha o tipo de gasto, ponha o valor e toque em Anotar gasto.")
        }
        for (dia in relatorio.dias) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Datas.diaPorExtenso(dia.dia, hoje).comMaiuscula(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Formato.centavos(dia.centavos),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (gasto in relatorio.gastos.filter { it.dia == dia.dia }) {
                LinhaDeGasto(gasto) { aApagar = gasto }
            }
        }
        if (!relatorio.vazio) Apoio("Toque num gasto para apagar.")
    }

    aApagar?.let { gasto ->
        AlertDialog(
            onDismissRequest = { aApagar = null },
            title = { Text("Apagar este gasto?") },
            text = { Text(Relatorios.maior(gasto) + ".") },
            confirmButton = {
                TextButton(onClick = {
                    estado.apagarGasto(gasto.id)
                    aApagar = null
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { aApagar = null }) { Text("Manter") } },
        )
    }
}

@Composable
private fun LinhaDeGasto(gasto: Gasto, aoTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CantoDePlaca)
            .clickable(onClickLabel = "Apagar", onClick = aoTocar)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacaDeServico(gasto.categoria, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(gasto.categoria.rotulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (gasto.nota.isNotBlank()) {
                Text(
                    gasto.nota,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(Formato.centavos(gasto.centavos), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Relatorio(relatorio: RelatorioDoMes) {
    val contexto = LocalContext.current

    if (relatorio.vazio) {
        Bloco { Apoio("O relatório aparece aqui quando houver gasto anotado em ${Datas.mes(relatorio.mes)}.") }
        return
    }

    Bloco("Para onde foi o dinheiro") {
        for (fatia in relatorio.fatias) {
            val parte = Relatorios.porcento(fatia.parte)
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription = "${fatia.categoria.rotulo}: ${Formato.centavos(fatia.centavos)}, $parte do mês"
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlacaDeServico(fatia.categoria, 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(fatia.categoria.rotulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Formato.centavos(fatia.centavos), style = MaterialTheme.typography.titleMedium)
                    }
                    Barra(fatia.parte)
                    Apoio("$parte do mês, em ${Relatorios.lancamentos(fatia.lancamentos)}")
                }
            }
        }
    }

    Bloco("Números do mês") {
        LinhaDeNumero("Dias com gasto", relatorio.dias.size.toString())
        LinhaDeNumero("Média nesses dias", Formato.centavos(relatorio.mediaPorDia))
        relatorio.diaMaisCaro?.let { LinhaDeNumero("Dia mais caro", "${Datas.dia(it.dia)}, ${Formato.centavos(it.centavos)}") }
        relatorio.maior?.let { LinhaDeNumero("Maior gasto", "${it.categoria.rotulo}, ${Formato.centavos(it.centavos)}") }
        relatorio.totalAnterior?.let { LinhaDeNumero("Em ${Datas.nomeDoMes(relatorio.mes.minusMonths(1))}", Formato.centavos(it)) }
    }

    Bloco {
        BotaoPrincipal("Compartilhar o relatório", {
            val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Relatorios.texto(relatorio))
            contexto.abrir(Intent.createChooser(envio, "Compartilhar o relatório"))
        }, Modifier.fillMaxWidth())
        Apoio("Sai como texto, para mandar por mensagem ou guardar. Os gastos ficam só neste aparelho.")
    }
}

/** Quanto do mês uma categoria levou. */
@Composable
private fun Barra(parte: Double) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(parte.toFloat().coerceIn(0.02f, 1f))
                .height(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
        )
    }
}

private fun String.comMaiuscula(): String = replaceFirstChar { it.uppercase() }
