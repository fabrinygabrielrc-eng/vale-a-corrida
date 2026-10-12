package app.valeacorrida.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.valeacorrida.LivroDeGastos
import app.valeacorrida.Preferencias

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val estado = Estado(Preferencias(this), LivroDeGastos(this))
        setContent {
            Tema { Aplicativo(estado) }
        }
    }
}

enum class Aba(val rotulo: String, val icone: ImageVector) {
    INICIO("Início", Icons.Filled.Home),
    VALORES("Valores", Pictogramas.Ajustes),
    GASTOS("Gastos", Pictogramas.Bomba),
    SIMULADOR("Simulador", Icons.Filled.PlayArrow),
    MAIS("Mais", Icons.Filled.Menu),
}

@Composable
fun Aplicativo(estado: Estado, abaInicial: Aba = Aba.INICIO) {
    var aba by rememberSaveable { mutableStateOf(abaInicial) }
    val contexto = LocalContext.current

    // A pessoa liga a leitura nas configurações do Android e volta: confere de novo ao voltar.
    LifecycleResumeEffect(Unit) {
        estado.leituraLigada = Estado.leituraLigada(contexto)
        onPauseOrDispose { }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (item in Aba.entries) {
                    NavigationBarItem(
                        selected = aba == item,
                        onClick = { aba = item },
                        icon = { Icon(item.icone, contentDescription = null) },
                        label = { Text(item.rotulo, maxLines = 1) },
                    )
                }
            }
        },
    ) { margens ->
        Box(Modifier.padding(margens).fillMaxSize()) {
            when (aba) {
                Aba.INICIO -> TelaInicio(estado, aoAbrirSimulador = { aba = Aba.SIMULADOR }, aoAbrirGastos = { aba = Aba.GASTOS })
                Aba.VALORES -> TelaValores(estado)
                Aba.GASTOS -> TelaGastos(estado)
                Aba.SIMULADOR -> TelaSimulador(estado)
                Aba.MAIS -> TelaMais(estado)
            }
        }
    }
}
