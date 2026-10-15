package com.katui.offline.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.Screen
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.*
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.*
import com.katui.offline.util.UiTextHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DashboardAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val hasSubmenu: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val compromissos by viewModel.proximosCompromissos.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val isSaudeFemininaEnabled = usuario?.sexo == SexoBiologico.FEMININO || usuario?.sexo == SexoBiologico.INTERSEXO
    
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isQuickActionExpanded by remember { mutableStateOf(false) }
    var expandedSubmenu by remember { mutableStateOf<String?>(null) }

    // ViewModels para Diálogos
    val symptomViewModel: SymptomViewModel = hiltViewModel()
    val medicationViewModel: MedicationViewModel = hiltViewModel()
    val consultationViewModel: ConsultationViewModel = hiltViewModel()
    val vitalsViewModel: VitalsViewModel = hiltViewModel()
    val exameViewModel: ExameViewModel = hiltViewModel()
    val femaleHealthViewModel: FemaleHealthViewModel = hiltViewModel()

    // Estados de Diálogos
    var showAddSymptom by remember { mutableStateOf(false) }
    var showAddMedication by remember { mutableStateOf(false) }
    var showAddConsultation by remember { mutableStateOf(false) }
    var showAddExam by remember { mutableStateOf(false) }
    var showAddVitalsType by remember { mutableStateOf<String?>(null) }
    var showFemaleAction by remember { mutableStateOf<String?>(null) }

    // Estados para Exames IA
    val extractedResults by exameViewModel.extractedResults.collectAsState()
    val extractionError by exameViewModel.extractionError.collectAsState()
    val isProcessingExame by exameViewModel.isProcessing.collectAsState()

    // FIX: fechar o FAB expandido ao pressionar o botão voltar do sistema
    BackHandler(enabled = isQuickActionExpanded || expandedSubmenu != null) {
        if (expandedSubmenu != null) {
            expandedSubmenu = null
        } else {
            isQuickActionExpanded = false
        }
    }

    val alarmesDoDia = viewModel.getAlarmesForDate(selectedDate)
    val sintomasDoDia = viewModel.getSintomasForDate(selectedDate)
    val consultasDoDia = viewModel.getConsultasForDate(selectedDate)
    val vitalsDoDia = viewModel.getVitalsForDate(selectedDate)

    var showAlarmDetail by remember { mutableStateOf<List<com.katui.offline.data.local.entities.Alarme>?>(null) }
    var showSymptomDetail by remember { mutableStateOf<List<com.katui.offline.data.local.entities.Sintoma>?>(null) }
    var showConsultationDetail by remember { mutableStateOf<List<com.katui.offline.data.local.entities.Consulta>?>(null) }
    var showVitalsDetail by remember { mutableStateOf<List<com.katui.offline.data.local.entities.Vitals>?>(null) }

    val quickActions = remember(usuario, isSaudeFemininaEnabled) {
        mutableListOf(
            DashboardAction("Sintoma", Icons.AutoMirrored.Filled.Assignment, AccentPurple, onClick = { showAddSymptom = true }),
            DashboardAction("Remédio", Icons.Default.MedicalServices, AccentTeal, onClick = { showAddMedication = true }),
            DashboardAction("Sinais Vitais", Icons.Default.MonitorHeart, AccentRed, hasSubmenu = true),
            DashboardAction("Consulta", Icons.Default.Event, AccentBlue, onClick = { showAddConsultation = true }),
            DashboardAction("Exame", Icons.Default.Description, AccentPink, onClick = { showAddExam = true }),
        ).also { list ->
            if (isSaudeFemininaEnabled) {
                list.add(DashboardAction("Saúde Feminina", Icons.Default.Female, AccentMagenta, hasSubmenu = true))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item { 
                val userColor = UiTextHelper.getUserColor(usuario)
                HeaderSection(usuario?.nome, usuario?.tipo, usuario?.cuidadorId, usuario?.sexo, userColor) 
            }

            item {
                CalendarSection(
                    selectedDate = selectedDate,
                    isCalendarExpanded = isCalendarExpanded,
                    onToggleCalendar = { isCalendarExpanded = !isCalendarExpanded },
                    onDateSelected = { viewModel.selectDate(it) },
                    dayIntensityProvider = { viewModel.getDayIntensity(it) },
                    hasDataIndicator = { date ->
                        viewModel.getAlarmesForDate(date).isNotEmpty() ||
                        viewModel.getConsultasForDate(date).isNotEmpty()
                    },
                    isDiaMenstruacao = if (isSaudeFemininaEnabled) { date -> viewModel.isDiaMenstruacao(date) } else null,
                    isDiaFertil = if (isSaudeFemininaEnabled) { date -> viewModel.isDiaFertil(date) } else null,
                    isDiaPrevisaoMenstruacao = if (isSaudeFemininaEnabled) { date -> viewModel.isDiaPrevisaoMenstruacao(date) } else null,
                    isDiaAnticoncepcional = if (isSaudeFemininaEnabled) { date -> viewModel.isDiaAnticoncepcional(date) } else null
                )
            }

            item { SectionTitle("Resumo de hoje", selectedDate) }

            item {
                SummarySection(
                    alarmes = alarmesDoDia,
                    sintomas = sintomasDoDia,
                    consultas = consultasDoDia,
                    vitals = vitalsDoDia,
                    onNavigate = { route -> onNavigate(route, true) },
                    onShowAlarmDetail = { showAlarmDetail = alarmesDoDia },
                    onShowSymptomDetail = { showSymptomDetail = sintomasDoDia },
                    onShowConsultationDetail = { showConsultationDetail = consultasDoDia },
                    onShowVitalsDetail = { showVitalsDetail = vitalsDoDia }
                )
            }

            item { SectionTitle("Próximos compromissos") }

            if (compromissos.isEmpty()) {
                item { EmptyStateCard("Nenhum compromisso para os próximos dias.") }
            } else {
                items(compromissos.size) { index ->
                    val comp = compromissos[index]
                    val isTomado = comp.alarmeReferencia?.tomado == true
                    
                    CompositionLocalProvider(
                        LocalContentColor provides if (isTomado) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else LocalContentColor.current
                    ) {
                        AppointmentItem(
                            comp = comp,
                            onClick = {
                                val route = when (comp.tipo) {
                                    TipoCompromisso.MEDICAMENTO -> Screen.Medications.route
                                    TipoCompromisso.CONSULTA -> Screen.Consultations.route
                                    TipoCompromisso.SAUDE_FEMININA -> Screen.FemaleHealth.route
                                }
                                onNavigate(route, true)
                            }
                        )
                    }
                }
            }
        }

        // FAB e Submenus
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Menu Principal de Ações
            if (isQuickActionExpanded) {
                // Revertendo a lista para que o "Arrangement.spacedBy" da Column (que empilha para cima)
                // mostre as ações na ordem correta: Sintoma no topo, Saúde Feminina perto do FAB.
                quickActions.asReversed().forEach { action ->
                    val isSubmenuOpen = expandedSubmenu == action.label
                    
                    // Se for um item com submenu e ele estiver aberto, mostramos os sub-itens ACIMA dele
                    // (que visualmente aparecerão abaixo na ordem de empilhamento se não invertermos)
                    // Mas como a Column empilha de baixo para cima, se queremos o submenu "abaixo" (mais perto do FAB) do item clicado,
                    // devemos renderizá-lo DEPOIS do item na ordem da Column.
                    
                    QuickActionFab(
                        label = action.label,
                        icon = if (isSubmenuOpen) Icons.Default.Close else action.icon,
                        color = if (isSubmenuOpen) MaterialTheme.colorScheme.outline else action.color,
                        onClick = {
                            if (action.hasSubmenu) {
                                expandedSubmenu = if (isSubmenuOpen) null else action.label
                            } else {
                                action.onClick?.invoke()
                                isQuickActionExpanded = false
                                expandedSubmenu = null
                            }
                        }
                    )

                    // Sub-itens renderizados logo após o item pai (ficando mais perto do FAB / abaixo dele visualmente)
                    AnimatedVisibility(
                        visible = isSubmenuOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            when (action.label) {
                                "Sinais Vitais" -> {
                                    val vitalsItems = listOf(
                                        Triple("Glicose", Icons.Default.Bloodtype, AccentPink),
                                        Triple("Pressão", Icons.Default.Compress, AccentOrange),
                                        Triple("Peso", Icons.Default.MonitorWeight, AccentBlue),
                                        Triple("Temperatura", Icons.Default.DeviceThermostat, AccentPurple)
                                    )
                                    vitalsItems.forEach { (label, icon, color) ->
                                        QuickActionFab(label, icon, color, onClick = {
                                            showAddVitalsType = label
                                            expandedSubmenu = null
                                            isQuickActionExpanded = false
                                        }, isSubItem = true)
                                    }
                                }
                                "Saúde Feminina" -> {
                                    QuickActionFab("Início de Ciclo", Icons.Default.WaterDrop, AccentPink, onClick = {
                                        showFemaleAction = "INICIO_CICLO"
                                        expandedSubmenu = null
                                        isQuickActionExpanded = false
                                    }, isSubItem = true)
                                    QuickActionFab("Sintomas/Fluxo", Icons.Default.Favorite, AccentMagenta, onClick = {
                                        showFemaleAction = "SINTOMAS_FLUXO"
                                        expandedSubmenu = null
                                        isQuickActionExpanded = false
                                    }, isSubItem = true)
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { 
                    isQuickActionExpanded = !isQuickActionExpanded
                    expandedSubmenu = null
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (isQuickActionExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Menu de Ações Rápidas"
                )
            }
        }
    }

    // --- DIÁLOGOS DE REGISTRO DIRETO ---

    if (showAddSymptom) {
        val searchResults by symptomViewModel.searchResults.collectAsState()
        val sideEffects by symptomViewModel.alertaEfeitoColateral.collectAsState()
        
        AddSymptomDialog(
            editingSintoma = null,
            initialType = "",
            onDismiss = { showAddSymptom = false },
            onConfirm = { symptomViewModel.addSintoma(it); showAddSymptom = false },
            searchResults = searchResults,
            onSearch = { symptomViewModel.setSearchQuery(it) },
            alertaEfeitoColateral = sideEffects,
            onSintomaCatalogoSelecionado = { symptomViewModel.verificarEfeitoColateral(it) }
        )
    }

    if (showAddMedication) {
        val searchResults by medicationViewModel.searchResults.collectAsState()
        AddMedicationDialog(
            onDismiss = { showAddMedication = false },
            onConfirm = { med, createAlarms -> medicationViewModel.addMedicamento(med, createAlarms); showAddMedication = false },
            searchResults = searchResults,
            onSearch = { medicationViewModel.setSearchQuery(it) },
            viewModel = medicationViewModel
        )
    }

    if (showAddConsultation) {
        ConsultationDialog(
            consulta = null,
            viewModel = consultationViewModel,
            onDismiss = { showAddConsultation = false },
            onConfirm = { consulta, lembretes -> 
                consultationViewModel.addConsulta(consulta, lembretes)
                showAddConsultation = false
            }
        )
    }

    if (showAddExam) {
        AddExameDialog(
            onDismiss = { exameViewModel.clearExtractedResults(); showAddExam = false },
            onConfirmImage = { n, c, o, p, r -> exameViewModel.addExame(n, c, o, p, r); showAddExam = false },
            onConfirmPdf = { n, c, o, u, r -> exameViewModel.addExameFromPdf(context, n, c, o, u, r); showAddExam = false },
            onConfirmMultipleImages = { n, c, o, us, r -> exameViewModel.addExameFromImages(context, n, c, o, us, r); showAddExam = false },
            onExtractMetrics = { exameViewModel.extractMetrics(context, it) },
            extractedResults = extractedResults,
            extractionError = extractionError,
            isExtracting = isProcessingExame,
            onNavigateToSettings = { onNavigate(Screen.Settings.route, true) }
        )
    }

    showAddVitalsType?.let { type ->
        AddSpecificVitalsDialog(
            type = type,
            usuario = usuario,
            onDismiss = { showAddVitalsType = null },
            onConfirm = { p, a, s, d, f, g, sa, t, o, dt ->
                vitalsViewModel.addVitals(p, a, s, d, f, g, sa, t, o, dt)
                showAddVitalsType = null
            }
        )
    }

    showFemaleAction?.let { action ->
        when (action) {
            "INICIO_CICLO" -> AddCicloDialog(
                onDismiss = { showFemaleAction = null },
                onConfirm = { femaleHealthViewModel.registrarInicioCiclo(it); showFemaleAction = null }
            )
            "SINTOMAS_FLUXO" -> SymptomHumorDialog(
                initialData = com.katui.offline.data.local.entities.SintomaDiarioFeminino(usuarioId = usuario?.id ?: 0, data = LocalDate.now()),
                onDismiss = { showFemaleAction = null },
                onSave = { d, i, s, h -> femaleHealthViewModel.salvarSintomasDiarios(d, i, s, h); showFemaleAction = null }
            )
        }
    }

    // --- DETALHES DO DIA (EXISTENTES) ---

    showAlarmDetail?.let { list ->
        DetailPopup(title = "Remédios do Dia", onDismiss = { showAlarmDetail = null }, onNavigate = { onNavigate(Screen.Medications.route, true) }) {
            list.sortedBy { it.horario }.forEach { alarme ->
                DetailRow(title = alarme.titulo, subtitle = alarme.horario.format(DateTimeFormatter.ofPattern("HH:mm")), icon = Icons.Default.MedicalServices, color = AccentTeal, isDone = alarme.tomado)
            }
        }
    }

    showSymptomDetail?.let { list ->
        DetailPopup(title = "Sintomas Registrados", onDismiss = { showSymptomDetail = null }, onNavigate = { onNavigate(Screen.Symptoms.route, true) }) {
            list.forEach { symptom ->
                DetailRow(title = symptom.qualidade ?: "Sintoma", subtitle = "Intensidade: ${symptom.intensidadeEscala}", icon = Icons.Default.Warning, color = AccentPurple)
            }
        }
    }

    showConsultationDetail?.let { list ->
        DetailPopup(title = "Consultas do Dia", onDismiss = { showConsultationDetail = null }, onNavigate = { onNavigate(Screen.Consultations.route, true) }) {
            list.forEach { consultation ->
                DetailRow(title = consultation.medico, subtitle = "${consultation.especialidade ?: "Geral"} • ${consultation.dataHora.format(DateTimeFormatter.ofPattern("HH:mm"))}", icon = Icons.Default.Event, color = AccentBlue)
            }
        }
    }

    showVitalsDetail?.let { list ->
        DetailPopup(title = "Sinais Vitais", onDismiss = { showVitalsDetail = null }, onNavigate = { onNavigate(Screen.Vitals.route, true) }) {
            list.forEach { vital ->
                val details = mutableListOf<String>()
                vital.sistolica?.let { details.add("PA: $it/${vital.diastolica}") }
                vital.glicose?.let { details.add("Glicose: $it") }
                vital.peso?.let { details.add("Peso: $it kg") }
                vital.temperatura?.let { details.add("Temp: $it°C") }
                DetailRow(title = details.joinToString(" • "), subtitle = vital.dataHora.format(DateTimeFormatter.ofPattern("HH:mm")), icon = Icons.Default.MonitorHeart, color = AccentRed)
            }
        }
    }
}

@Composable
fun DetailPopup(
    title: String,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            Button(onClick = { onDismiss(); onNavigate() }) {
                Text("Ir para Tela Completa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun DetailRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    isDone: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isDone) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal)
        }
    }
}

@Composable
fun HeaderSection(nome: String?, tipo: TipoUsuario?, cuidadorId: Long?, sexo: com.katui.offline.data.local.entities.SexoBiologico? = null, userColor: Color = MaterialTheme.colorScheme.primary) {
    val primeiroNome = nome?.split(" ")?.firstOrNull() ?: ""
    val isDependente = cuidadorId != null || (tipo != null && tipo != TipoUsuario.CUIDADOR)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        userColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
            .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = com.katui.offline.R.drawable.logo_katui_new),
                contentDescription = "Logo Katu'I",
                modifier = Modifier
                    .height(65.dp)
                    .padding(bottom = 16.dp)
            )

            if (cuidadorId != null) {
                Surface(
                    color = userColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Gerenciando: $primeiroNome",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = userColor
                    )
                }
            }
            val preposicao = when (sexo) {
                com.katui.offline.data.local.entities.SexoBiologico.FEMININO,
                com.katui.offline.data.local.entities.SexoBiologico.INTERSEXO -> "da"
                else -> "do"
            }
            Text(
                text = if (isDependente) "Perfil $preposicao $primeiroNome" else "Olá${if (primeiroNome.isNotEmpty()) ", $primeiroNome" else ""}!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isDependente) "Como $primeiroNome está se sentindo hoje?" else "Como você está se sentindo hoje?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarSection(
    selectedDate: LocalDate,
    isCalendarExpanded: Boolean,
    onToggleCalendar: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    dayIntensityProvider: (LocalDate) -> Int,
    hasDataIndicator: (LocalDate) -> Boolean,
    isDiaMenstruacao: ((LocalDate) -> Boolean)? = null,
    isDiaFertil: ((LocalDate) -> Boolean)? = null,
    isDiaPrevisaoMenstruacao: ((LocalDate) -> Boolean)? = null,
    isDiaAnticoncepcional: ((LocalDate) -> Boolean)? = null
) {
    val temDadosFemininos = isDiaMenstruacao != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isCalendarExpanded) {
                com.katui.offline.ui.components.MonthlyCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    initialExpanded = true,
                    hasDataIndicator = hasDataIndicator,
                    dayBackground = if (temDadosFemininos) { date ->
                        when {
                            isDiaMenstruacao!!(date) -> AccentMagenta.copy(alpha = 0.25f) // Corrected Magenta
                            isDiaPrevisaoMenstruacao?.invoke(date) == true -> PrevisaoMenstruacao.copy(alpha = 0.6f)
                            isDiaFertil!!(date) -> AccentPurple.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        }
                    } else null
                )
            } else {
                WeeklyCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    dayIntensityProvider = dayIntensityProvider,
                    dayBackground = if (temDadosFemininos) { date ->
                        when {
                            isDiaMenstruacao!!(date) -> AccentMagenta.copy(alpha = 0.25f) // Corrected Magenta
                            isDiaPrevisaoMenstruacao?.invoke(date) == true -> PrevisaoMenstruacao.copy(alpha = 0.6f)
                            isDiaFertil!!(date) -> AccentPurple.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        }
                    } else null
                )
            }

            if (temDadosFemininos) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(AccentMagenta.copy(alpha = 0.6f), CircleShape)
                        )
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentMagenta)
                        Text("Menstruação", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(PrevisaoMenstruacao, CircleShape)
                        )
                        Text("Estimativa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(AccentPurple.copy(alpha = 0.5f), CircleShape)
                        )
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentPurple)
                        Text("Fértil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isDiaAnticoncepcional?.invoke(selectedDate) == true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentTeal)
                            Text("Pílula hoje", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                        }
                    }
                }
            }

            TextButton(
                onClick = onToggleCalendar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isCalendarExpanded) "Ver semana" else "Ver mês")
                    Icon(
                        if (isCalendarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, date: LocalDate? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (date != null) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("pt", "BR"))),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SummarySection(
    alarmes: List<com.katui.offline.data.local.entities.Alarme>,
    sintomas: List<com.katui.offline.data.local.entities.Sintoma>,
    consultas: List<com.katui.offline.data.local.entities.Consulta>,
    vitals: List<com.katui.offline.data.local.entities.Vitals>,
    onNavigate: (String) -> Unit = {},
    onShowAlarmDetail: () -> Unit = {},
    onShowSymptomDetail: () -> Unit = {},
    onShowConsultationDetail: () -> Unit = {},
    onShowVitalsDetail: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alarmes.isEmpty() && sintomas.isEmpty() && consultas.isEmpty() && vitals.isEmpty()) {
            EmptyStateCard("Nada registrado para este dia.")
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryBox(
                    title = "Remédios",
                    value = "${alarmes.count { it.tomado }}/${alarmes.size}",
                    icon = Icons.Default.MedicalServices,
                    color = AccentTeal,
                    modifier = Modifier.weight(1f),
                    onClick = onShowAlarmDetail
                )
                SummaryBox(
                    title = "Sintomas",
                    value = "${sintomas.size}",
                    icon = Icons.Default.Warning,
                    color = AccentPurple,
                    modifier = Modifier.weight(1f),
                    onClick = onShowSymptomDetail
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryBox(
                    title = "Consultas",
                    value = "${consultas.size}",
                    icon = Icons.Default.Event,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onShowConsultationDetail
                )
                SummaryBox(
                    title = "Sinais Vitais",
                    value = "${vitals.size}",
                    icon = Icons.Default.MonitorHeart,
                    color = AccentRed,
                    modifier = Modifier.weight(1f),
                    onClick = onShowVitalsDetail
                )
            }
        }
    }
}

