package com.katui.offline.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.katui.offline.R
import com.katui.offline.data.SessionManager
import com.katui.offline.data.repository.FemaleHealthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FemaleHealthReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: FemaleHealthRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            reprogramarLembretes(context)
            return
        }

        val type = intent.getStringExtra("REMINDER_TYPE") ?: return
        
        when (type) {
            "PERIOD_PREDICTION" -> showPeriodReminder(context)
            "BIRTH_CONTROL" -> showBirthControlReminder(context, intent.getStringExtra("PILL_NAME") ?: "Anticoncepcional")
        }
    }

    private fun reprogramarLembretes(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.userId.first()
                if (userId != -1L) {
                    repository.triggerScheduling(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("KATUI_HEALTH", "Erro ao reprogramar lembretes de saúde feminina", e)
            }
        }
    }

    private fun showPeriodReminder(context: Context) {
        showNotification(
            context,
            1001,
            "Saúde Feminina",
            "Sua menstruação está prevista para começar amanhã. Que tal registrar como você está se sentindo?"
        )
    }

    private fun showBirthControlReminder(context: Context, pillName: String) {
        showNotification(
            context,
            1002,
            "Hora da Pílula",
            "Está na hora de tomar seu $pillName. Não esqueça de registrar no app!"
        )
    }

    private fun showNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "female_health_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Saúde Feminina",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_custom)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
