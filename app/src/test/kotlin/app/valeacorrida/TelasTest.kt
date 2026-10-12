package app.valeacorrida

import android.content.ComponentName
import android.provider.Settings
import androidx.compose.runtime.Composable
import app.valeacorrida.leitura.ServicoDeLeitura
import app.valeacorrida.nucleo.Origem
import app.valeacorrida.ui.Aba
import app.valeacorrida.ui.Aplicativo
import app.valeacorrida.ui.Estado
import app.valeacorrida.ui.Tema
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Desenha cada tela e grava a imagem em `app/build/telas/`, sem celular nem emulador.
 * Rode com `./gradlew :app:recordRoborazziDebug` e abra as imagens para conferir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = CELULAR)
class TelasTest {

    /** A tela pergunta ao Android se a leitura está ligada; aqui o teste responde por ele. */
    private fun estado(ligada: Boolean = false): Estado {
        val contexto = RuntimeEnvironment.getApplication()
        val servico = ComponentName(contexto, ServicoDeLeitura::class.java).flattenToString()
        Settings.Secure.putString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            if (ligada) servico else "",
        )
        return Estado(Preferencias(contexto))
    }

    private fun fotografar(nome: String, escuro: Boolean = true, tela: @Composable () -> Unit) {
        captureRoboImage("build/telas/$nome.png") { Tema(escuro) { tela() } }
    }

    @Test fun inicio() = fotografar("inicio") { Aplicativo(estado(), Aba.INICIO) }

    @Test fun inicioDeDia() = fotografar("inicio-dia", escuro = false) { Aplicativo(estado(), Aba.INICIO) }

    @Test fun simulador() = fotografar("simulador") { Aplicativo(estado(ligada = true), Aba.SIMULADOR) }

    @Config(qualifiers = COMPRIDA)
    @Test fun inicioInteira() = fotografar("inteira-inicio") { Aplicativo(estado(ligada = true), Aba.INICIO) }

    @Config(qualifiers = COMPRIDA)
    @Test fun valoresInteira() = fotografar("inteira-valores") { Aplicativo(estado(), Aba.VALORES) }

    @Config(qualifiers = COMPRIDA)
    @Test fun valoresInteiraDeDia() = fotografar("inteira-valores-dia", escuro = false) { Aplicativo(estado(), Aba.VALORES) }

    @Config(qualifiers = COMPRIDA)
    @Test fun simuladorInteira() = fotografar("inteira-simulador") { Aplicativo(estado(), Aba.SIMULADOR) }

    @Config(qualifiers = COMPRIDA)
    @Test fun maisInteira() {
        val contexto = RuntimeEnvironment.getApplication()
        Diagnostico.apagar(contexto)
        Diagnostico.registrar(
            contexto,
            Diagnostico.Leitura(0, Origem.UBER.rotulo, "texto", false, listOf("R$ 12,48", "linha que o leitor não entendeu")),
        )
        fotografar("inteira-mais") { Aplicativo(estado(), Aba.MAIS) }
        Diagnostico.apagar(contexto)
    }
}

private const val CELULAR = "w411dp-h891dp-420dpi"
private const val COMPRIDA = "w411dp-h1900dp-420dpi"
