package app.valeacorrida.nucleo

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Datas do jeito que se fala no Brasil. Os nomes ficam escritos aqui, e não vêm do aparelho,
 * para o relatório sair igual em qualquer celular, seja qual for o idioma dele.
 */
object Datas {
    private val MESES = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val SEMANA = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    /** "outubro" */
    fun nomeDoMes(mes: YearMonth): String = MESES[mes.monthValue - 1]

    /** "outubro de 2026" */
    fun mes(mes: YearMonth): String = "${nomeDoMes(mes)} de ${mes.year}"

    /** "08/10" */
    fun dia(dia: LocalDate): String = String.format(Locale.ROOT, "%02d/%02d", dia.dayOfMonth, dia.monthValue)

    /** "Hoje", "Ontem" ou "quinta, 08/10". */
    fun diaPorExtenso(dia: LocalDate, hoje: LocalDate): String = when (dia) {
        hoje -> "Hoje"
        hoje.minusDays(1) -> "Ontem"
        else -> "${SEMANA[dia.dayOfWeek.value - 1]}, ${dia(dia)}"
    }
}
