package app.valeacorrida.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.valeacorrida.LivroDeGastos
import app.valeacorrida.Posicao
import app.valeacorrida.Preferencias
import app.valeacorrida.leitura.ServicoDeLeitura
import app.valeacorrida.nucleo.Categoria
import app.valeacorrida.nucleo.Criterios
import app.valeacorrida.nucleo.Origem
import java.time.LocalDate
import java.time.YearMonth

/** O que as telas mostram. Cada mudança é gravada na hora, e o serviço de leitura passa a usá-la. */
class Estado(
    private val preferencias: Preferencias,
    private val livro: LivroDeGastos,
    /** Que dia é hoje. Os testes trocam para as telas saírem sempre iguais. */
    val hoje: () -> LocalDate = LocalDate::now,
) {
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

    var gastos by mutableStateOf(livro.listar())
        private set

    /** O mês aberto na aba Gastos. */
    var mesDosGastos: YearMonth by mutableStateOf(YearMonth.from(hoje()))
        private set

    /** Na aba Gastos: o relatório do mês, em vez da lista para anotar. */
    var vendoRelatorio by mutableStateOf(false)
        private set

    private var relatorioVisto by mutableStateOf(preferencias.relatorioVisto)

    /** O mês passado, enquanto ele tiver gasto anotado e o relatório dele ainda não tiver sido aberto. */
    val relatorioPronto: YearMonth?
        get() {
            val fechado = YearMonth.from(hoje()).minusMonths(1)
            return fechado.takeIf { relatorioVisto < it.toString() && gastos.any { gasto -> YearMonth.from(gasto.dia) == it } }
        }

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

    /** Devolve falso se o aparelho não deixou gravar. */
    fun anotarGasto(dia: LocalDate, categoria: Categoria, centavos: Long, nota: String): Boolean {
        val guardado = livro.anotar(dia, categoria, centavos, nota) != null
        gastos = livro.listar()
        return guardado
    }

    fun apagarGasto(id: Long) {
        livro.apagar(id)
        gastos = livro.listar()
    }

    fun abrirMes(mes: YearMonth) {
        mesDosGastos = mes
        if (vendoRelatorio) marcarVisto(mes)
    }

    fun verRelatorio(ligado: Boolean) {
        vendoRelatorio = ligado
        if (ligado) marcarVisto(mesDosGastos)
    }

    fun abrirRelatorio(mes: YearMonth) {
        mesDosGastos = mes
        verRelatorio(true)
    }

    // Só mês fechado conta como visto: o relatório do mês corrente ainda vai mudar.
    private fun marcarVisto(mes: YearMonth) {
        if (mes < YearMonth.from(hoje()) && mes.toString() > relatorioVisto) {
            relatorioVisto = mes.toString()
            preferencias.relatorioVisto = relatorioVisto
        }
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
