package com.katui.offline.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.R
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.ProfileViewModel

@Composable
fun OnboardingProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val usuario by viewModel.usuario.collectAsState()
    var currentStep by remember { mutableStateOf(0) }
    
    // Local state for the form
    var nome by remember { mutableStateOf("") }
    var idade by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf(SexoBiologico.NAO_INFORMADO) }
    var peso by remember { mutableStateOf("") }
    var alturaCm by remember { mutableStateOf("") }
    var alergias by remember { mutableStateOf("") }
    var condicoesEspeciais by remember { mutableStateOf("") }

    // Initialize local state when user data is loaded
    LaunchedEffect(usuario) {
        usuario?.let {
            if (nome.isEmpty() && it.nome != "Cuidador Principal") nome = it.nome
            if (idade.isEmpty()) idade = it.idade?.toString() ?: ""
            sexo = it.sexo
            if (peso.isEmpty()) peso = it.peso?.toString() ?: ""
            if (alturaCm.isEmpty()) alturaCm = it.alturaCm?.toString() ?: ""
            if (alergias.isEmpty()) alergias = it.alergias ?: ""
            if (condicoesEspeciais.isEmpty()) condicoesEspeciais = it.condicoesEspeciais ?: ""
        }
    }

    val steps = listOf(
        "Boas-vindas",
        "Seu Nome",
        "Sua Idade",
        "Sexo Biológico",
        "Seu Peso",
        "Sua Altura",
        "Alergias",
        "Condições Especiais",
        "IA Opcional",
        "Tudo Pronto"
    )

    Scaffold(
        bottomBar = {
            if (currentStep > 0 && currentStep < steps.size - 1) {
                OnboardingBottomBar(
                    currentStep = currentStep,
                    totalSteps = steps.size,
                    onNext = { if (currentStep < steps.size - 1) currentStep++ },
                    onBack = { if (currentStep > 0) currentStep-- },
                    isNextEnabled = when (currentStep) {
                        1 -> nome.isNotBlank()
                        else -> true
                    }
                )
            }
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "StepTransition"
            ) { step ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (step) {
                        0 -> WelcomeStep(onStart = { currentStep++ })
                        1 -> NameStep(nome) { nome = it }
                        2 -> AgeStep(idade) { idade = it }
                        3 -> SexStep(sexo) { sexo = it }
                        4 -> WeightStep(peso) { peso = it }
                        5 -> HeightStep(alturaCm) { alturaCm = it }
                        6 -> AllergyStep(alergias) { alergias = it }
                        7 -> SpecialConditionsStep(condicoesEspeciais) { condicoesEspeciais = it }
                        8 -> AiOptionalStep()
                        9 -> SummaryStep(
                            nome = nome,
                            onConfirm = {
                                usuario?.let {
                                    viewModel.updateUsuario(it.copy(
                                        nome = nome,
                                        idade = idade.toIntOrNull(),
                                        sexo = sexo,
                                        peso = peso.toDoubleOrNull(),
                                        alturaCm = alturaCm.toIntOrNull(),
                                        alergias = alergias.ifBlank { "Nenhuma" },
                                        condicoesEspeciais = condicoesEspeciais.ifBlank { "Nenhuma" },
                                        tipo = TipoUsuario.CUIDADOR // The first profile is the Caregiver
                                    ))
                                }
                                onComplete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.logo_katui_new),
            contentDescription = "Katu'I Logo",
            modifier = Modifier
                .height(120.dp)
                .padding(bottom = 32.dp)
        )
        Text(
            text = "Olá! Vamos cuidar da sua saúde juntos?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Para te ajudar melhor, preciso te conhecer um pouco. É bem rápido, vamos lá?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Começar", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun NameStep(nome: String, onValueChange: (String) -> Unit) {
    StepContent(
        title = "Como podemos te chamar?",
        description = "Escreva seu nome completo abaixo."
    ) {
        OutlinedTextField(
            value = nome,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("Ex: Maria Oliveira") },
            singleLine = true
        )
    }
}

@Composable
fun AgeStep(idade: String, onValueChange: (String) -> Unit) {
    StepContent(
        title = "Qual a sua idade?",
        description = "Saber sua idade ajuda a recomendar os melhores cuidados."
    ) {
        OutlinedTextField(
            value = idade,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("00", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true
        )
        Text("anos", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun SexStep(sexo: SexoBiologico, onValueChange: (SexoBiologico) -> Unit) {
    StepContent(
        title = "Qual seu sexo biológico?",
        description = "Isso nos ajuda a habilitar funções específicas como saúde feminina."
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.height(64.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilterChip(
                    selected = sexo == SexoBiologico.FEMININO,
                    onClick = { onValueChange(SexoBiologico.FEMININO) },
                    label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Female, contentDescription = "Feminino", modifier = Modifier.size(32.dp), tint = AccentPink) } },
                    modifier = Modifier.width(80.dp).fillMaxHeight()
                )
                FilterChip(
                    selected = sexo == SexoBiologico.MASCULINO,
                    onClick = { onValueChange(SexoBiologico.MASCULINO) },
                    label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Male, contentDescription = "Masculino", modifier = Modifier.size(32.dp), tint = AccentBlue) } },
                    modifier = Modifier.width(80.dp).fillMaxHeight()
                )
                FilterChip(
                    selected = sexo == SexoBiologico.INTERSEXO,
                    onClick = { onValueChange(SexoBiologico.INTERSEXO) },
                    label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Transgender, contentDescription = "Intersexo", modifier = Modifier.size(32.dp), tint = AccentPurple) } },
                    modifier = Modifier.width(80.dp).fillMaxHeight()
                )
            }
            TextButton(onClick = { onValueChange(SexoBiologico.NAO_INFORMADO) }) {
                Text("Prefiro não informar")
            }
        }
    }
}

