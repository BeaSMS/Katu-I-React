package com.katui.offline.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katui.offline.util.MiuiPermissionHelper

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var showExplanation by remember { mutableStateOf(false) }
    var showOverlayTutorial by remember { mutableStateOf(false) }
    var showFullScreenIntentTutorial by remember { mutableStateOf(false) }
    var showMiuiTutorial by remember { mutableStateOf(false) }
    // A MIUI não expõe API para confirmar se o usuário realmente ativou o
    // Autostart / Outras permissões, então só conseguimos mostrar o passo
    // uma vez por sessão de onboarding (evita loop infinito).
    var miuiStepAcknowledged by remember { mutableStateOf(false) }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.VIBRATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Avalia, em ordem, qual o próximo passo de permissão "avançada" ainda
    // pendente. Cada tela de tutorial leva o usuário para uma Settings
    // específica; ao voltar para o app, o botão principal reavalia tudo
    // de novo — por isso este chain é chamado tanto pelo launcher quanto
    // sempre que um passo é concluído.
    fun proceedToNextStep() {
        val overlayMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(context)

        when {
            overlayMissing -> showOverlayTutorial = true
            MiuiPermissionHelper.needsFullScreenIntentPermission(context) -> showFullScreenIntentTutorial = true
            MiuiPermissionHelper.isXiaomiDevice() && !miuiStepAcknowledged -> showMiuiTutorial = true
            else -> onContinue()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            // In Katui, all permissions are mandatory for the elderly care flow
            val allGranted = results.values.all { it }
            if (allGranted) {
                proceedToNextStep()
            } else {
                showExplanation = true
            }
        }
    )

    when {
        showOverlayTutorial -> {
            OverlayPermissionTutorial(
                onConfirm = {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                    showOverlayTutorial = false
                }
            )
        }
        showFullScreenIntentTutorial -> {
            FullScreenIntentPermissionTutorial(
                onConfirm = {
                    MiuiPermissionHelper.openFullScreenIntentSettings(context)
                    showFullScreenIntentTutorial = false
                }
            )
        }
        showMiuiTutorial -> {
            MiuiPermissionTutorial(
                onOpenAutoStart = { MiuiPermissionHelper.openAutoStartSettings(context) },
                onOpenOtherPermissions = { MiuiPermissionHelper.openOtherPermissionsSettings(context) },
                onOpenBattery = { MiuiPermissionHelper.requestIgnoreBatteryOptimizations(context) },
                onConfirm = {
                    miuiStepAcknowledged = true
                    showMiuiTutorial = false
                    proceedToNextStep()
                }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Bem-vindo ao Katu'I",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Para garantir sua segurança e o aviso correto dos seus remédios, precisamos que você autorize as permissões abaixo:",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                PermissionItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notificações e Alarmes",
                    description = "Obrigatório: Para que o celular toque na hora exata do seu remédio."
                )

                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Câmera",
                    description = "Obrigatório: Para ler suas receitas médicas automaticamente."
                )

                PermissionItem(
                    icon = Icons.Default.Folder,
                    title = "Arquivos e Fotos",
                    description = "Obrigatório: Para salvar seus exames e lembretes com segurança."
                )

                PermissionItem(
                    icon = Icons.Default.Layers,
                    title = "Sobrepor a outros Apps",
                    description = "Obrigatório: Para que o aviso do remédio apareça mesmo se você estiver usando outro aplicativo."
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (showExplanation) {
                    Text(
                        text = "Atenção: Você precisa aceitar todas as permissões para que os alarmes funcionem corretamente. Por favor, tente novamente.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = { launcher.launch(permissionsToRequest.toTypedArray()) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Autorizar e Continuar", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun OverlayPermissionTutorial(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Passo Importante!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Agora precisamos ativar o alarme em cima de outros apps. Siga estes passos na próxima tela:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TutorialStep(1, "Procure pelo aplicativo Katu'I na lista.")
                TutorialStep(2, "Toque no nome do aplicativo.")
                TutorialStep(3, "Ative a chave 'Permitir sobreposição a outros apps'.")
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Dica para Android 13+: Se a opção estiver desabilitada (cinza), vá nas Configurações do Celular > Apps > Katu'I > clique nos 3 pontinhos (topo) > Permitir configurações restritas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Entendi, vamos lá", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Passo oficial do Android 14+: pede autorização para disparar notificações
 * em tela cheia (full-screen intent). Sem isso, o alarme só aparece como
 * banner e precisa de toque manual para abrir a tela do remédio.
 */
@Composable
fun FullScreenIntentPermissionTutorial(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Liberar Tela Cheia do Alarme",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "O Android pede uma autorização extra para o alarme abrir a tela cheia automaticamente, em vez de só mostrar uma notificação.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TutorialStep(1, "Na tela que vai abrir, procure 'Katu'I'.")
                TutorialStep(2, "Ative a opção de permitir tela cheia.")
                TutorialStep(3, "Volte para o app.")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Entendi, vamos lá", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Passo extra apenas para Xiaomi/Redmi/Poco (MIUI/HyperOS). Estes atalhos
 * não são API pública do Android — são telas proprietárias da MIUI. Por
 * isso oferecemos 3 botões avulsos (o usuário pode abrir cada um e voltar)
 * e não temos como confirmar automaticamente se foram ativados.
 */
@Composable
fun MiuiPermissionTutorial(
    onOpenAutoStart: () -> Unit,
    onOpenOtherPermissions: () -> Unit,
    onOpenBattery: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ajustes extras para Xiaomi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Detectamos um celular Xiaomi/Redmi/Poco. A MIUI tem permissões próprias que podem impedir o alarme de abrir automaticamente. Toque em cada botão abaixo e ative a opção indicada:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        MiuiActionCard(
            icon = Icons.Default.RocketLaunch,
            title = "Inicialização automática",
            description = "Ative o Autostart para o Katu'I, assim ele continua rodando em segundo plano.",
            buttonText = "Abrir Autostart",
            onClick = onOpenAutoStart
        )

        Spacer(modifier = Modifier.height(16.dp))

        MiuiActionCard(
            icon = Icons.Default.Layers,
            title = "Outras permissões",
            description = "Ative 'Exibir janelas pop-up enquanto em segundo plano', para o alarme aparecer sem precisar tocar na notificação.",
            buttonText = "Abrir Outras Permissões",
            onClick = onOpenOtherPermissions
        )

        Spacer(modifier = Modifier.height(16.dp))

        MiuiActionCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Economia de bateria",
            description = "Defina como 'Sem restrições', para o sistema não impedir o alarme de tocar.",
            buttonText = "Abrir Bateria",
            onClick = onOpenBattery
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Já ativei, continuar", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun MiuiActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun TutorialStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
        }
    }
}
