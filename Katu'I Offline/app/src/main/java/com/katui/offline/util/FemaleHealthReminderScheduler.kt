package com.katui.offline.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.katui.offline.data.local.entities.Anticoncepcional
import com.katui.offline.data.local.entities.CicloMenstrual
import com.katui.offline.receiver.FemaleHealthReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object FemaleHealthReminderScheduler {

    private const val REQUEST_CODE_PERIOD = 2001
    private const val REQUEST_CODE_PILL = 2002

    fun scheduleReminders(
        context: Context,
        ciclos: List<CicloMenstrual>,
        anticoncepcional: Anticoncepcional?
    ) {
        schedulePeriodReminder(context, ciclos)
        schedulePillReminder(context, anticoncepcional)
    }

    private fun schedulePeriodReminder(context: Context, ciclos: List<CicloMenstrual>) {
        if (ciclos.isEmpty()) return

        val mediaCiclo = if (ciclos.size < 2) 28L else {
            val intervalos = (0 until ciclos.size - 1).map {
                ChronoUnit.DAYS.between(ciclos[it + 1].dataInicio, ciclos[it].dataInicio)
            }
            intervalos.average().toLong().coerceAtLeast(21L)
        }

        val proximaPrevista = ciclos.first().dataInicio.plusDays(mediaCiclo)
        val reminderDate = proximaPrevista.minusDays(1)
        val reminderTime = LocalDateTime.of(reminderDate, LocalTime.of(9, 0)) // 09:00 AM

        if (reminderTime.isAfter(LocalDateTime.now())) {
            val intent = Intent(context, FemaleHealthReminderReceiver::class.java).apply {
                putExtra("REMINDER_TYPE", "PERIOD_PREDICTION")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE_PERIOD, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Log.d("KATUI_HEALTH", "Lembrete de menstruação agendado para $reminderTime")
        }
    }

    private fun schedulePillReminder(context: Context, anticoncepcional: Anticoncepcional?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FemaleHealthReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", "BIRTH_CONTROL")
            putExtra("PILL_NAME", anticoncepcional?.nome)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_PILL, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (anticoncepcional == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val pillTime = try { LocalTime.parse(anticoncepcional.horarioTomada) } catch (e: Exception) { LocalTime.of(8, 0) }
        var triggerTime = LocalDateTime.of(LocalDate.now(), pillTime)

        if (triggerTime.isBefore(LocalDateTime.now())) {
            triggerTime = triggerTime.plusDays(1)
        }

        val triggerAt = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // Agendamento diário
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("KATUI_HEALTH", "Lembrete de pílula ($anticoncepcional.nome) agendado diariamente para $pillTime")
    }
}
