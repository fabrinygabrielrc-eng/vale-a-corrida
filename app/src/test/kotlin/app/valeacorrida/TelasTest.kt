package app.valeacorrida

import android.content.ComponentName
import android.provider.Settings
import androidx.compose.runtime.Composable
import app.valeacorrida.leitura.ServicoDeLeitura
import app.valeacorrida.nucleo.Categoria
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
import java.time.LocalDate

/**
 * Desenha cada tela e grava a imagem em `app/build/telas/`, sem celular nem emulador.
 * Rode com `./gradlew :app:recordRoborazziDebug` e abra as imagens para conferir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = CELULAR)
class TelasTest {

    /** A tela pergunta ao Android se a leitura está ligada; aqui o teste responde por ele. */
    private fun estado(ligada: Boolean = false, comGastos: Boolean = false): Estado {
        val contexto = RuntimeEnvironment.getApplication()
        val servico = ComponentName(contexto, ServicoDeLeitura::class.java).flattenToString()
        Settings.Secure.putString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            if (ligada) servico else "",
        )
        val livro = LivroDeGastos(contexto)
        if (comGastos) anotarUmMes(livro)
        // O dia é fixo para as telas saírem iguais em qualquer data.
        return Estado(Preferencias(contexto), livro, hoje = { HOJE })
    }

    /** Um mês de quem roda de verdade, e um setembro para o relatório ter com o que comparar. */
    private fun anotarUmMes(livro: LivroDeGastos) {
        fun gasto(dia: String, categoria: Categoria, reais: Double, nota: String = "") =
            livro.anotar(LocalDate.parse(dia), categoria, Math.round(reais * 100), nota)
        gasto("2026-09-05", Categoria.CARRO, 600.0, "semana")
        gasto("2026-09-12", Categoria.COMBUSTIVEL, 700.0)
        gasto("2026-09-20", Categoria.ALIMENTACAO, 180.0)
        gasto("2026-10-01", Categoria.CARRO, 600.0, "semana")
        gasto("2026-10-03", Categoria.COMBUSTIVEL, 140.0)
        gasto("2026-10-05", Categoria.CELULAR, 49.9, "plano do mês")
        gasto("2026-10-08", Categoria.MANUTENCAO, 250.0, "troca de óleo")
        gasto("2026-10-10", Categoria.COMBUSTIVEL, 150.0)
        gasto("2026-10-10", Categoria.PEDAGIO, 8.0, "estacionamento do shopping")
        gasto("2026-10-11", Categoria.LAVAGEM, 35.0)
        gasto("2026-10-11", Categoria.ALIMENTACAO, 12.0, "lanche")
        gasto("2026-10-12", Categoria.COMBUSTIVEL, 120.0, "posto da avenida")
        gasto("2026-10-12", Categoria.ALIMENTACAO, 18.5, "almoço")
    }

    private fun fotografar(nome: String, escuro: Boolean = true, tela: @Composable () -> Unit) {
        captureRoboImage("build/telas/$nome.png") { Tema(escuro) { tela() } }
    }

    @Test fun inicio() = fotografar("inicio") { Aplicativo(estado(), Aba.INICIO) }

    @Test fun inicioDeDia() = fotografar("inicio-dia", escuro = false) { Aplicativo(estado(), Aba.INICIO) }

    @Test fun simulador() = fotografar("simulador") { Aplicativo(estado(ligada = true), Aba.SIMULADOR) }

    @Test fun gastos() = fotografar("gastos") { Aplicativo(estado(comGastos = true), Aba.GASTOS) }

    @Test fun relatorio() = fotografar("relatorio") {
        Aplicativo(estado(comGastos = true).apply { verRelatorio(true) }, Aba.GASTOS)
    }

    @Config(qualifiers = COMPRIDA)
    @Test fun inicioInteira() = fotografar("inteira-inicio") { Aplicativo(estado(ligada = true, comGastos = true), Aba.INICIO) }

    @Config(qualifiers = COMPRIDA)
    @Test fun valoresInteira() = fotografar("inteira-valores") { Aplicativo(estado(), Aba.VALORES) }

    @Config(qualifiers = COMPRIDA)
    @Test fun valoresInteiraDeDia() = fotografar("inteira-valores-dia", escuro = false) { Aplicativo(estado(), Aba.VALORES) }

    @Config(qualifiers = COMPRIDA)
    @Test fun simuladorInteira() = fotografar("inteira-simulador") { Aplicativo(estado(), Aba.SIMULADOR) }

    @Config(qualifiers = MAIS_COMPRIDA)
    @Test fun gastosInteira() = fotografar("inteira-gastos") { Aplicativo(estado(comGastos = true), Aba.GASTOS) }

    @Config(qualifiers = MAIS_COMPRIDA)
    @Test fun gastosInteiraDeDia() = fotografar("inteira-gastos-dia", escuro = false) { Aplicativo(estado(comGastos = true), Aba.GASTOS) }

    @Config(qualifiers = COMPRIDA)
    @Test fun gastosSemNada() = fotografar("inteira-gastos-vazio") { Aplicativo(estado(), Aba.GASTOS) }

    @Config(qualifiers = COMPRIDA)
    @Test fun relatorioInteiro() = fotografar("inteira-relatorio") {
        Aplicativo(estado(comGastos = true).apply { verRelatorio(true) }, Aba.GASTOS)
    }

    @Config(qualifiers = COMPRIDA)
    @Test fun relatorioInteiroDeDia() = fotografar("inteira-relatorio-dia", escuro = false) {
        Aplicativo(estado(comGastos = true).apply { verRelatorio(true) }, Aba.GASTOS)
    }

    @Config(qualifiers = COMPRIDA)
    @Test fun maisInteira() {
        val contexto = RuntimeEnvironment.getApplication()
        Diagnostico.apagar(contexto)
        Diagnostico.registrar(
            contexto,
            Diagnostico.Leitura(0, Origem.UBER.rotulo, "texto", false, listOf("R$ 12,48", "linha que o leitor não entendeu")),
        )
        Rastro.apagar(contexto)
        Rastro.aviso(contexto, Origem.UBER.rotulo, 0)
        Rastro.passo(contexto, Rastro.Passo(0, Origem.UBER.rotulo, "janelas 1 de 4, 0 pedaços", "o Android recusou a captura (código 4)", false))
        fotografar("inteira-mais") { Aplicativo(estado().apply { mudarGuardarLeituras(true) }, Aba.MAIS) }
        Diagnostico.apagar(contexto)
        Rastro.apagar(contexto)
        Preferencias(contexto).guardarLeituras = false
    }
}

private val HOJE: LocalDate = LocalDate.of(2026, 10, 12)
private const val CELULAR = "w411dp-h891dp-420dpi"
private const val COMPRIDA = "w411dp-h1900dp-420dpi"
private const val MAIS_COMPRIDA = "w411dp-h2500dp-420dpi"
