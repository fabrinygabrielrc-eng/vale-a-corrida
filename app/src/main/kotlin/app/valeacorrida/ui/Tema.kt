package app.valeacorrida.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.valeacorrida.R

// A rua, de noite e de dia: asfalto, a faixa pintada no chão e as placas.
// Verde, amarelo e vermelho ficam guardados para o veredito da corrida: são as únicas cores
// vivas, e por isso se leem de relance. O azul é o das placas de serviço (posto, restaurante,
// oficina), que é do que a parte de gastos trata.
val Asfalto = Color(0xFF15181C)
private val Pista = Color(0xFF20252B)
private val MeioFio = Color(0xFF39414A)
val Faixa = Color(0xFFF4F4EF)
private val Neblina = Color(0xFFA9B2BA)
private val Cinza = Color(0xFF6B747D)
private val Concreto = Color(0xFFECEEF0)
private val Guia = Color(0xFFD8DCE0)

/** O azul das placas de serviço. Não muda com o tema: placa é placa, de dia e de noite. */
val AzulDePlaca = Color(0xFF1F5FAE)
private val AzulDeNoite = Color(0xFF8DBBF5)
private val AzulFundo = Color(0xFF0B2A52)

private val Noite = darkColorScheme(
    primary = AzulDeNoite,
    onPrimary = AzulFundo,
    primaryContainer = AzulDePlaca,
    onPrimaryContainer = Color.White,
    secondaryContainer = MeioFio,
    onSecondaryContainer = Faixa,
    background = Asfalto,
    onBackground = Faixa,
    surface = Asfalto,
    onSurface = Faixa,
    surfaceVariant = Pista,
    onSurfaceVariant = Neblina,
    surfaceContainerLowest = Asfalto,
    surfaceContainerLow = Pista,
    surfaceContainer = Pista,
    surfaceContainerHigh = Pista,
    surfaceContainerHighest = MeioFio,
    outline = Cinza,
    outlineVariant = MeioFio,
)

private val Dia = lightColorScheme(
    primary = AzulDePlaca,
    onPrimary = Color.White,
    primaryContainer = AzulDePlaca,
    onPrimaryContainer = Color.White,
    secondaryContainer = Guia,
    onSecondaryContainer = Asfalto,
    background = Concreto,
    onBackground = Asfalto,
    surface = Concreto,
    onSurface = Asfalto,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF4E5760),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Guia,
    outline = Cinza,
    outlineVariant = Color(0xFFC3C9CF),
)

// Overpass: a letra aberta desenhada a partir das placas de estrada. Feita para ser lida de
// relance e de longe, que é como o motorista olha para a tela.
private val Overpass = FontFamily(
    Font(R.font.overpass_regular, FontWeight.Normal),
    Font(R.font.overpass_semibold, FontWeight.SemiBold),
    Font(R.font.overpass_extrabold, FontWeight.ExtraBold),
)

private fun letra(tamanho: Int, linha: Int, peso: FontWeight, espaco: Double = 0.0) = TextStyle(
    fontFamily = Overpass,
    fontWeight = peso,
    fontSize = tamanho.sp,
    lineHeight = linha.sp,
    letterSpacing = espaco.sp,
    fontFeatureSettings = "tnum", // algarismos da mesma largura: valores alinham e não dançam
)

private val Letras = Typography(
    displayLarge = letra(52, 56, FontWeight.ExtraBold, -1.0),
    displayMedium = letra(46, 50, FontWeight.ExtraBold, -0.8),
    displaySmall = letra(40, 44, FontWeight.ExtraBold, -0.6),
    headlineLarge = letra(34, 40, FontWeight.ExtraBold, -0.4),
    headlineMedium = letra(30, 36, FontWeight.ExtraBold, -0.3),
    headlineSmall = letra(24, 30, FontWeight.ExtraBold, -0.2),
    titleLarge = letra(20, 26, FontWeight.ExtraBold),
    titleMedium = letra(17, 24, FontWeight.SemiBold),
    titleSmall = letra(15, 20, FontWeight.SemiBold),
    bodyLarge = letra(16, 24, FontWeight.Normal),
    bodyMedium = letra(14, 21, FontWeight.Normal),
    bodySmall = letra(12, 17, FontWeight.Normal),
    labelLarge = letra(15, 20, FontWeight.SemiBold),
    labelMedium = letra(12, 16, FontWeight.SemiBold),
    labelSmall = letra(11, 14, FontWeight.SemiBold),
)

/** Cantos de placa: arredondados, mas pouco. */
val CantoDePlaca = RoundedCornerShape(10.dp)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = CantoDePlaca,
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun Tema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) Noite else Dia,
        typography = Letras,
        shapes = Formas,
        content = conteudo,
    )
}
