package app.valeacorrida.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.valeacorrida.Posicao
import app.valeacorrida.Preferencias
import app.valeacorrida.leitura.ServicoDeLeitura
import app.valeacorrida.nucleo.Criterios
import app.valeacorrida.nucleo.Origem

/** O que as telas mostram. Cada mudança é gravada na hora, e o serviço de leitura passa a usá-la. */
class Estado(private val preferencias: Preferencias) {
    var criterios by mutableStateOf(preferencias.criterios)
        private set
    var acompanhados by mutableStateOf(preferencias.acompanhados)
        private set
    var lerPorImagem by mutableStateOf(preferencias.lerPorImagem)
        private set
    var guardarLeituras by mutableStateOf(preferencias.guardarLeituras)
        private set
    var posicao by mutableStateOf(preferencias.posicao)
        private set

    /** Se a leitura está ligada nas configurações de acessibilidade do Android. */
    var leituraLigada by mutableStateOf(false)

    fun mudarCriterios(novos: Criterios) {
        criterios = novos
        preferencias.criterios = novos
    }

    fun acompanhar(origem: Origem, ligado: Boolean) {
        acompanhados = if (ligado) acompanhados + origem else acompanhados - origem
        preferencias.acompanhados = acompanhados
    }

    fun mudarLerPorImagem(ligado: Boolean) {
        lerPorImagem = ligado
        preferencias.lerPorImagem = ligado
    }

    fun mudarGuardarLeituras(ligado: Boolean) {
        guardarLeituras = ligado
        preferencias.guardarLeituras = ligado
    }

    fun mudarPosicao(nova: Posicao) {
        posicao = nova
        preferencias.posicao = nova
    }

    companion object {
        fun leituraLigada(contexto: Context): Boolean {
            val ligados = Settings.Secure.getString(contexto.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val servico = ComponentName(contexto, ServicoDeLeitura::class.java)
            return ligados.split(':').any { ComponentName.unflattenFromString(it) == servico }
        }
    }
}