@Composable
fun SummaryBox(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .heightIn(min = 110.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun AppointmentItem(
    comp: com.katui.offline.ui.viewmodel.Compromisso,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, color) = when (comp.tipo) {
                com.katui.offline.ui.viewmodel.TipoCompromisso.MEDICAMENTO -> Icons.Default.MedicalServices to AccentTeal
                com.katui.offline.ui.viewmodel.TipoCompromisso.CONSULTA -> Icons.Default.Event to AccentBlue
                com.katui.offline.ui.viewmodel.TipoCompromisso.SAUDE_FEMININA -> Icons.Default.Female to AccentMagenta
            }
            
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (comp.tipo == com.katui.offline.ui.viewmodel.TipoCompromisso.CONSULTA) {
                    Text(
                        text = "Consulta",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = comp.descricao, // Agora contém apenas o nome do médico
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!comp.extraInfo.isNullOrBlank()) {
                        Text(
                            text = comp.extraInfo, // Agora contém a especialidade
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = comp.descricao,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!comp.extraInfo.isNullOrBlank()) {
                        Text(
                            text = comp.extraInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule, 
                        contentDescription = null, 
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = comp.dataHora.format(DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", Locale("pt", "BR"))),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun QuickActionFab(
    label: String,
    icon: ImageVector,
    color: Color,
    isSubItem: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = if (isSubItem) 12.dp else 4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = if (isSubItem) 2.dp else 4.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = if (isSubItem) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