@Composable
fun WeightStep(peso: String, onValueChange: (String) -> Unit) {
    StepContent(
        title = "Quanto você pesa hoje?",
        description = "Se não souber exatamente, pode colocar um valor aproximado."
    ) {
        OutlinedTextField(
            value = peso,
            onValueChange = onValueChange,
            modifier = Modifier.width(150.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("0.0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true
        )
        Text("kg", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun HeightStep(altura: String, onValueChange: (String) -> Unit) {
    StepContent(
        title = "E qual a sua altura?",
        description = "Coloque sua altura em centímetros."
    ) {
        OutlinedTextField(
            value = altura,
            onValueChange = onValueChange,
            modifier = Modifier.width(150.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("165", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true
        )
        Text("cm", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun AllergyStep(alergias: String, onValueChange: (String) -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val saudeCatalogo by viewModel.saudeCatalogo.collectAsState()
    var selectedList by remember { mutableStateOf(alergias.split(", ").filter { it.isNotBlank() && it != "Nenhuma" }) }
    var showDialog by remember { mutableStateOf(false) }

    StepContent(
        title = "Você tem alguma alergia?",
        description = "Selecione da lista ou adicione se não encontrar."
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedList.forEach { alergia ->
                    InputChip(
                        selected = true,
                        onClick = { 
                            val newList = selectedList - alergia
                            selectedList = newList
                            onValueChange(newList.joinToString(", ").ifBlank { "Nenhuma" })
                        },
                        label = { Text(alergia) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Selecionar Alergias")
            }
        }
    }

    if (showDialog) {
        SaudeSelectionDialog(
            title = "Selecionar Alergias",
            items = saudeCatalogo.filter { it.tipo == com.katui.offline.data.local.entities.TipoSaudeItem.ALERGIA },
            selectedItems = selectedList,
            onDismiss = { showDialog = false },
            onConfirm = { 
                selectedList = it
                onValueChange(it.joinToString(", ").ifBlank { "Nenhuma" })
            }
        )
    }
}

@Composable
fun SpecialConditionsStep(condicoes: String, onValueChange: (String) -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val saudeCatalogo by viewModel.saudeCatalogo.collectAsState()
    var selectedList by remember { mutableStateOf(condicoes.split(", ").filter { it.isNotBlank() && it != "Nenhum" }) }
    var showDialog by remember { mutableStateOf(false) }

    StepContent(
        title = "Acompanhamento de Saúde",
        description = "Você possui alguma condição crônica que exige atenção?"
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedList.forEach { condicao ->
                    InputChip(
                        selected = true,
                        onClick = { 
                            val newList = selectedList - condicao
                            selectedList = newList
                            onValueChange(newList.joinToString(", ").ifBlank { "Nenhum" })
                        },
                        label = { Text(condicao) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Selecionar Condições")
            }
        }
    }

    if (showDialog) {
        SaudeSelectionDialog(
            title = "Condições de Saúde",
            items = saudeCatalogo.filter { it.tipo == com.katui.offline.data.local.entities.TipoSaudeItem.CONDICAO },
            selectedItems = selectedList,
            onDismiss = { showDialog = false },
            onConfirm = { 
                selectedList = it
                onValueChange(it.joinToString(", ").ifBlank { "Nenhum" })
            }
        )
    }
}

@Composable
fun SummaryStep(nome: String, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.logo_katui_new),
            contentDescription = "Katu'I Logo",
            modifier = Modifier
                .height(100.dp)
                .padding(bottom = 24.dp)
        )
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tudo pronto, $nome!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Já salvei suas informações. Agora vamos começar a usar o aplicativo!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Entrar no App", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun AiOptionalStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.AutoFixHigh,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Funções com IA",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O Katu'I pode ler receitas e exames automaticamente usando Inteligência Artificial.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Como o app é focado em privacidade e funciona offline, para usar essa função você precisará inserir sua própria 'API Key' do Google Gemini (é gratuita) nas configurações do app mais tarde.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Você pode configurar isso quando quiser!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StepContent(
    title: String,
    description: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    isNextEnabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / (totalSteps - 1) },
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(bottom = 16.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar", style = MaterialTheme.typography.bodyLarge)
            }
            
            Button(
                onClick = onNext,
                enabled = isNextEnabled,
                modifier = Modifier.height(56.dp).padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Próximo", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
