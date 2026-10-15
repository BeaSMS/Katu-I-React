package com.katui.offline.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.katui.offline.R
import com.katui.offline.data.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Um alarme individual pendente de ser tocado/anunciado. Usado pela fila
 * interna do AlarmService para decidir se agrupa (mesmo usuário) ou
 * enfileira (usuário diferente) quando vários disparam ao mesmo tempo.
 */
data class PendingAlarmEntry(
    val alarmId: Long,
    val userId: Long,
    val userName: String,
    val userSexo: String,
    val medicationName: String,
    val dosage: String,
    val isCustomAlarm: Boolean,
    val soundUri: String?
)

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Grupo de alarmes tocando AGORA (mesmo usuário, mesmo horário, nomes
    // mesclados numa única notificação/som). Fica vazio quando nada toca.
    // FIX: Usamos StateFlow para que a AlarmActivity consiga observar mudanças
    // em tempo real se um novo remédio for adicionado ao grupo enquanto a tela
    // já está aberta.
    private val _currentGroup = MutableStateFlow<List<PendingAlarmEntry>>(emptyList())
    val currentGroup: StateFlow<List<PendingAlarmEntry>> = _currentGroup.asStateFlow()

    // Alarmes de OUTROS usuários que dispararam enquanto currentGroup já
    // estava tocando. São anunciados em sequência, um por vez, depois que
    // o grupo atual for resolvido (Tomei/Adiar/Snooze/Dispensar).
    private val queue = mutableListOf<PendingAlarmEntry>()

    /**
     * FIX (bug real): com dois remédios no mesmo horário, cada disparo
     * chamava playAlarm() criando um NOVO MediaPlayer, sobrescrevendo a
     * referência ao anterior SEM pará-lo. O primeiro MediaPlayer ficava
     * tocando para sempre, "órfão" — marcar qualquer alarme como Tomado só
     * conseguia parar o mediaPlayer da referência atual (o mais recente).
     *
     * Comportamento agora, conforme decidido:
     * - Mesmo usuário, mesmo horário -> agrupa os nomes numa única
     *   notificação/som (NÃO inicia um segundo MediaPlayer).
     * - Usuários diferentes, mesmo horário -> o primeiro toca normalmente;
     *   o(s) seguinte(s) entram numa fila e só começam a tocar depois que
     *   o grupo atual for resolvido.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val entry = intent?.toPendingAlarmEntry() ?: return START_NOT_STICKY
        Log.d("KATUI_ALARM", "AlarmService: recebido alarme ${entry.alarmId} (user=${entry.userId})")

        when {
            _currentGroup.value.isEmpty() -> {
                _currentGroup.value = listOf(entry)
                playCurrentGroup()
            }
            _currentGroup.value.first().userId == entry.userId -> {
                // Mesmo usuário já tocando: mescla o nome, NÃO reinicia o som/MediaPlayer.
                _currentGroup.value = _currentGroup.value + entry
                updateNotificationForCurrentGroup()
            }
            else -> {
                // Usuário diferente: aguarda a vez na fila.
                Log.d("KATUI_ALARM", "AlarmService: alarme ${entry.alarmId} enfileirado (usuário diferente já tocando)")
                queue.add(entry)
            }
        }

        return START_NOT_STICKY
    }

    private fun Intent.toPendingAlarmEntry(): PendingAlarmEntry? {
        val alarmId = getLongExtra("ALARM_ID", -1L)
        return PendingAlarmEntry(
            alarmId = alarmId,
            userId = getLongExtra("USER_ID", -1L),
            userName = getStringExtra("USER_NAME") ?: "",
            userSexo = getStringExtra("USER_SEXO") ?: "",
            medicationName = getStringExtra("MEDICATION_NAME") ?: (getStringExtra("ALARM_TITLE") ?: "Lembrete de Medicamento"),
            dosage = getStringExtra("DOSAGE") ?: "",
            isCustomAlarm = getBooleanExtra("IS_CUSTOM_ALARM", false),
            soundUri = getStringExtra("ALARM_SOUND")
        )
    }

    /** Inicia o som/vibração/notificação para o grupo atual (chamado só na primeira entrada do grupo). */
    private fun playCurrentGroup() {
        val first = _currentGroup.value.firstOrNull() ?: return
        updateNotificationForCurrentGroup()

        if (first.soundUri != null) {
            playAlarm(first.soundUri)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val uri = sessionManager.alarmSound.first()
                playAlarm(uri)
            }
        }
        startVibration()
    }

    /**
     * Atualiza a notificação em foreground para refletir todos os alarmes do
     * grupo atual (1 ou mais, sempre do mesmo usuário). Chamado tanto ao
     * iniciar o grupo quanto ao adicionar um novo medicamento ao grupo já
     * tocando — neste segundo caso, NÃO toca o som de novo, só atualiza o texto.
     */
    private fun updateNotificationForCurrentGroup() {
        val group = _currentGroup.value
        if (group.isEmpty()) return
        val first = group.first()

        val prefix = when (first.userSexo) {
            "FEMININO" -> "da"
            "MASCULINO" -> "do"
            else -> "de"
        }
        val displayTitle = if (first.userName.isNotBlank()) {
            "Hora $prefix ${first.userName.primeiroNomeAS()}"
        } else {
            "Lembrete de Medicamento"
        }
        // Mescla os nomes de todos os medicamentos do grupo: "Remédio A, Remédio B"
        val message = group.joinToString(", ") { entry ->
            if (entry.dosage.isNotBlank()) "${entry.medicationName} (${entry.dosage})" else entry.medicationName
        }

        startForegroundNotification(displayTitle, message, group)
    }

    private fun startForegroundNotification(title: String, message: String, group: List<PendingAlarmEntry>) {
        val channelId = "katui_alarm_service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alarm Active", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val first = group.first()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_custom)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // impede dispensar sem disparar nenhuma ação por engano

        if (first.alarmId != -1L) {
            // O toque/snooze/tomei do botão da notificação sempre se referem ao
            // PRIMEIRO alarme do grupo. Resolver qualquer um deles libera o grupo
            // inteiro (ver resolveCurrentGroupAndAdvance), então isso é seguro mesmo
            // com vários remédios mesclados na mesma notificação.
            val fullScreenIntent = Intent(this, com.katui.offline.ui.screens.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra("ALARM_ID", first.alarmId)
                putExtra("ALARM_TITLE", first.medicationName)
                putExtra("ALARM_MESSAGE", message)
                putExtra("DOSAGE", first.dosage)
                putExtra("USER_NAME", first.userName)
                putExtra("USER_SEXO", first.userSexo)
                putExtra("USER_ID", first.userId)
                putExtra("IS_CUSTOM_ALARM", first.isCustomAlarm)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                com.katui.offline.util.AlarmScheduler.safeRequestCode(first.alarmId),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(contentPendingIntent)

            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = "ACTION_SNOOZE_GROUP"
                putExtra("ALARM_IDS", group.map { it.alarmId }.toLongArray())
                putExtra("ALARM_TITLE", first.medicationName)
                putExtra("DOSAGE", first.dosage)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                this,
                com.katui.offline.util.AlarmScheduler.safeRequestCode(first.alarmId + 100000),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Deslizar a notificação = adiar 5 minutos (todo o grupo), em vez de não fazer nada.
            notificationBuilder.setDeleteIntent(snoozePendingIntent)
            notificationBuilder.addAction(R.drawable.logo_katui_new, "Adiar 5min", snoozePendingIntent)

            val anyMedication = group.any { !it.isCustomAlarm }
            if (anyMedication) {
                val takeIntent = Intent(this, AlarmReceiver::class.java).apply {
                    action = "ACTION_MARK_GROUP_AS_TAKEN"
                    putExtra("ALARM_IDS", group.filter { !it.isCustomAlarm }.map { it.alarmId }.toLongArray())
                }
                val takePendingIntent = PendingIntent.getBroadcast(
                    this,
                    com.katui.offline.util.AlarmScheduler.safeRequestCode(first.alarmId + 200000),
                    takeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(R.drawable.logo_katui_new, "Tomei", takePendingIntent)
            }
        }

        startForeground(1001, notificationBuilder.build())
    }

    /**
     * Chamado pelo AlarmReceiver quando o grupo atual foi resolvido (Tomei,
     * Adiar, ou a AlarmActivity foi dispensada). Limpa o grupo atual e, se
     * houver alguém na fila esperando (de outro usuário), promove o próximo
     * a "grupo atual" e começa a tocar para ele.
     */
    fun resolveCurrentGroupAndAdvance() {
        _currentGroup.value = emptyList()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()

        if (queue.isNotEmpty()) {
            val next = queue.removeAt(0)
            _currentGroup.value = listOf(next)
            playCurrentGroup()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun playAlarm(soundUriStr: String?) {
        val soundUri = if (soundUriStr == null || soundUriStr == "default_alarm") {
            Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
        } else {
            Uri.parse(soundUriStr)
        }

        // Garante que nunca exista mais de um MediaPlayer tocando ao mesmo
        // tempo nesta instância do Service — essencial agora que o mesmo
        // Service pode avançar de grupo em grupo (resolveCurrentGroupAndAdvance).
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // FIX: Se o volume do alarme estiver mutado ou muito baixo, garantimos
        // um volume mínimo para que o usuário ouça o lembrete crítico.
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            if (currentVolume < (maxVolume / 3)) {
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_ALARM,
                    (maxVolume / 2), // Define em 50% como segurança
                    0
                )
            }
        } catch (e: Exception) {
            Log.w("KATUI_ALARM", "Não foi possível ajustar o volume do alarme automaticamente")
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to system default if custom or provided raw fails
            mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun startVibration() {
        vibrator?.cancel()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        _currentGroup.value = emptyList()
        queue.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /**
         * Referência à instância viva do Service, para o AlarmReceiver poder
         * chamar resolveCurrentGroupAndAdvance() sem precisar de bind/Intent.
         * É seguro porque o Android só mantém uma instância de Service por vez
         * (Service é singleton por definição), e ela só existe enquanto o
         * Service está em foreground (ou seja, enquanto algo está tocando).
         */
        @Volatile
        var instance: AlarmService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

/**
 * Extrai apenas o primeiro nome de um nome completo (ex: "Beatriz Machado" -> "Beatriz").
 */
private fun String.primeiroNomeAS(): String =
    trim().split(" ").firstOrNull { it.isNotBlank() } ?: this
