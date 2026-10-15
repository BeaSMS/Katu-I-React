package com.katui.offline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Consulta
import com.katui.offline.data.local.entities.ConsultaLembrete
import com.katui.offline.data.local.entities.MetodoNotificacao
import com.katui.offline.data.local.entities.TipoLembreteConsulta
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.DateSelectorField
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.viewmodel.ConsultationViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationScreen(
    viewModel: ConsultationViewModel = hiltViewModel()
) {
    val consultas by viewModel.filteredConsultas.collectAsState()
    val upcomingConsultas by viewModel.upcomingConsultas.collectAsState()
    val allConsultas by viewModel.consultas.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var consultaParaEditar by remember { mutableStateOf<Consulta?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agendar Consulta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                "Consultas de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
            } else {
                "Minhas Consultas"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            MonthlyCalendar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                collapsible = false,
                hasDataIndicator = { date ->
                    allConsultas.any { it.dataHora.toLocalDate() == date }
                },
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = if (selectedDate == LocalDate.now()) "Consultas de Hoje" else "Consultas em ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (consultas.isEmpty()) {
                    item {
                        Text(
                            "Nenhuma consulta para este dia",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(consultas) { consulta ->
                        ConsultationItem(
                            consulta = consulta,
                            onDelete = { viewModel.deleteConsulta(consulta) },
                            onEdit = { consultaParaEditar = it }
                        )
                    }
                }

                if (upcomingConsultas.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                        Text(
                            text = "Próximas Consultas (7 dias)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(upcomingConsultas.filter { it.dataHora.toLocalDate() != selectedDate }) { consulta ->
                        ConsultationItem(
                            consulta = consulta,
                            onDelete = { viewModel.deleteConsulta(consulta) },
                            onEdit = { consultaParaEditar = it }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            ConsultationDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onConfirm = { newConsulta, lembretes ->
                    viewModel.addConsulta(newConsulta, lembretes)
                    showAddDialog = false
                }
            )
        }

        consultaParaEditar?.let { consulta ->
            ConsultationDialog(
                consulta = consulta,
                viewModel = viewModel,
                onDismiss = { consultaParaEditar = null },
                onConfirm = { updatedConsulta, lembretes ->
                    viewModel.updateConsulta(updatedConsulta, lembretes)
                    consultaParaEditar = null
                }
            )
        }
    }
}

@Composable
fun ConsultationItem(consulta: Consulta, onDelete: () -> Unit, onEdit: (Consulta) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Consulta?") },
            text = { Text("Deseja desmarcar esta consulta? Esta ação não pode ser desfeita.") },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = consulta.medico, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = { onEdit(consulta) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(text = "Especialidade: ${consulta.especialidade ?: "Geral"}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Data: ${consulta.dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}", style = MaterialTheme.typography.bodyMedium)
            consulta.local?.let { if (it.isNotBlank()) Text(text = "Local: $it", style = MaterialTheme.typography.bodySmall) }
            consulta.observacao?.let { if (it.isNotBlank()) Text(text = "Obs: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun ConsultationDialog(
    consulta: Consulta? = null,
    viewModel: ConsultationViewModel,
    onDismiss: () -> Unit,
    onConfirm: (Consulta, List<ConsultaLembrete>) -> Unit
) {
    var medico by remember { mutableStateOf(consulta?.medico ?: "") }
    var especialidade by remember { mutableStateOf(consulta?.especialidade ?: "") }
    var local by remember { mutableStateOf(consulta?.local ?: "") }
    var observacao by remember { mutableStateOf(consulta?.observacao ?: "") }
    var selectedDate by remember { mutableStateOf(consulta?.dataHora?.toLocalDate() ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(consulta?.dataHora?.toLocalTime() ?: LocalTime.of(9, 0)) }

    var lembretes by remember { 
        mutableStateOf(emptyList<ConsultaLembrete>()) 
    }

    LaunchedEffect(consulta) {
        if (consulta != null) {
            viewModel.getLembretesForConsulta(consulta.id).collectLatest {
                lembretes = it
            }
        } else {
            lembretes = viewModel.getDefaultReminders(selectedDate)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 24.dp),
        title = { 
            Text(
                if (consulta == null) "Agendar Consulta" else "Editar Consulta",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = medico, 
                    onValueChange = { medico = it }, 
                    label = { Text("Nome do Médico") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                OutlinedTextField(
                    value = especialidade, 
                    onValueChange = { especialidade = it }, 
                    label = { Text("Especialidade") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) }
                )
                OutlinedTextField(
                    value = local, 
                    onValueChange = { local = it }, 
                    label = { Text("Local / Clínica") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateSelectorField(
                        label = "Data",
                        selectedDate = selectedDate,
                        onDateSelected = { 
                            val oldDate = selectedDate
                            selectedDate = it 
                            if (consulta == null) {
                                lembretes = lembretes.map { l ->
                                    if (l.tipo == TipoLembreteConsulta.ABSOLUTO && l.horarioAbsoluto?.toLocalDate() == oldDate.minusDays(1)) {
                                        l.copy(horarioAbsoluto = it.minusDays(1).atTime(l.horarioAbsoluto.toLocalTime()))
                                    } else l
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectorField(
                        label = "Hora",
                        selectedTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = observacao, 
                    onValueChange = { observacao = it }, 
                    label = { Text("Observações") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Lembretes e Avisos", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Configure como e quando ser avisado", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.outline
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lembretes.forEachIndexed { index, lembrete ->
                        EnhancedReminderCard(
                            lembrete = lembrete,
                            onDelete = { lembretes = lembretes.filterIndexed { i, _ -> i != index } },
                            onUpdate = { updated -> lembretes = lembretes.mapIndexed { i, l -> if (i == index) updated else l } }
                        )
                    }
                }
                
                Button(
                    onClick = { 
                        lembretes = lembretes + ConsultaLembrete(
                            consultaId = consulta?.id ?: 0,
                            tipo = TipoLembreteConsulta.RELATIVO,
                            metodo = MetodoNotificacao.NOTIFICACAO,
                            minutosAntes = 30,
                            rotulo = "Lembrete extra"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar outro lembrete")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        consulta?.copy(
                            medico = medico,
                            especialidade = especialidade,
                            local = local,
                            dataHora = LocalDateTime.of(selectedDate, selectedTime),
                            observacao = observacao
                        ) ?: Consulta(
                            usuarioId = 0,
                            medico = medico,
                            especialidade = especialidade,
                            local = local,
                            dataHora = LocalDateTime.of(selectedDate, selectedTime),
                            observacao = observacao
                        ),
                        lembretes
                    )
                }, 
                enabled = medico.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Salvar Consulta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EnhancedReminderCard(
    lembrete: ConsultaLembrete,
    onDelete: () -> Unit,
    onUpdate: (ConsultaLembrete) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Seletor de Tipo (Relativo vs Absoluto)
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRelativo = lembrete.tipo == TipoLembreteConsulta.RELATIVO
                    ReminderTypeButton(
                        text = "Antes",
                        selected = isRelativo,
                        onClick = { 
                            onUpdate(lembrete.copy(tipo = TipoLembreteConsulta.RELATIVO, minutosAntes = 60)) 
                        }
                    )
                    ReminderTypeButton(
                        text = "Fixo",
                        selected = !isRelativo,
                        onClick = { 
                            onUpdate(lembrete.copy(tipo = TipoLembreteConsulta.ABSOLUTO, horarioAbsoluto = LocalDateTime.now().withMinute(0).withSecond(0))) 
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botão de Método (Notificação vs Alarme)
                    val isAlarme = lembrete.metodo == MetodoNotificacao.ALARME
                    IconButton(
                        onClick = {
                            onUpdate(lembrete.copy(metodo = if (isAlarme) MetodoNotificacao.NOTIFICACAO else MetodoNotificacao.ALARME))
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isAlarme) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isAlarme) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isAlarme) Icons.Default.Alarm else Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Área de edição do valor/horário
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                if (lembrete.tipo == TipoLembreteConsulta.RELATIVO) {
                    val totalMin = lembrete.minutosAntes ?: 0
                    val isHours = totalMin >= 60 && totalMin % 60 == 0
                    
                    var unitByHours by remember(lembrete) { mutableStateOf(isHours) }
                    val displayValue = if (unitByHours) (totalMin / 60).toString() else totalMin.toString()
                    var text by remember(displayValue) { mutableStateOf(displayValue) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = { 
                                text = it
                                val num = it.toIntOrNull() ?: 0
                                onUpdate(lembrete.copy(minutosAntes = if (unitByHours) num * 60 else num))
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(IntrinsicSize.Min).minWidth(30.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Seletor de Unidade (Minutos vs Horas)
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UnitSelectorButton(
                                text = "min",
                                selected = !unitByHours,
                                onClick = { 
                                    unitByHours = false 
                                    // Mantém o valor atual em minutos
                                }
                            )
                            UnitSelectorButton(
                                text = "h",
                                selected = unitByHours,
                                onClick = { 
                                    unitByHours = true 
                                    // Valor atual será interpretado como horas no próximo onUpdate
                                    val num = text.toIntOrNull() ?: 0
                                    onUpdate(lembrete.copy(minutosAntes = num * 60))
                                }
                            )
                        }
                        
                        Text(
                            " antes", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                } else {
                    val time = lembrete.horarioAbsoluto?.toLocalTime() ?: LocalTime.NOON
                    val date = lembrete.horarioAbsoluto?.toLocalDate() ?: LocalDate.now()
                    
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("às", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Text(
                    text = if (lembrete.metodo == MetodoNotificacao.ALARME) "Com Alarme" else "Notificação",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lembrete.metodo == MetodoNotificacao.ALARME) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UnitSelectorButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ReminderTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun Modifier.minWidth(width: androidx.compose.ui.unit.Dp) = this.defaultMinSize(minWidth = width)
