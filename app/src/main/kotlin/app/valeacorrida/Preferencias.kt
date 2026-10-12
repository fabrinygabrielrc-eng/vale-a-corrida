package app.valeacorrida

import android.content.Context
import android.content.SharedPreferences
import app.valeacorrida.nucleo.Criterios
import app.valeacorrida.nucleo.Origem

/** Onde o cartão aparece na tela. */
enum class Posicao(val rotulo: String) { TOPO("Em cima"), MEIO("No meio"), BASE("Embaixo") }

/** As escolhas da pessoa, guardadas no aparelho. A tela grava; o serviço de leitura lê. */
class Preferencias(contexto: Context) {
    private val dados: SharedPreferences =
        contexto.applicationContext.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    var criterios: Criterios
        get() {
            val padrao = Criterios()
            return Criterios(
                porKmMinimo = numero("porKmMinimo", padrao.porKmMinimo),
                porKmBom = numero("porKmBom", padrao.porKmBom),
                porHoraMinimo = numero("porHoraMinimo", padrao.porHoraMinimo),
                porHoraBom = numero("porHoraBom", padrao.porHoraBom),
                valorMinimo = numero("valorMinimo", padrao.valorMinimo),
                buscaMaximaKm = numero("buscaMaximaKm", padrao.buscaMaximaKm),
                notaMinima = numero("notaMinima", padrao.notaMinima),
                custoPorKm = numero("custoPorKm", padrao.custoPorKm),
                contarBusca = dados.getBoolean("contarBusca", padrao.contarBusca),
            )
        }
        set(valor) = dados.edit()
            .putFloat("porKmMinimo", valor.porKmMinimo.toFloat())
            .putFloat("porKmBom", valor.porKmBom.toFloat())
            .putFloat("porHoraMinimo", valor.porHoraMinimo.toFloat())
            .putFloat("porHoraBom", valor.porHoraBom.toFloat())
            .putFloat("valorMinimo", valor.valorMinimo.toFloat())
            .putFloat("buscaMaximaKm", valor.buscaMaximaKm.toFloat())
            .putFloat("notaMinima", valor.notaMinima.toFloat())
            .putFloat("custoPorKm", valor.custoPorKm.toFloat())
            .putBoolean("contarBusca", valor.contarBusca)
            .apply()

    /** Os aplicativos de corrida que a pessoa quer acompanhar. Todos, até ela desligar algum. */
    var acompanhados: Set<Origem>
        get() = Origem.DE_CORRIDA.filter { dados.getBoolean("acompanhar." + it.name, true) }.toSet()
        set(valor) = dados.edit().apply {
            for (origem in Origem.DE_CORRIDA) putBoolean("acompanhar." + origem.name, origem in valor)
        }.apply()

    /** Quando o aplicativo de corrida esconde o texto, lê a imagem da tela (Android 11 em diante). */
    var lerPorImagem: Boolean
        get() = dados.getBoolean("lerPorImagem", true)
        set(valor) = dados.edit().putBoolean("lerPorImagem", valor).apply()

    /** Guarda as últimas leituras no aparelho, para a pessoa poder mandar quando algo não é reconhecido. */
    var guardarLeituras: Boolean
        get() = dados.getBoolean("guardarLeituras", false)
        set(valor) = dados.edit().putBoolean("guardarLeituras", valor).apply()

    var posicao: Posicao
        get() = Posicao.entries.firstOrNull { it.name == dados.getString("posicao", null) } ?: Posicao.TOPO
        set(valor) = dados.edit().putString("posicao", valor.name).apply()

    fun escutar(ouvinte: SharedPreferences.OnSharedPreferenceChangeListener) =
        dados.registerOnSharedPreferenceChangeListener(ouvinte)

    fun pararDeEscutar(ouvinte: SharedPreferences.OnSharedPreferenceChangeListener) =
        dados.unregisterOnSharedPreferenceChangeListener(ouvinte)

    // Gravado como Float; arredonda para não devolver 1.5 como 1.4999999.
    private fun numero(chave: String, padrao: Double): Double =
        if (dados.contains(chave)) Math.round(dados.getFloat(chave, 0f) * 100.0) / 100.0 else padrao
}
