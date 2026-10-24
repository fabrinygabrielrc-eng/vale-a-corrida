package app.valeacorrida

import android.content.ComponentName
import android.provider.Settings
import app.valeacorrida.leitura.ServicoDeLeitura
import app.valeacorrida.leitura.ServicoVivo
import app.valeacorrida.ui.Estado
import app.valeacorrida.ui.Leitura
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Em 14/10/2026 o "limpar tudo" de um Xiaomi fechou o aplicativo com a leitura rodando. O Android
 * marcou o serviço como travado e não o religou, e a aba Início seguiu dizendo "ligada" por nove
 * dias, porque só olhava a configuração. Aqui se separa o que a pessoa ligou do que está rodando.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LeituraTest {

    private val contexto = RuntimeEnvironment.getApplication()

    private fun estado(ligadaNasConfiguracoes: Boolean, rodando: Boolean): Estado {
        val servico = ComponentName(contexto, ServicoDeLeitura::class.java).flattenToString()
        Settings.Secure.putString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            if (ligadaNasConfiguracoes) servico else "",
        )
        ServicoVivo.desde = if (rodando) 1L else null
        return Estado(Preferencias(contexto), LivroDeGastos(contexto)).apply {
            leituraLigada = Estado.leituraLigada(contexto)
        }
    }

    @After
    fun limpar() {
        ServicoVivo.desde = null
    }

    @Test
    fun `ligada e rodando`() = assertEquals(Leitura.RODANDO, estado(ligadaNasConfiguracoes = true, rodando = true).leitura)

    @Test
    fun `ligada nas configuracoes mas o celular parou o servico`() =
        assertEquals(Leitura.PARADA, estado(ligadaNasConfiguracoes = true, rodando = false).leitura)

    @Test
    fun desligada() = assertEquals(Leitura.DESLIGADA, estado(ligadaNasConfiguracoes = false, rodando = false).leitura)

    @Test
    fun `a tela percebe quando o servico volta, sem esperar a pessoa sair e voltar`() {
        val estado = estado(ligadaNasConfiguracoes = true, rodando = false)
        assertEquals(Leitura.PARADA, estado.leitura)
        ServicoVivo.desde = 1L
        assertEquals(Leitura.RODANDO, estado.leitura)
    }
}
