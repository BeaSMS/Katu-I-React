package com.katui.offline.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.katui.offline.data.local.entities.MetodoNotificacao
import com.katui.offline.data.repository.ConsultaRepository
import com.katui.offline.data.repository.UsuarioRepository
import com.katui.offline.util.ConsultationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ConsultationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ConsultaRepository
    
    @Inject
    lateinit var usuarioRepository: UsuarioRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            reprogramarConsultas(context)
            return
        }

        val consultaId = intent.getLongExtra("CONSULTA_ID", -1L)
        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        val medico = intent.getStringExtra("MEDICO") ?: "Médico"
        val especialidade = intent.getStringExtra("ESPECIALIDADE")
        val rotulo = intent.getStringExtra("ROTULO")
        val metodoStr = intent.getStringExtra("METODO") ?: MetodoNotificacao.NOTIFICACAO.name
        val metodo = MetodoNotificacao.valueOf(metodoStr)
        val userName = intent.getStringExtra("USER_NAME")
        val userSexo = intent.getStringExtra("USER_SEXO")
        val userId = intent.getLongExtra("USER_ID", -1L)

        if (consultaId != -1L) {
            val isDependent = !userName.isNullOrBlank()
            val firstName = userName?.trim()?.split(" ")?.firstOrNull() ?: ""
            val prefix = when (userSexo) {
                "FEMININO" -> "da"
                "MASCULINO" -> "do"
                else -> "de"
            }
            
            val subject = if (especialidade.isNullOrBlank()) medico else "$especialidade com $medico"

            if (metodo == MetodoNotificacao.ALARME) {
                triggerAlarm(context, consultaId, subject, firstName, userSexo ?: "", userId)
            } else {
                val title = if (isDependent) "Consulta $prefix $firstName" else "Lembrete de Consulta"
                
                val message = if (!rotulo.isNullOrBlank()) {
                    "$rotulo: $subject"
                } else {
                    if (isDependent) "$firstName tem uma consulta em breve ($subject)."
                    else "Você tem uma consulta em breve ($subject)."
                }

                showNotification(context, title, message, consultaId, reminderId)
            }
        }
    }

    private fun triggerAlarm(
        context: Context,
        consultaId: Long,
        subject: String,
        userName: String,
        userSexo: String,
        userId: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "katui_alarms"

        val fullScreenIntent = Intent(context, com.katui.offline.ui.screens.AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", consultaId)
            putExtra("ALARM_TITLE", subject)
            putExtra("USER_NAME", userName)
            putExtra("USER_SEXO", userSexo)
            putExtra("USER_ID", userId)
            putExtra("IS_CONSULTATION", true)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (500000 + (consultaId % 10000).toInt()),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.katui.offline.R.drawable.ic_notification_custom)
            .setContentTitle("Alarme de Consulta")
            .setContentText("Sua consulta ($subject) será em breve.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_TITLE", "Consulta em breve")
        }
        context.startForegroundService(serviceIntent)

        notificationManager.notify(500000 + (consultaId % 10000).toInt(), notificationBuilder.build())
    }

    private fun reprogramarConsultas(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val agora = LocalDateTime.now()
            val pendentes = repository.getConsultasPendentes(agora)
            pendentes.forEach { consulta ->
                val usuario = usuarioRepository.getUsuario(consulta.usuarioId)
                val isPrincipal = usuario?.id == 1L
                val lembretes = repository.getLembretesForConsulta(consulta.id).firstOrNull() ?: emptyList()
                
                ConsultationScheduler.schedule(
                    context = context,
                    consultaId = consulta.id,
                    medico = consulta.medico,
                    especialidade = consulta.especialidade,
                    time = consulta.dataHora,
                    lembretes = lembretes,
                    userName = if (!isPrincipal) usuario?.nome else null,
                    userSexo = if (!isPrincipal) usuario?.sexo?.name else null,
                    userId = usuario?.id
                )
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        consultaId: Long,
        reminderId: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "katui_consultations"

        val channel = NotificationChannel(
            channelId,
            "Consultas e Agendamentos",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.katui.offline.R.drawable.ic_notification_custom)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = 1000 + (consultaId % 100).toInt() * 100 + (reminderId % 100).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
