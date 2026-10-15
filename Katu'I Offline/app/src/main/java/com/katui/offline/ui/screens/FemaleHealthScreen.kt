package com.katui.offline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Anticoncepcional
import com.katui.offline.data.local.entities.CicloMenstrual
import com.katui.offline.data.local.entities.IntensidadeFluxo
import com.katui.offline.data.local.entities.SintomaDiarioFeminino
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.DateSelectorField
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.components.TwoColumnGrid
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.FemaleHealthViewModel
import com.katui.offline.ui.viewmodel.PregnancyViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FemaleHealthScreen(
    viewModel: FemaleHealthViewModel = hiltViewModel(),
    pregnancyViewModel: PregnancyViewModel = hiltViewModel(),
    onNavigateToPregnancy: () -> Unit = {}
) {
    val ciclos by viewModel.ciclos.collectAsState()
    val sintomasDiarios by viewModel.sintomasDiarios.collectAsState()
    val mediaCiclo by viewModel.mediaCiclo.collectAsState()
    val mediaDuracao by viewModel.mediaDuracao.collectAsState()
    val proximaMenstruacao by viewModel.proximaMenstruacao.collectAsState()
    val diasFerteis by viewModel.diasFerteis.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val previsaoFerteis by viewModel.previsaoProximosDiasFerteis.collectAsState()
    val diasEstimadosCicloAberto by viewModel.diasEstimadosCicloAberto.collectAsState()
    val previsaoProximoPeriodo by viewModel.previsaoProximoPeriodo.collectAsState()
    val diasPrevistos = remember(diasEstimadosCicloAberto, previsaoProximoPeriodo) {
        (diasEstimadosCicloAberto + previsaoProximoPeriodo).distinct()
    }
    val anti by viewModel.anticoncepcional.collectAsState()
    
    val gestacao by pregnancyViewModel.gestacao.collectAsState()
    val pregnancyHidden by pregnancyViewModel.pregnancyHidden.collectAsState()
    
    var showAddCicloDialog by remember { mutableStateOf(false) }
    var showAntiDialog by remember { mutableStateOf(false) }
    var showRetroactiveDialog by remember { mutableStateOf(false) }
    var showHealthSettingsDialog by remember { mutableStateOf(false) }
    var selectedSintomaForEdit by remember { mutableStateOf<SintomaDiarioFeminino?>(null) }
    var selectedCicloForEdit by remember { mutableStateOf<CicloMenstrual?>(null) }
    var selectedDateForAction by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    if (!pregnancyHidden) {
                        IconButton(onClick = { showHealthSettingsDialog = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Configurações de Saúde")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { showRetroactiveDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.History, contentDescription = "Registrar Retroativo", tint = MaterialTheme.colorScheme.onSecondary)
                }
                SmallFloatingActionButton(onClick = { showAntiDialog = true }, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.Medication, contentDescription = "Anticoncepcional", tint = MaterialTheme.colorScheme.onSecondary)
                }
                // O botão de + agora abre o registro para hoje, mas encorajamos o uso do calendário
                FloatingActionButton(onClick = { selectedDateForAction = LocalDate.now() }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Default.Add, contentDescription = "Registrar Hoje")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Título Integrado
                val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                    "Saúde de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
                } else {
                    "Saúde Feminina"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    text = "Toque em um dia no calendário para registrar o início/fim da menstruação, sintomas ou humor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            // Card de Gestação (se ativo)
            gestacao?.let { g ->
                item {
                    PregnancySummaryCard(
                        gestacao = g,
                        onClick = onNavigateToPregnancy
                    )
                }
            }

            // Calendário
            item {
                HealthCalendar(
                    ciclos = ciclos, 
                    diasFerteis = diasFerteis + previsaoFerteis,
                    diasPrevistos = diasPrevistos,
                    onDateSelected = { selectedDateForAction = it }
                )
            }

            item {
                CalendarLegend()
            }

            // Resumo e Predição
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Previsão do Próximo Ciclo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = proximaMenstruacao?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Aguardando mais dados",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Ciclo: $mediaCiclo dias", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Duração: $mediaDuracao dias", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val diasRestantes = proximaMenstruacao?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
                        if (diasRestantes != null) {
                            Text(
                                text = if (diasRestantes > 0) "Faltam aproximadamente $diasRestantes dias" else "Deve iniciar a qualquer momento!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Seção Anticoncepcional
            item {
                AnticoncepcionalSection(
                    anti = anti, 
                    onConfig = { showAntiDialog = true }, 
                    onToggleTomada = { viewModel.registrarTomadaPilha(it) },
                    onStop = { viewModel.pararAnticoncepcional() }
                )
            }

            item {
                Text("Histórico de Sintomas", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            if (sintomasDiarios.isEmpty()) {
                item { Text("Nenhum sintoma registrado ainda.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(sintomasDiarios) { sintoma ->
                    SintomaItemDetailed(
                        sintoma = sintoma,
                        onEdit = { selectedSintomaForEdit = sintoma }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ciclos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            if (ciclos.isEmpty()) {
                item { Text("Nenhum ciclo registrado ainda.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(ciclos) { ciclo ->
                    CicloItemDetailed(
                        ciclo = ciclo,
                        onUpdateFim = { viewModel.registrarFimCiclo(ciclo, it) },
                        onEdit = { selectedCicloForEdit = ciclo },
                        onDelete = { viewModel.excluirCiclo(ciclo) }
                    )
                }
            }
        }

        if (showAddCicloDialog) {
            AddCicloDialog(onDismiss = { showAddCicloDialog = false }, onConfirm = { viewModel.registrarInicioCiclo(it) })
        }

        if (showAntiDialog) {
            ConfigAntiDialog(onDismiss = { showAntiDialog = false }, onConfirm = { nome, tipo, hora -> viewModel.configurarAnticoncepcional(nome, tipo, hora) })
        }

        if (selectedSintomaForEdit != null) {
            SymptomHumorDialog(
                initialData = selectedSintomaForEdit!!,
                onDismiss = { selectedSintomaForEdit = null },
                onSave = { data, intensidade, sintomas, humor ->
                    viewModel.salvarSintomasDiarios(data, intensidade, sintomas, humor)
                    selectedSintomaForEdit = null
                }
            )
        }

        if (selectedCicloForEdit != null) {
            EditCicloDialog(
                ciclo = selectedCicloForEdit!!,
                onDismiss = { selectedCicloForEdit = null },
                onConfirm = { novoInicio, novoFim ->
                    viewModel.editarCiclo(selectedCicloForEdit!!, novoInicio, novoFim)
                    selectedCicloForEdit = null
                }
            )
        }

        if (selectedDateForAction != null) {
            val date = selectedDateForAction!!
            // Ciclo que já contempla este dia dentro do intervalo confirmado [início, fim].
            // Para ciclos ainda abertos (sem fim), o intervalo "já registrado" é só o dia de início
            // (os dias seguintes são apenas estimativa, tratados por diasEstimadosCicloAberto).
            val cicloDoDia = ciclos.find { ciclo ->
                val fim = ciclo.dataFim ?: ciclo.dataInicio
                !date.isBefore(ciclo.dataInicio) && !date.isAfter(fim)
            }
            val cicloAtivo = ciclos.find { it.dataFim == null && !date.isBefore(it.dataInicio) }
            
            val sintomaNoDia = sintomasDiarios.find { it.data == date }
            
            val isFertile = date in (diasFerteis + previsaoFerteis)
            val isEstimado = date in diasPrevistos

            AlertDialog(
                onDismissRequest = { selectedDateForAction = null },
                title = { 
                    Column {
                        Text(date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale.forLanguageTag("pt-BR"))), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        if (isFertile) {
                            Surface(
                                color = AccentPurple.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(14.dp), tint = AccentPurple)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Período Fértil / Ovulação", style = MaterialTheme.typography.labelSmall, color = AccentPurple, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (isEstimado) {
                            Surface(
                                color = PrevisaoMenstruacao.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, Modifier.size(14.dp), tint = AccentPink)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dia estimado de menstruação", style = MaterialTheme.typography.labelSmall, color = AccentPink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (sintomaNoDia != null) {
                            Text("Registro do Dia:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("• Intensidade: ${sintomaNoDia.intensidade}", style = MaterialTheme.typography.bodySmall)
                            if (!sintomaNoDia.sintomas.isNullOrBlank()) Text("• Sintomas: ${sintomaNoDia.sintomas}", style = MaterialTheme.typography.bodySmall)
                            if (!sintomaNoDia.humor.isNullOrBlank()) Text("• Humor: ${sintomaNoDia.humor}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }

                        if (cicloDoDia != null) {
                            when {
                                cicloDoDia.dataInicio == date && cicloDoDia.dataFim == date ->
                                    Text("Ciclo registrado neste único dia (${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}).")
                                cicloDoDia.dataInicio == date ->
                                    Text("Início de ciclo registrado em ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}.")
                                cicloDoDia.dataFim == date ->
                                    Text("Fim de ciclo registrado em ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}.")
                                else ->
                                    Text("Dia dentro do ciclo iniciado em ${cicloDoDia.dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} e encerrado em ${cicloDoDia.dataFim?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}.")
                            }
                        } else if (cicloAtivo != null) {
                            Text("Ciclo ativo (Iniciado em ${cicloAtivo.dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))})")
                        } else {
                            Text("Deseja marcar este dia como o início da sua menstruação?")
                        }
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botão Principal: Registrar Sintomas (Sempre útil)
                        Button(
                            onClick = { 
                                selectedSintomaForEdit = sintomaNoDia ?: SintomaDiarioFeminino(usuarioId = 0, data = date)
                                selectedDateForAction = null 
                            }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (sintomaNoDia != null) "Editar Sintomas e Fluxo" else "Registrar Fluxo e Sintomas")
                        }

                        // Botão de Ciclo: Iniciar ou Encerrar
                        if (cicloDoDia == null && cicloAtivo == null) {
                            OutlinedButton(
                                onClick = { viewModel.registrarInicioCiclo(date); selectedDateForAction = null },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcar como Início do Ciclo")
                            }
                        }
                        
                        if (cicloAtivo != null && (cicloAtivo.dataFim == null) && date.isAfter(cicloAtivo.dataInicio)) {
                            OutlinedButton(
                                onClick = { viewModel.registrarFimCiclo(cicloAtivo, date); selectedDateForAction = null },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcar como Fim do Ciclo")
                            }
                        }

                        if (cicloDoDia != null) {
                            var showDeleteConfirm by remember { mutableStateOf(false) }
                            if (showDeleteConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirm = false },
                                    title = { Text("Remover Registro de Ciclo?") },
                                    text = { Text("Deseja remover este registro de ciclo (início${if (cicloDoDia.dataFim != null) " e fim" else ""})?") },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            viewModel.excluirCiclo(cicloDoDia)
                                            showDeleteConfirm = false
                                            selectedDateForAction = null 
                                        }) { Text("Remover", color = MaterialTheme.colorScheme.error) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { 
                                    selectedCicloForEdit = cicloDoDia
                                    selectedDateForAction = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar Ciclo")
                            }
                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Remover Registro do Ciclo")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        TextButton(
                            onClick = { selectedDateForAction = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }

        if (showRetroactiveDialog) {
            RetroactiveSymptomDialog(
                onDismiss = { showRetroactiveDialog = false },
                onSave = { data, intensidade, sintomas, humor ->
                    viewModel.registrarDadosRetroativos(data, intensidade, sintomas, humor)
                    showRetroactiveDialog = false
                }
            )
        }

        if (showHealthSettingsDialog) {
            HealthSettingsDialog(
                isGestacaoAtiva = gestacao != null,
                onDismiss = { showHealthSettingsDialog = false },
                onToggleGestacao = { 
                    if (it) {
                        // O ViewModel lidará com a criação se não houver uma
                    } else {
                        pregnancyViewModel.encerrarGestacao()
                    }
                },
                onSetHidden = { 
                    pregnancyViewModel.setPregnancyHidden(it)
                    showHealthSettingsDialog = false
                },
                onSetupGestacao = { dum, nome ->
                    pregnancyViewModel.iniciarGestacao(dum, nome)
                    showHealthSettingsDialog = false
                }
            )
        }
    }
}

@Composable
fun PregnancySummaryCard(
    gestacao: com.katui.offline.data.local.entities.Gestacao,
    onClick: () -> Unit
) {
    val week = remember(gestacao) { 
        gestacao.dataUltimaMenstruacao?.let { com.katui.offline.util.PregnancyContentProvider.calculateGestationalWeek(it) } ?: 1 
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gestacao.nomeBebe?.let { "Gestação: $it" } ?: "Minha Gestação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Você está na ${week}ª semana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun HealthSettingsDialog(
    isGestacaoAtiva: Boolean,
    onDismiss: () -> Unit,
    onToggleGestacao: (Boolean) -> Unit,
    onSetHidden: (Boolean) -> Unit,
    onSetupGestacao: (LocalDate, String?) -> Unit
) {
    var showSetupForm by remember { mutableStateOf(false) }
    var dum by remember { mutableStateOf(LocalDate.now().minusWeeks(4)) }
    var nomeBebe by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações de Saúde") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!showSetupForm) {
                    Text(
                        "Aqui você pode ativar ou desativar funções específicas para o seu momento de vida.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isGestacaoAtiva,
                            onCheckedChange = { 
                                if (it) showSetupForm = true else onToggleGestacao(false)
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Modo Gestação")
                    }

                    HorizontalDivider()

                    TextButton(
                        onClick = { onSetHidden(true) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Esconder funções de gestação permanentemente")
                    }
                } else {
                    Text("Conte-nos mais para personalizar seu acompanhamento:", style = MaterialTheme.typography.titleSmall)
                    
                    DateSelectorField(
                        label = "Data da Última Menstruação (DUM)",
                        selectedDate = dum,
                        onDateSelected = { dum = it }
                    )
                    
                    OutlinedTextField(
                        value = nomeBebe,
                        onValueChange = { nomeBebe = it },
                        label = { Text("Apelido do Bebê (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showSetupForm) {
                Button(onClick = { onSetupGestacao(dum, nomeBebe.ifBlank { null }) }) {
                    Text("Confirmar e Ativar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = AccentPink, label = "Menstruação")
        LegendItem(color = PrevisaoMenstruacao, label = "Estimativa")
        LegendItem(color = AccentPurple, label = "Período Fértil")
        LegendItem(color = Color.Transparent, label = "Hoje", border = true)
    }
}

@Composable
fun LegendItem(color: Color, label: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = if (border) Color.Transparent else color.copy(alpha = 0.6f),
            border = if (border) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RetroactiveSymptomDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, IntensidadeFluxo, String, String) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var intensidade by remember { mutableStateOf(IntensidadeFluxo.MEDIA) }
    val selectedSintomas = remember { mutableStateListOf<String>() }
    val selectedHumor = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Registro Retroativo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DateSelectorField(label = "Data do Registro", selectedDate = date, onDateSelected = { date = it })
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Intensidade do Fluxo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val flows = listOf(
                            Triple(IntensidadeFluxo.LEVE, 1, Color(0xFFE57373)),
                            Triple(IntensidadeFluxo.MEDIA, 2, Color(0xFFE53935)),
                            Triple(IntensidadeFluxo.FORTE, 3, Color(0xFFB71C1C))
                        )
                        val labels = listOf("Leve", "Média", "Forte")
                        flows.forEachIndexed { index, (i, count, tint) ->
                            VerticalFlowChip(
                                selected = intensidade == i,
                                onClick = { intensidade = i },
                                icon = Icons.Default.WaterDrop,
                                iconCount = count,
                                iconTint = tint,
                                label = labels[index],
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Column {
                    Text("Sintomas Físicos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sints = listOf(
                        "Cólica" to Pair(Icons.Default.Bolt, Color(0xFFFFA000)),
                        "Dores" to Pair(Icons.Default.MoodBad, Color(0xFFE53935)),
                        "Inchaço" to Pair(Icons.Default.Air, Color(0xFF7B1FA2)),
                        "Sensível" to Pair(Icons.Default.Favorite, Color(0xFFE91E63)),
                        "Enjoo" to Pair(Icons.Default.SentimentDissatisfied, Color(0xFF43A047)),
                        "Cansada" to Pair(Icons.Default.BatteryAlert, Color(0xFF78909C))
                    )
                    TwoColumnGrid(items = sints) { (sName, iconPair) ->
                        val isSelected = sName in selectedSintomas
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedSintomas.remove(sName) else selectedSintomas.add(sName) },
                            label = { Text(sName, maxLines = 1) },
                            leadingIcon = { Icon(iconPair.first, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconPair.second) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Column {
                    Text("Humor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val humors = listOf(
                        "Feliz" to Pair(Icons.Default.WbSunny, Color(0xFFFFA000)),
                        "Irritada" to Pair(Icons.Default.Bolt, Color(0xFFE53935)),
                        "Triste" to Pair(Icons.Default.WbCloudy, Color(0xFF1E88E5)),
                        "Desânimo" to Pair(Icons.Default.BatteryAlert, Color(0xFF78909C)),
                        "Ansiosa" to Pair(Icons.Default.Psychology, Color(0xFF8E24AA)),
                        "Depressiva" to Pair(Icons.Default.MoodBad, Color(0xFF546E7A)),
                        "Apática" to Pair(Icons.Default.AcUnit, Color(0xFF00ACC1))
                    )
                    TwoColumnGrid(items = humors) { (hName, iconPair) ->
                        val isSelected = hName in selectedHumor
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedHumor.remove(hName) else selectedHumor.add(hName) },
                            label = { Text(hName, maxLines = 1) },
                            leadingIcon = { Icon(iconPair.first, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconPair.second) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(date, intensidade, selectedSintomas.joinToString(","), selectedHumor.joinToString(","))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Salvar Registro") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    )
}

@Composable
fun HealthCalendar(
    ciclos: List<CicloMenstrual>,
    diasFerteis: List<LocalDate> = emptyList(),
    diasPrevistos: List<LocalDate> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
    
    // Ajuste para Domingo ser o primeiro dia da semana (0)
    // DayOfWeek: Monday=1, ..., Sunday=7
    val offset = if (firstDayOfMonth == DayOfWeek.SUNDAY) 0 else firstDayOfMonth.value

    val days = mutableListOf<LocalDate?>()
    repeat(offset) { days.add(null) }
    for (i in 1..daysInMonth) {
        days.add(currentMonth.atDay(i))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo")
                }
            }

            // Header dias da semana
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekDays = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab")
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Grid de dias
            val rows = days.chunked(7)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .padding(1.dp)
                                .then(
                                    if (date != null) Modifier.clickable { onDateSelected(date) } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                // Dia confirmado: apenas dentro do intervalo [início, fim] quando o ciclo já foi
                                // encerrado, ou o próprio dia de início quando o ciclo ainda está aberto.
                                // (Antes, um ciclo aberto pintava infinitamente todos os dias futuros.)
                                val isInCycle = ciclos.any { ciclo ->
                                    val fim = ciclo.dataFim ?: ciclo.dataInicio
                                    !date.isBefore(ciclo.dataInicio) && !date.isAfter(fim)
                                }
                                
                                val isStart = ciclos.any { it.dataInicio == date }
                                val isEnd = ciclos.any { it.dataFim == date }
                                val isFertile = date in diasFerteis
                                val isPrevisto = !isInCycle && date in diasPrevistos

                                if (isInCycle) {
                                    Surface(
                                        shape = if (isStart) MaterialTheme.shapes.small else if (isEnd) MaterialTheme.shapes.small else androidx.compose.ui.graphics.RectangleShape,
                                        color = AccentPink.copy(alpha = if (isStart || isEnd) 0.6f else 0.3f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                } else if (isPrevisto) {
                                    // Dia estimado (continuação do ciclo aberto ou previsão do próximo período):
                                    // cor clara, distinta da menstruação confirmada.
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = PrevisaoMenstruacao.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                } else if (isFertile) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = AccentPurple.copy(alpha = 0.2f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                }

                                if (date == LocalDate.now()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color.Transparent
                                    ) {}
                                }

                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isInCycle) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isInCycle || date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    if (row.size < 7) {
                        repeat(7 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnticoncepcionalSection(
    anti: Anticoncepcional?, 
    onConfig: () -> Unit, 
    onToggleTomada: (Boolean) -> Unit,
    onStop: () -> Unit = {}
) {
    var showStopConfirm by remember { mutableStateOf(false) }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = { showStopConfirm = false },
            title = { Text("Parar Método?") },
            text = { Text("Isso desativará os lembretes e removerá o anticoncepcional da lista de remédios gerais. Os registros passados serão mantidos.") },
            confirmButton = {
                Button(onClick = { onStop(); showStopConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Parar Método")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Anticoncepcional", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row {
                    if (anti != null) {
                        IconButton(onClick = { showStopConfirm = true }) { 
                            Icon(Icons.Default.Delete, contentDescription = "Parar", tint = MaterialTheme.colorScheme.error) 
                        }
                    }
                    IconButton(onClick = onConfig) { 
                        Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = MaterialTheme.colorScheme.primary) 
                    }
                }
            }
            
            if (anti != null) {
                Text(anti.nome, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Horário: ${anti.horarioTomada}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onToggleTomada(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marcar como Tomado Hoje")
                }
            } else {
                Text("Nenhum método configurado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onConfig, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("Configurar agora") }
            }
        }
    }
}

@Composable
fun SintomaItemDetailed(sintoma: SintomaDiarioFeminino, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sintoma.data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onEdit) { 
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) 
                }
            }
            
            Text("Intensidade: ${sintoma.intensidade}", style = MaterialTheme.typography.bodySmall)
            if (!sintoma.sintomas.isNullOrBlank()) Text("Sintomas: ${sintoma.sintomas}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!sintoma.humor.isNullOrBlank()) Text("Humor: ${sintoma.humor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CicloItemDetailed(ciclo: CicloMenstrual, onUpdateFim: (LocalDate) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Ciclo?") },
            text = { Text("Tem certeza que deseja excluir este registro de ciclo? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Início: ${ciclo.dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (ciclo.dataFim != null) "Fim: ${ciclo.dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}" else "Ativo",
                        color = if (ciclo.dataFim == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar datas", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                }
            }

            if (ciclo.dataFim == null) {
                Button(
                    onClick = { onUpdateFim(LocalDate.now()) },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Encerrar Ciclo Hoje") }
            }
        }
    }
}

@Composable
fun EditCicloDialog(ciclo: CicloMenstrual, onDismiss: () -> Unit, onConfirm: (LocalDate, LocalDate?) -> Unit) {
    var inicio by remember { mutableStateOf(ciclo.dataInicio) }
    var temFim by remember { mutableStateOf(ciclo.dataFim != null) }
    var fim by remember { mutableStateOf(ciclo.dataFim ?: ciclo.dataInicio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Ciclo") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Corrija a data de início ou defina/ajuste o fim deste período.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateSelectorField(label = "Início", selectedDate = inicio, onDateSelected = { inicio = it })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = temFim, onCheckedChange = { temFim = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (temFim) "Ciclo encerrado" else "Ciclo ainda em aberto")
                }

                if (temFim) {
                    DateSelectorField(label = "Fim", selectedDate = fim, onDateSelected = { fim = it })
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfirm(inicio, if (temFim) fim else null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !temFim || !fim.isBefore(inicio)
                ) { Text("Salvar") }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun AddCicloDialog(onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = AccentPink)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Início de Ciclo")
            }
        },
        text = { 
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Selecione o primeiro dia da sua última menstruação para atualizar suas previsões.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateSelectorField(
                    label = "Data de Início", 
                    selectedDate = date, 
                    onDateSelected = { date = it },
                    maxDate = LocalDate.now()
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfirm(date); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirmar Registro") }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun ConfigAntiDialog(onDismiss: () -> Unit, onConfirm: (String, Int, String) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("21") }
    var hora by remember { mutableStateOf(LocalTime.of(8, 0)) }
    
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sobre as Cartelas")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Escolha o tipo que corresponde à sua cartela atual:", fontWeight = FontWeight.Bold)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• 21 dias: Você toma por 21 dias e faz uma pausa de 7 dias.")
                        Text("• 24 dias: Você toma por 24 dias e faz uma pausa de 4 dias.")
                        Text("• 28 dias: Cartela sem pausas (uso contínuo ou com placebos).")
                        Text("• Contínuo: Para injeções ou pílulas de uso ininterrupto.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Entendi") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = AccentTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurar Anticoncepcional")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Configure o lembrete diário para não esquecer de tomar sua pílula.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Medicamento") },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: Selene, Ciclo 21...") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipo de Cartela", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("21", "24", "28", "Cont").forEach { t ->
                            FilterChip(
                                selected = tipo == t,
                                onClick = { tipo = t },
                                label = { Text(t, modifier = Modifier.padding(vertical = 4.dp)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                TimeSelectorField(
                    label = "Horário de Tomada",
                    selectedTime = hora,
                    onTimeSelected = { hora = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val tipoInt = if (tipo == "Cont") 0 else tipo.toInt()
                    onConfirm(nome, tipoInt, hora.format(DateTimeFormatter.ofPattern("HH:mm")))
                    onDismiss() 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nome.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Salvar Configuração") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomHumorDialog(
    initialData: SintomaDiarioFeminino, 
    onDismiss: () -> Unit, 
    onSave: (LocalDate, IntensidadeFluxo, String, String) -> Unit
) {
    var intensidade by remember { mutableStateOf(initialData.intensidade) }
    val selectedSintomas = remember { mutableStateListOf<String>().apply { initialData.sintomas?.split(",")?.filter { it.isNotBlank() }?.let { addAll(it) } } }
    val selectedHumor = remember { mutableStateListOf<String>().apply { initialData.humor?.split(",")?.filter { it.isNotBlank() }?.let { addAll(it) } } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = AccentPink, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sintomas e Humor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    initialData.data.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale.forLanguageTag("pt-BR"))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Intensidade do Fluxo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val flows = listOf(
                            Triple(IntensidadeFluxo.LEVE, 1, Color(0xFFE57373)),
                            Triple(IntensidadeFluxo.MEDIA, 2, Color(0xFFE53935)),
                            Triple(IntensidadeFluxo.FORTE, 3, Color(0xFFB71C1C))
                        )
                        val labels = listOf("Leve", "Média", "Forte")
                        flows.forEachIndexed { index, (i, count, tint) ->
                            VerticalFlowChip(
                                selected = intensidade == i,
                                onClick = { intensidade = i },
                                icon = Icons.Default.WaterDrop,
                                iconCount = count,
                                iconTint = tint,
                                label = labels[index],
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Column {
                    Text("Sintomas Físicos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sints = listOf(
                        "Cólica" to Pair(Icons.Default.Bolt, Color(0xFFFFA000)),
                        "Dores" to Pair(Icons.Default.MoodBad, Color(0xFFE53935)),
                        "Inchaço" to Pair(Icons.Default.Air, Color(0xFF7B1FA2)),
                        "Sensível" to Pair(Icons.Default.Favorite, Color(0xFFE91E63)),
                        "Enjoo" to Pair(Icons.Default.SentimentDissatisfied, Color(0xFF43A047)),
                        "Cansada" to Pair(Icons.Default.BatteryAlert, Color(0xFF78909C))
                    )
                    TwoColumnGrid(items = sints) { (sName, iconPair) ->
                        val isSelected = sName in selectedSintomas
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedSintomas.remove(sName) else selectedSintomas.add(sName) },
                            label = { Text(sName, maxLines = 1) },
                            leadingIcon = { Icon(iconPair.first, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconPair.second) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Column {
                    Text("Como está seu Humor?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val humors = listOf(
                        "Feliz" to Pair(Icons.Default.WbSunny, Color(0xFFFFA000)),
                        "Irritada" to Pair(Icons.Default.Bolt, Color(0xFFE53935)),
                        "Triste" to Pair(Icons.Default.WbCloudy, Color(0xFF1E88E5)),
                        "Desânimo" to Pair(Icons.Default.BatteryAlert, Color(0xFF78909C)),
                        "Ansiosa" to Pair(Icons.Default.Psychology, Color(0xFF8E24AA)),
                        "Depressiva" to Pair(Icons.Default.MoodBad, Color(0xFF546E7A)),
                        "Apática" to Pair(Icons.Default.AcUnit, Color(0xFF00ACC1))
                    )
                    TwoColumnGrid(items = humors) { (hName, iconPair) ->
                        val isSelected = hName in selectedHumor
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedHumor.remove(hName) else selectedHumor.add(hName) },
                            label = { Text(hName, maxLines = 1) },
                            leadingIcon = { Icon(iconPair.first, contentDescription = null, modifier = Modifier.size(16.dp), tint = iconPair.second) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(initialData.data, intensidade, selectedSintomas.joinToString(","), selectedHumor.joinToString(","))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Salvar Registro") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    )
}

@Composable
fun VerticalFlowChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconCount: Int,
    iconTint: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(iconCount) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconTint)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
