package com.katui.offline.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.katui.offline.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val geminiKey by viewModel.geminiKey.collectAsState()
    val openRouterKey by viewModel.openRouterKey.collectAsState()
    val mistralKey by viewModel.mistralKey.collectAsState()
    val ocrProvider by viewModel.ocrProvider.collectAsState()
    val openRouterModel by viewModel.openRouterModel.collectAsState()

    val alarmSoundState by viewModel.alarmSound.collectAsState()
    val darkModeState by viewModel.darkMode.collectAsState()
    val fontSizeState by viewModel.fontSize.collectAsState()
    val dyslexiaFontState by viewModel.dyslexiaFont.collectAsState()

    val isValidating by viewModel.isValidating.collectAsState()
    var validationMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var showDriveMessage by remember { mutableStateOf<String?>(null) }
    var showApiTutorial by remember { mutableStateOf<String?>(null) }

    val recoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { /* O usuário retornou da autorização */ }
    )

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    showDriveMessage = "Sincronizando com Google Drive..."
                    viewModel.backupToGoogleDrive(account) { resultBackup ->
                        resultBackup.fold(
                            onSuccess = { success ->
                                showDriveMessage = if (success) "Backup realizado com sucesso!" else "Falha ao realizar backup."
                            },
                            onFailure = { e ->
                                if (e is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                                    showDriveMessage = "Autorização necessária."
                                    recoveryLauncher.launch(e.intent)
                                } else {
                                    showDriveMessage = "Erro no backup: ${e.localizedMessage}"
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                showDriveMessage = "Erro no login: ${e.localizedMessage}"
            }
        }
    )

    val googleRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    showDriveMessage = "Buscando backup..."
                    viewModel.restoreFromGoogleDrive(account) { resultRestore ->
                        resultRestore.fold(
                            onSuccess = { success ->
                                showDriveMessage = if (success) "Dados restaurados! Reinicie o app." else "Nenhum backup encontrado."
                            },
                            onFailure = { e ->
                                if (e is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                                    showDriveMessage = "Autorização necessária."
                                    recoveryLauncher.launch(e.intent)
                                } else {
                                    showDriveMessage = "Erro ao restaurar: ${e.localizedMessage}"
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                showDriveMessage = "Erro no login: ${e.localizedMessage}"
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> uri?.let { viewModel.exportBackup(it) } }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> 
            uri?.let { 
                showDriveMessage = "Importando..."
                viewModel.importBackup(it) { result ->
                    result.fold(
                        onSuccess = { showDriveMessage = "Dados importados! Reinicie o app." },
                        onFailure = { e -> showDriveMessage = "Erro: ${e.localizedMessage}" }
                    )
                }
            } 
        }
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Título Integrado
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Sons
            Text(
                text = "Sons e Alertas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val soundPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri -> uri?.let { viewModel.setAlarmSound(it.toString()) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth().clickable { soundPickerLauncher.launch("audio/*") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Som do Alarme")
                    Text(text = if (alarmSoundState == "default_alarm") "Som Padrão" else "Som Personalizado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (alarmSoundState != "default_alarm") {
                    TextButton(onClick = { viewModel.setAlarmSound("default_alarm") }) { Text("Resetar") }
                } else {
                    Icon(Icons.Default.MusicNote, null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            // Acessibilidade
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Acessibilidade",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tema do Aplicativo")
                val currentTheme = when(darkModeState) { true -> "Escuro"; false -> "Claro"; null -> "Sistema" }
                var expandedTheme by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedTheme = true }) { Text(currentTheme) }
                    DropdownMenu(expanded = expandedTheme, onDismissRequest = { expandedTheme = false }) {
                        listOf("Claro", "Escuro", "Sistema").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                viewModel.setDarkMode(when(option) { "Claro" -> false; "Escuro" -> true; else -> null })
                                expandedTheme = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fonte Dislexia Friendly")
                Switch(checked = dyslexiaFontState, onCheckedChange = { viewModel.setDyslexiaFont(it) })
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text("Tamanho da Fonte: $fontSizeState")
                var expandedSize by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedSize = true }) { Text("Alterar Tamanho") }
                    DropdownMenu(expanded = expandedSize, onDismissRequest = { expandedSize = false }) {
                        listOf("Pequeno", "Normal", "Grande", "Extra Grande").forEach { size ->
                            DropdownMenuItem(text = { Text(size) }, onClick = { viewModel.setFontSize(size); expandedSize = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            // IA
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Configurações de IA (OCR)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Provedor de IA")
                var expandedProvider by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expandedProvider = true }) {
                        Text(when(ocrProvider) { "gemini" -> "Google Gemini"; "openrouter" -> "OpenRouter"; "mistral" -> "Mistral AI"; else -> "Google Gemini" })
                    }
                    DropdownMenu(expanded = expandedProvider, onDismissRequest = { expandedProvider = false }) {
                        DropdownMenuItem(text = { Text("Google Gemini") }, onClick = { viewModel.saveOcrProvider("gemini"); expandedProvider = false })
                        DropdownMenuItem(text = { Text("OpenRouter") }, onClick = { viewModel.saveOcrProvider("openrouter"); expandedProvider = false })
                        DropdownMenuItem(text = { Text("Mistral AI") }, onClick = { viewModel.saveOcrProvider("mistral"); expandedProvider = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (ocrProvider) {
                "gemini" -> {
                    val uriHandler = LocalUriHandler.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Obter chave", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { showApiTutorial = "gemini" }) { Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) }
                    }

                    var apiKeyInput by remember(geminiKey) { mutableStateOf(geminiKey) }
                    var passwordVisible by remember { mutableStateOf(false) }
                    var showSaveSuccess by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it; showSaveSuccess = false; validationMessage = null },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        supportingText = {
                            if (showSaveSuccess) Text("Salvo!", color = MaterialTheme.colorScheme.primary)
                            validationMessage?.let { Text(it.second, color = if (it.first) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) }
                        }
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.testKey("gemini", apiKeyInput) { s, m -> validationMessage = s to m } }, modifier = Modifier.weight(1f), enabled = !isValidating) {
                            if (isValidating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Testar")
                        }
                        Button(onClick = { viewModel.saveGeminiKey(apiKeyInput); showSaveSuccess = true }, modifier = Modifier.weight(1f)) { Text("Salvar") }
                    }
                }
                "openrouter" -> {
                    val uriHandler = LocalUriHandler.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { uriHandler.openUri("https://openrouter.ai/keys") }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Obter chave", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { showApiTutorial = "openrouter" }) { Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) }
                    }

                    var orKeyInput by remember(openRouterKey) { mutableStateOf(openRouterKey) }
                    var modelInput by remember(openRouterModel) { mutableStateOf(openRouterModel) }
                    var passwordVisible by remember { mutableStateOf(false) }
                    var showSaveSuccess by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = orKeyInput,
                        onValueChange = { orKeyInput = it; showSaveSuccess = false; validationMessage = null },
                        label = { Text("OpenRouter API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Modelos Grátis:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { modelInput = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free"; viewModel.saveOpenRouterModel(modelInput) }, label = { Text("Nemotron Omni") })
                        AssistChip(onClick = { modelInput = "meta-llama/llama-3.2-11b-vision-instruct:free"; viewModel.saveOpenRouterModel(modelInput) }, label = { Text("Llama 3.2") })
                    }

                    OutlinedTextField(value = modelInput, onValueChange = { modelInput = it; showSaveSuccess = false }, label = { Text("ID do Modelo") }, modifier = Modifier.fillMaxWidth(), supportingText = {
                        if (showSaveSuccess) Text("Salvo!", color = MaterialTheme.colorScheme.primary)
                        validationMessage?.let { Text(it.second, color = if (it.first) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) }
                    })

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.testKey("openrouter", orKeyInput) { s, m -> validationMessage = s to m } }, modifier = Modifier.weight(1f), enabled = !isValidating) {
                            if (isValidating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Testar")
                        }
                        Button(onClick = { viewModel.saveOpenRouterKey(orKeyInput); viewModel.saveOpenRouterModel(modelInput); showSaveSuccess = true }, modifier = Modifier.weight(1f)) { Text("Salvar") }
                    }
                }
                "mistral" -> {
                    val uriHandler = LocalUriHandler.current
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { uriHandler.openUri("https://console.mistral.ai/api-keys/") }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Obter chave", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { showApiTutorial = "mistral" }) { Icon(Icons.Default.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) }
                    }

                    var mistralKeyInput by remember(mistralKey) { mutableStateOf(mistralKey) }
                    var passwordVisible by remember { mutableStateOf(false) }
                    var showSaveSuccess by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = mistralKeyInput,
                        onValueChange = { mistralKeyInput = it; showSaveSuccess = false; validationMessage = null },
                        label = { Text("Mistral API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        supportingText = {
                            if (showSaveSuccess) Text("Salvo!", color = MaterialTheme.colorScheme.primary)
                            validationMessage?.let { Text(it.second, color = if (it.first) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) }
                        }
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.testKey("mistral", mistralKeyInput) { s, m -> validationMessage = s to m } }, modifier = Modifier.weight(1f), enabled = !isValidating) {
                            if (isValidating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Testar")
                        }
                        Button(onClick = { viewModel.saveMistralKey(mistralKeyInput); showSaveSuccess = true }, modifier = Modifier.weight(1f)) { Text("Salvar") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            // Backup
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nuvem (Google Drive)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sincronize seus dados com o Google Drive.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) }, modifier = Modifier.weight(1f)) { Text("Sincronizar") }
                        OutlinedButton(onClick = { googleRestoreLauncher.launch(viewModel.getGoogleSignInIntent()) }, modifier = Modifier.weight(1f)) { Text("Restaurar") }
                    }
                    showDriveMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Arquivos Locais", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Button(onClick = { exportLauncher.launch("katui_backup.zip") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar ZIP")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Importar ZIP")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Tutorials
        showApiTutorial?.let { type ->
            AlertDialog(
                onDismissRequest = { showApiTutorial = null },
                title = { Text("Como obter a chave") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when(type) {
                            "gemini" -> "1. Acesse o Google AI Studio.\n2. Crie uma API Key em um novo projeto.\n3. Copie a chave e cole aqui."
                            "openrouter" -> "1. Crie uma conta no OpenRouter.ai.\n2. Crie uma chave em 'Keys'.\n3. Use modelos ':free' para não ter custos."
                            "mistral" -> "1. Acesse o console da Mistral AI.\n2. Crie uma nova chave na aba 'API Keys'.\n3. A Mistral oferece um plano gratuito estável."
                            else -> ""
                        })
                    }
                },
                confirmButton = { TextButton(onClick = { showApiTutorial = null }) { Text("Entendi") } }
            )
        }
    }
}
