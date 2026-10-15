package com.katui.offline.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.katui.offline.R
import com.katui.offline.data.SessionManager
import com.katui.offline.data.repository.AlarmeRepository
import com.katui.offline.data.repository.UsuarioRepository
import com.katui.offline.util.AlarmScheduler
import com.katui.offline.receiver.AlarmReceiver
import com.katui.offline.receiver.AlarmService
import com.katui.offline.receiver.PendingAlarmEntry
import com.katui.offline.ui.theme.KatuiTheme
import com.katui.offline.util.UiTextHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var repository: AlarmeRepository
    
    @Inject
    lateinit var usuarioRepository: UsuarioRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val title = intent.getStringExtra("ALARM_TITLE") ?: ""
        val message = intent.getStringExtra("ALARM_MESSAGE") ?: title
        val dosage = intent.getStringExtra("DOSAGE") ?: ""
        val userName = intent.getStringExtra("USER_NAME") ?: ""
        val userSexo = intent.getStringExtra("USER_SEXO") ?: ""
        val isCustomAlarm = intent.getBooleanExtra("IS_CUSTOM_ALARM", false)
        val isConsultation = intent.getBooleanExtra("IS_CONSULTATION", false)
        val userId = intent.getLongExtra("USER_ID", -1L)

        // Busca o grupo atual do AlarmService (se estiver rodando)
        val alarmGroup = AlarmService.instance?.currentGroup?.value ?: emptyList()

        onBackPressedDispatcher.addCallback(this) {
            CoroutineScope(Dispatchers.Main).launch {
                if (alarmGroup.isNotEmpty()) {
                    for (entry in alarmGroup) {
                        if (!isConsultation) {
                            AlarmScheduler.snooze(this@AlarmActivity, repository, entry.alarmId, entry.medicationName, entry.dosage)
                        }
                    }
                } else {
                    if (!isConsultation) {
                        AlarmScheduler.snooze(this@AlarmActivity, repository, alarmId, title, dosage)
                    }
                }
                stopAlarmService()
                finish()
            }
        }

        if (alarmId == -1L && alarmGroup.isEmpty()) {
            finish()
            return
        }

        setContent {
            val darkMode by sessionManager.darkMode.collectAsState(initial = null)
            val fontSize by sessionManager.fontSize.collectAsState(initial = "Normal")
            val dyslexiaFont by sessionManager.dyslexiaFont.collectAsState(initial = false)

            // Observa o grupo de alarmes do serviço em tempo real
            val alarmGroupState by (AlarmService.instance?.currentGroup ?: MutableStateFlow(emptyList())).collectAsState()
            
            // Fallback para o alarme do Intent se o serviço não estiver disponível 
            // ou se o grupo estiver vazio (não deveria acontecer se a tela está aberta)
            val alarmGroup = if (alarmGroupState.isNotEmpty()) alarmGroupState else {
                listOf(PendingAlarmEntry(alarmId, userId, userName, userSexo, title, dosage, isCustomAlarm, null))
            }
            
            val usuarioAtivo by produceState<com.katui.offline.data.local.entities.Usuario?>(initialValue = null, userId) {
                value = if (userId != -1L) usuarioRepository.getUsuario(userId) else null
            }
            
            val seedColor = remember(usuarioAtivo) {
                UiTextHelper.getUserColor(usuarioAtivo)
            }

            KatuiTheme(
                darkTheme = darkMode ?: isSystemInDarkTheme(),
                fontSize = fontSize,
                dyslexiaFont = dyslexiaFont,
                seedColor = seedColor
            ) {
                // Se houver um grupo, mescla as mensagens para exibição
                val displayTitle = if (alarmGroup.size > 1) {
                    alarmGroup.joinToString(", ") { it.medicationName }
                } else {
                    title
                }
                
                val displayDosage = if (alarmGroup.size > 1) {
                    alarmGroup.filter { it.dosage.isNotBlank() }
                        .joinToString("\n") { "${it.medicationName}: ${it.dosage}" }
                } else {
                    dosage
                }

                // Se houver qualquer medicamento no grupo, tratamos como alarme de medicamento
                val effectivelyCustom = if (alarmGroup.isNotEmpty()) alarmGroup.all { it.isCustomAlarm } else isCustomAlarm

                val scope = rememberCoroutineScope()

                AlarmContent(
                    title = displayTitle,
                    message = message,
                    dosage = displayDosage,
                    userName = userName,
                    userSexo = userSexo,
                    isCustomAlarm = effectivelyCustom,
                    isConsultation = isConsultation,
                    onSnooze = {
                        if (!isConsultation) {
                            scope.launch {
                                for (entry in alarmGroup) {
                                    AlarmScheduler.snooze(this@AlarmActivity, repository, entry.alarmId, entry.medicationName, entry.dosage)
                                }
                                withContext(Dispatchers.Main) {
                                    stopAlarmService()
                                    finish()
                                }
                            }
                        } else {
                            stopAlarmService()
                            finish()
                        }
                    },
                    onConfirm = {
                        if (effectivelyCustom || isConsultation) {
                            stopAlarmService()
                            finish()
                        } else {
                            scope.launch {
                                for (entry in alarmGroup) {
                                    markAsTaken(entry.alarmId)
                                }
                                withContext(Dispatchers.Main) {
                                    stopAlarmService()
                                    finish()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun stopAlarmService() {
        // FIX: stopService() matava o AlarmService inteiro, descartando qualquer
        // outro alarme que estivesse esperando na fila (de outro usuário, mesmo
        // horário). resolveCurrentGroupAndAdvance() limpa só o grupo atual e, se
        // houver alguém na fila, promove o próximo a tocar; só para o Service de
        // fato quando a fila estiver vazia.
        AlarmService.instance?.resolveCurrentGroupAndAdvance()
    }

    private suspend fun markAsTaken(alarmId: Long) {
        if (alarmId != -1L) {
            repository.updateStatus(alarmId, true)
        }
    }
}

@Composable
fun AlarmContent(
    title: String,
    message: String,
    dosage: String,
    userName: String,
    userSexo: String,
    isCustomAlarm: Boolean,
    isConsultation: Boolean = false,
    onSnooze: () -> Unit,
    onConfirm: () -> Unit
) {
    val prefix = when (userSexo) {
        "FEMININO" -> "da"
        "MASCULINO" -> "do"
        else -> "de"
    }

    val firstName = userName.trim().split(" ").firstOrNull { it.isNotBlank() } ?: userName
    val mainWord = when {
        isConsultation -> "Lembrete"
        isCustomAlarm -> "Alarme"
        else -> "Hora"
    }
    
    val personalizedHeadline = if (firstName.isNotBlank()) {
        "$mainWord $prefix $firstName"
    } else {
        when {
            isConsultation -> "Lembrete de Consulta"
            isCustomAlarm -> "Lembrete"
            else -> "Hora do Medicamento"
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Círculos decorativos de fundo
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .offset(x = (-80).dp, y = (-80).dp)
                    .background(
                        color = primaryColor.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
                    .background(
                        color = secondaryColor.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            )

            if (isConsultation) {
                // LAYOUT DE CONSULTA (Diferente)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                         Surface(
                            modifier = Modifier.size(100.dp * scale),
                            shape = RoundedCornerShape(24.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                             Box(contentAlignment = Alignment.Center) {
                                 Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(48.dp)
                                )
                             }
                         }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = personalizedHeadline,
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryColor,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Consulta em breve",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Deslize para cima para dispensar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        VerticalSwipeButton(onConfirm = onConfirm, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            } else {
                // LAYOUT DE MEDICAMENTO (Original)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Seção Superior: Ícone e Título Personalizado
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(110.dp * scale),
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.12f)
                            ) {}
                            
                            Icon(
                                imageVector = if (isCustomAlarm) Icons.Default.Alarm else Icons.Default.Medication,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = personalizedHeadline,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Seção Central: Card de Informação
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isCustomAlarm) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 34.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Seu Medicamento",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 34.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                if (dosage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = dosage,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Seção Inferior: Ações
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isCustomAlarm) "Deslize para dispensar" else "Deslize para confirmar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        SwipeButton(
                            onConfirm = onConfirm,
                            accentColor = primaryColor
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        TextButton(
                            onClick = onSnooze,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Adiar por 5 minutos",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeButton(onConfirm: () -> Unit, accentColor: Color = Color.Unspecified) {
    val width = 280.dp
    val height = 72.dp
    val swipeSize = 64.dp
    val density = LocalDensity.current
    val swipeRangePx = with(density) { (width - swipeSize - 8.dp).toPx() }
    
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(4.dp)
    ) {
        Text(
            text = ">>> >>> >>>",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(swipeSize)
                .clip(CircleShape)
                .background(if (accentColor == Color.Unspecified) MaterialTheme.colorScheme.primary else accentColor)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value >= swipeRangePx * 0.8f) {
                                scope.launch {
                                    offsetX.animateTo(swipeRangePx)
                                    onConfirm()
                                }
                            } else {
                                scope.launch {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newValue = (offsetX.value + dragAmount).coerceIn(0f, swipeRangePx)
                                offsetX.snapTo(newValue)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun VerticalSwipeButton(onConfirm: () -> Unit, accentColor: Color = Color.Unspecified) {
    val width = 72.dp
    val height = 220.dp
    val swipeSize = 64.dp
    val density = LocalDensity.current
    val swipeRangePx = with(density) { (height - swipeSize - 8.dp).toPx() }
    
    val offsetY = remember { Animatable(swipeRangePx) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(width / 2))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .size(swipeSize)
                .clip(CircleShape)
                .background(if (accentColor == Color.Unspecified) MaterialTheme.colorScheme.primary else accentColor)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY.value <= swipeRangePx * 0.2f) {
                                scope.launch {
                                    offsetY.animateTo(0f)
                                    onConfirm()
                                }
                            } else {
                                scope.launch {
                                    offsetY.animateTo(swipeRangePx)
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newValue = (offsetY.value + dragAmount).coerceIn(0f, swipeRangePx)
                                offsetY.snapTo(newValue)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
