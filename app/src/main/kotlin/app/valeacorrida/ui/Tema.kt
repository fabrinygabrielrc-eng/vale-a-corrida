package app.valeacorrida.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// As telas são asfalto e faixa branca. Verde, amarelo e vermelho ficam guardados para o
// veredito da corrida: são as únicas cores vivas, e por isso se leem de relance.
private val Asfalto = Color(0xFF15181C)
private val Pista = Color(0xFF21262C)
private val MeioFio = Color(0xFF39414A)
private val Faixa = Color(0xFFF3F1EA)
private val Concreto = Color(0xFFEDEBE4)
private val Cinza = Color(0xFF5E6770)
private val Neblina = Color(0xFFA7B0B8)

private val Noite = darkColorScheme(
    primary = Faixa,
    onPrimary = Asfalto,
    secondaryContainer = MeioFio,
    onSecondaryContainer = Faixa,
    background = Asfalto,
    onBackground = Faixa,
    surface = Asfalto,
    onSurface = Faixa,
    surfaceVariant = Pista,
    onSurfaceVariant = Neblina,
    surfaceContainer = Pista,
    surfaceContainerHigh = Pista,
    surfaceContainerHighest = MeioFio,
    outline = Cinza,
    outlineVariant = MeioFio,
)

private val Dia = lightColorScheme(
    primary = Asfalto,
    onPrimary = Faixa,
    secondaryContainer = Color(0xFFDAD7CD),
    onSecondaryContainer = Asfalto,
    background = Concreto,
    onBackground = Asfalto,
    surface = Concreto,
    onSurface = Asfalto,
    surfaceVariant = Color.White,
    onSurfaceVariant = Cinza,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFDAD7CD),
    outline = Cinza,
    outlineVariant = Color(0xFFCFCBC0),
)

@Composable
fun Tema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (escuro) Noite else Dia, content = conteudo)
}
