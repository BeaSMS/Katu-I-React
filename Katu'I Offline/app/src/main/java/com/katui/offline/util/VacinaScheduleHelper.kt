package com.katui.offline.util

import com.katui.offline.data.local.entities.Vacina
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calcula e formata o texto de "Próxima dose" para vacinas recorrentes.
 *
 * Regras (calendário PNI / Ministério da Saúde Brasil):
 *
 * 1. Vacinas com [mesCampanha] definido (ex: Influenza = abril):
 *    → Exibe: "Próxima dose: Abril/YYYY"
 *
 * 2. Vacinas com [mesesParaReforco] mas sem [mesCampanha] (ex: dT = 10 anos, COVID = 12 meses):
 *    → Exibe: "Próxima dose: dd/MM/yyyy" (data exata baseada na dataTomada)
 *    → Se não há dataTomada: "Reforço a cada X anos / meses"
 *    → Se já passou: "Dose em atraso desde dd/MM/yyyy"
 *
 * 3. Vacina recorrente sem nenhum dado de intervalo → retorna null.
 */
object VacinaScheduleHelper {

    /**
     * Retorna o label de próxima dose, ou null se não aplicável.
     * Só deve ser chamado quando [vacina.tomada] == true.
     */
    fun proximaDoseLabel(vacina: Vacina, hoje: LocalDate = LocalDate.now()): String? {
        if (!vacina.recorrente) return null

        // ── Caso 1: vacina com mês fixo de campanha (ex: Influenza em abril) ──────
        vacina.mesCampanha?.let { mes ->
            val proxima = proximoCicloMesPublico(mes, hoje)
            val nomeMes = proxima.month
                .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                .replaceFirstChar { it.uppercase() }
            return "Próxima dose: $nomeMes/${proxima.year}"
        }

        // ── Caso 2: intervalo em meses a partir da dataTomada ────────────────────
        val meses = vacina.mesesParaReforco
        if (meses != null) {
            val base = vacina.dataTomada ?: return labelIntervalo(meses)
            val proxima = base.plusMonths(meses.toLong())
            return when {
                proxima.isBefore(hoje) -> "Dose em atraso desde ${formatar(proxima)}"
                else -> "Próxima dose: ${formatar(proxima)}"
            }
        }

        return null
    }

    /**
     * Retorna o primeiro dia do próximo ciclo do [mes] a partir de [hoje].
     *
     * Considera que a janela de vacinação dura ~2 meses após o início da campanha.
     * Se ainda estivermos dentro dessa janela, mantém o ciclo deste ano;
     * caso contrário, aponta para o próximo ano.
     *
     * Exposto como `public` para uso no ViewModel (agendamento de alarme).
     */
    fun proximoCicloMesPublico(mes: Int, hoje: LocalDate): LocalDate {
        val inicioEsteAno = LocalDate.of(hoje.year, mes, 1)
        val fimJanelaEsteAno = inicioEsteAno.plusMonths(2) // Campanha dura ~2 meses
        return if (!hoje.isAfter(fimJanelaEsteAno)) inicioEsteAno
        else LocalDate.of(hoje.year + 1, mes, 1)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────────────

    private fun labelIntervalo(meses: Int): String {
        return when {
            meses % 12 == 0 -> {
                val anos = meses / 12
                "Reforço a cada ${if (anos == 1) "1 ano" else "$anos anos"}"
            }
            else -> "Reforço a cada $meses meses"
        }
    }

    private fun formatar(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
