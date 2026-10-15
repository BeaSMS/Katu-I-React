package com.katui.offline.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.katui.offline.data.local.entities.ConsultaLembrete
import com.katui.offline.data.local.entities.MetodoNotificacao
import com.katui.offline.data.local.entities.TipoLembreteConsulta
import com.katui.offline.receiver.ConsultationReceiver
import java.time.LocalDateTime
import java.time.ZoneId

object ConsultationScheduler {

    fun schedule(
        context: Context,
        consultaId: Long,
        medico: String,
        especialidade: String?,
        time: LocalDateTime,
        lembretes: List<ConsultaLembrete>,
        userName: String? = null,
        userSexo: String? = null,
        userId: Long? = null
    ) {
        Log.d("KATUI_CONSULTA", "Agendando ciclo de ${lembretes.size} lembretes para consulta $consultaId com $medico em $time para $userName")
        
        cancel(context, consultaId)

        lembretes.forEach { lembrete ->
            val triggerTime = when (lembrete.tipo) {
                TipoLembreteConsulta.RELATIVO -> {
                    time.minusMinutes((lembrete.minutosAntes ?: 0).toLong())
                }
                TipoLembreteConsulta.ABSOLUTO -> {
                    lembrete.horarioAbsoluto ?: time
                }
            }

            if (triggerTime.isAfter(LocalDateTime.now())) {
                scheduleReminder(
                    context, 
                    consultaId, 
                    lembrete.id,
                    medico, 
                    especialidade, 
                    triggerTime, 
                    lembrete.rotulo,
                    lembrete.metodo,
                    userName, 
                    userSexo, 
                    userId
                )
            } else {
                Log.d("KATUI_CONSULTA", "Lembrete ${lembrete.id} ignorado pois o horário ($triggerTime) já passou")
            }
        }
    }

    private fun scheduleReminder(
        context: Context,
        consultaId: Long,
        reminderId: Long,
        medico: String,
        especialidade: String?,
        triggerTime: LocalDateTime,
        rotulo: String?,
        metodo: MetodoNotificacao,
        userName: String? = null,
        userSexo: String? = null,
        userId: Long? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ConsultationReceiver::class.java).apply {
            putExtra("CONSULTA_ID", consultaId)
            putExtra("REMINDER_ID", reminderId)
            putExtra("MEDICO", medico)
            putExtra("ESPECIALIDADE", especialidade)
            putExtra("ROTULO", rotulo)
            putExtra("METODO", metodo.name)
            putExtra("USER_NAME", userName)
            putExtra("USER_SEXO", userSexo)
            putExtra("USER_ID", userId ?: -1L)
        }

        val requestCode = safeRequestCode(consultaId, reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        
        Log.d("KATUI_CONSULTA", "Lembrete ID $reminderId (Consulta $consultaId) como $metodo agendado para $triggerTime (RequestCode: $requestCode)")
    }

    fun cancel(context: Context, consultaId: Long) {
        Log.d("KATUI_CONSULTA", "Limpando agendamentos antigos para consulta $consultaId")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ConsultationReceiver::class.java)
        
        (0..2).forEach { oldType ->
            val oldRequestCode = 500000 + (oldType * 10000) + (consultaId % 10000).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                oldRequestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun cancelSpecific(context: Context, consultaId: Long, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ConsultationReceiver::class.java)
        val requestCode = safeRequestCode(consultaId, reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { 
            alarmManager.cancel(it)
            Log.d("KATUI_CONSULTA", "Cancelado lembrete ID $reminderId (RequestCode $requestCode)")
        }
    }

    private fun safeRequestCode(consultaId: Long, reminderId: Long): Int {
        val base = 500000
        return (base + (reminderId % 100000).toInt())
    }
}
