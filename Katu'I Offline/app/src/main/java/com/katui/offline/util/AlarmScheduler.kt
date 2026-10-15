package com.katui.offline.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    fun schedule(
        context: Context,
        alarmId: Long,
        title: String,
        time: LocalDateTime,
        userName: String = "",
        dosage: String = "",
        sexo: SexoBiologico = SexoBiologico.NAO_INFORMADO,
        isCustomAlarm: Boolean = false,
        userId: Long = -1L
    ) {
        Log.d("KATUI_ALARM", "Agendando alarme $alarmId para $time - $title (user=$userId, custom=$isCustomAlarm)")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.katui.offline.ALARM_TRIGGER_${alarmId}" // Ação ÚNICA por ID para evitar colisão de intents
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_ID", alarmId)
            putExtra("USER_NAME", userName)
            putExtra("USER_SEXO", sexo.name)
            putExtra("USER_ID", userId)
            putExtra("DOSAGE", dosage)
            putExtra("IS_CUSTOM_ALARM", isCustomAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            safeRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        if (triggerAtMillis > now) {
            try {
                // FIX: setAlarmClock é o método mais agressivo e confiável do Android.
                // Ele ignora Doze Mode, App Standby e restrições de economia de bateria,
                // além de mostrar o ícone de alarme e o horário na barra de status.
                // É o padrão ouro para apps de despertador e lembrete crítico.
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
                Log.d("KATUI_ALARM", "Alarme $alarmId agendado com SUCESSO via setAlarmClock")
            } catch (e: SecurityException) {
                Log.e("KATUI_ALARM", "Falha de permissão para setAlarmClock, tentando fallback", e)
                val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

                if (canScheduleExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        } else {
            Log.w("KATUI_ALARM", "Tentativa de agendar alarme no passado: $time (Agora: $now, Alvo: $triggerAtMillis)")
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        Log.d("KATUI_ALARM", "Cancelando alarme $alarmId")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // FIX: O Intent para cancelamento DEVE ser idêntico ao usado no schedule, inclusive a ACTION.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.katui.offline.ALARM_TRIGGER_${alarmId}"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            safeRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // Libera o recurso do sistema explicitamente
        }
    }

    /**
     * Converte um ID Long do banco em um Int seguro para request codes do sistema.
     * Usa modulo para evitar overflow e manter consistencia.
     */
    fun safeRequestCode(id: Long): Int {
        return (id % Int.MAX_VALUE).toInt()
    }

    suspend fun snooze(context: Context, repository: com.katui.offline.data.repository.AlarmeRepository, alarmId: Long, title: String, dosage: String) {
        val originalAlarm = repository.alarmeDao.getAlarmeById(alarmId)
        if (originalAlarm != null) {
            val snoozeTime = LocalDateTime.now().plusMinutes(5)
            val snoozedAlarm = originalAlarm.copy(
                id = 0,
                horario = snoozeTime,
                tomado = false
            )
            val newId = repository.alarmeDao.insert(snoozedAlarm)

            val userName = repository.getUsuarioNome(originalAlarm.usuarioId)
            val userSexo = repository.getUsuarioSexo(originalAlarm.usuarioId)

            schedule(
                context,
                newId,
                title,
                snoozeTime,
                userName = userName,
                dosage = dosage,
                sexo = userSexo,
                isCustomAlarm = originalAlarm.medicamentoId == null,
                userId = originalAlarm.usuarioId
            )
        }
    }
}
