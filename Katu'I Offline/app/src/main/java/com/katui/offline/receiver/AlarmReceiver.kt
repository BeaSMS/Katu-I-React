package com.katui.offline.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.katui.offline.data.SessionManager
import com.katui.offline.data.repository.AlarmeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmeRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        
        Log.d("KATUI_ALARM", "AlarmReceiver: Recebido alarme $alarmId com ação: $action")

        // Verifica se a ação é uma das nossas ações de trigger dinâmicas
        val isTriggerAction = action?.startsWith("com.katui.offline.ALARM_TRIGGER_") == true

        // Adquire um WakeLock temporário para garantir que o dispositivo não durma
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Katui:AlarmReceiver")
        wakeLock.acquire(10000) // 10 segundos são suficientes

        try {
            // Caso especial: Reboot do sistema
            if (action == Intent.ACTION_BOOT_COMPLETED) {
                reprogramarAlarmes(context)
                return
            }

            // Ignora gatilhos sem ID válido exceto ações específicas e ações de trigger dinâmicas
            val isGroupAction = action == "ACTION_MARK_GROUP_AS_TAKEN" || action == "ACTION_SNOOZE_GROUP"
            if (alarmId == -1L && action != "ACTION_MARK_AS_TAKEN" && action != "ACTION_SNOOZE" && !isTriggerAction && !isGroupAction) {
                Log.w("KATUI_ALARM", "AlarmReceiver: Alarme ignorado - ID inválido e sem ação específica")
                return
            }

            if (action == "ACTION_MARK_AS_TAKEN") {
                markAsTaken(context, alarmId)
                AlarmService.instance?.resolveCurrentGroupAndAdvance()
                return
            }

            if (action == "ACTION_MARK_GROUP_AS_TAKEN") {
                val ids = intent.getLongArrayExtra("ALARM_IDS") ?: longArrayOf()
                ids.forEach { markAsTaken(context, it) }
                AlarmService.instance?.resolveCurrentGroupAndAdvance()
                return
            }

            if (action == "ACTION_SNOOZE") {
                val title = intent.getStringExtra("ALARM_TITLE") ?: "Lembrete de Medicamento"
                val dosage = intent.getStringExtra("DOSAGE") ?: ""
                
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        com.katui.offline.util.AlarmScheduler.snooze(context, repository, alarmId, title, dosage)
                        withContext(Dispatchers.Main) {
                            AlarmService.instance?.resolveCurrentGroupAndAdvance()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }

            if (action == "ACTION_SNOOZE_GROUP") {
                val ids = intent.getLongArrayExtra("ALARM_IDS") ?: longArrayOf()
                val title = intent.getStringExtra("ALARM_TITLE") ?: "Lembrete de Medicamento"
                val dosage = intent.getStringExtra("DOSAGE") ?: ""
                
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ids.forEach { id ->
                            com.katui.offline.util.AlarmScheduler.snooze(context, repository, id, title, dosage)
                        }
                        withContext(Dispatchers.Main) {
                            AlarmService.instance?.resolveCurrentGroupAndAdvance()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }

            val title = intent.getStringExtra("ALARM_TITLE") ?: "Lembrete de Medicamento"
            val userName = intent.getStringExtra("USER_NAME") ?: ""
            val userSexo = intent.getStringExtra("USER_SEXO") ?: ""
            val dosage = intent.getStringExtra("DOSAGE") ?: ""
            val isCustomAlarm = intent.getBooleanExtra("IS_CUSTOM_ALARM", false)
            val userIdFromIntent = intent.getLongExtra("USER_ID", -1L)

            val prefix = when (userSexo) {
                "FEMININO" -> "da"
                "MASCULINO" -> "do"
                else -> "de"
            }
            val mainWord = if (isCustomAlarm) "Alarme" else "Hora"

            // Alarme avulso: mostra o título direto
            // Alarme de remédio: mostra "Hora da/do [Nome]" como cabeçalho
            val displayTitle = when {
                userName.isNotBlank() -> "$mainWord $prefix ${userName.primeiroNome()}"
                else -> title
            }
            val displayMessage = when {
                isCustomAlarm -> title
                userName.isNotBlank() -> if (dosage.isNotBlank()) "$title ($dosage)" else title
                else -> intent.getStringExtra("ALARM_MESSAGE") ?: "Está na hora do seu medicamento."
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Mantém o WakeLock durante o processamento assíncrono
                    val soundUri = sessionManager.alarmSound.first()
                    
                    showNotification(
                        context = context,
                        displayTitle = displayTitle,
                        message = displayMessage,
                        alarmId = alarmId,
                        soundUri = soundUri,
                        dosage = dosage,
                        userName = userName,
                        userSexo = userSexo,
                        isCustomAlarm = isCustomAlarm,
                        medicationName = title,
                        userId = userIdFromIntent
                    )
                } catch (e: Exception) {
                    Log.e("KATUI_ALARM", "Erro ao processar alarme", e)
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            Log.e("KATUI_ALARM", "Erro crítico no AlarmReceiver", e)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun markAsTaken(context: Context, alarmId: Long) {
        if (alarmId != -1L) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(com.katui.offline.util.AlarmScheduler.safeRequestCode(alarmId))
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.updateStatus(alarmId, true)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun reprogramarAlarmes(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendentes = repository.getAlarmesPendentes()
                pendentes.forEach { alarme ->
                    val userName = repository.getUsuarioNome(alarme.usuarioId)
                    val userSexo = repository.getUsuarioSexo(alarme.usuarioId)
                    val dosage = alarme.medicamentoId?.let { repository.getMedicamentoDosagem(it) } ?: ""
                    
                    com.katui.offline.util.AlarmScheduler.schedule(
                        context,
                        alarme.id,
                        alarme.titulo,
                        alarme.horario,
                        userName,
                        dosage,
                        userSexo,
                        isCustomAlarm = alarme.medicamentoId == null,
                        userId = alarme.usuarioId
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        displayTitle: String,
        message: String,
        alarmId: Long,
        soundUri: String,
        dosage: String,
        userName: String = "",
        userSexo: String = "",
        isCustomAlarm: Boolean = false,
        medicationName: String = "",
        userId: Long = -1L
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "katui_alarms"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alarmes Katui", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, com.katui.offline.ui.screens.AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", medicationName)
            putExtra("ALARM_MESSAGE", message)
            putExtra("DOSAGE", dosage)
            putExtra("USER_NAME", userName)
            putExtra("USER_SEXO", userSexo)
            putExtra("USER_ID", userId)
            putExtra("IS_CUSTOM_ALARM", isCustomAlarm)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            com.katui.offline.util.AlarmScheduler.safeRequestCode(alarmId),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", medicationName)
            putExtra("DOSAGE", dosage)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            com.katui.offline.util.AlarmScheduler.safeRequestCode(alarmId + 100000),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ACTION_MARK_AS_TAKEN"
            putExtra("ALARM_ID", alarmId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            com.katui.offline.util.AlarmScheduler.safeRequestCode(alarmId + 200000),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_TITLE", displayTitle)
            putExtra("ALARM_SOUND", soundUri)
            // FIX: a notificação do AlarmService ("O alarme está tocando") precisa
            // desses dados para montar seus próprios botões (Adiar/Tomei) e o
            // toque/deslizar (abrir tela / soneca), no mesmo padrão da notificação
            // principal. Sem isso, deslizar essa notificação não fazia nada e o
            // som continuava tocando escondido (ver AlarmService.kt).
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_MESSAGE", message)
            putExtra("DOSAGE", dosage)
            putExtra("USER_NAME", userName)
            putExtra("USER_SEXO", userSexo)
            putExtra("USER_ID", userId)
            putExtra("IS_CUSTOM_ALARM", isCustomAlarm)
            putExtra("MEDICATION_NAME", medicationName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Alarme avulso não tem ação "Tomei" — só "Dispensar" e "Adiar"
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.katui.offline.R.drawable.ic_notification_custom)
            .setContentTitle(displayTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(com.katui.offline.R.drawable.logo_katui_new, "Adiar 5min", snoozePendingIntent)

        if (!isCustomAlarm) {
            notificationBuilder.addAction(com.katui.offline.R.drawable.logo_katui_new, "Tomei", takePendingIntent)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(com.katui.offline.util.AlarmScheduler.safeRequestCode(alarmId), notificationBuilder.build())
        }
    }
}

/**
 * Extrai apenas o primeiro nome de um nome completo (ex: "Beatriz Machado" -> "Beatriz").
 */
private fun String.primeiroNome(): String =
    trim().split(" ").firstOrNull { it.isNotBlank() } ?: this
