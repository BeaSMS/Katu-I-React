package com.katui.offline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Alarme
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.viewmodel.AlarmViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val alarmes by viewModel.filteredAlarmes.collectAsState()
    val allAlarmes by viewModel.alarmes.collectAsState()
    val medicamentos by viewModel.medicamentos.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val estoqueBaixoAlerta by viewModel.estoqueBaixoAlerta.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var alarmeParaEditar by remember { mutableStateOf<Alarme?>(null) }

    val (tomados, naoTomados) = alarmes.partition { it.tomado }

    estoqueBaixoAlerta?.let { alerta ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissEstoqueAlerta() },
            icon = { Icon(Icons.Default.AlarmOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Estoque Baixo!") },
            text = {
                Text(
                    "Restam apenas ${alerta.estoqueRestante} ${alerta.unidade} de ${alerta.nomeMedicamento}.\n" +
                    "Lembre-se de repor o medicamento para não interromper o tratamento."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissEstoqueAlerta() }) { Text("Entendi") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (medicamentos.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDialog = true },
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                        text = { Text("Novo Alarme") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Alarme")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Título Integrado
            val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                "Alarmes de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
            } else {
                "Seus Alarmes"
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
                hasDataIndicator = { date ->
                    allAlarmes.any { it.horario.toLocalDate() == date }
                },
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (alarmes.isEmpty()) {
                    item {
                        EmptyAlarmsState(hasMedications = medicamentos.isNotEmpty())
                    }
                } else {
                    if (naoTomados.isNotEmpty()) {
                        item { SectionHeader(title = "Pendentes", icon = Icons.Default.Today) }
                        items(naoTomados.sortedBy { it.horario }) { alarme ->
                            AlarmItem(
                                alarme = alarme,
                                onToggle = { viewModel.toggleTomado(alarme) },
                                onDelete = { viewModel.deleteAlarme(alarme) },
                                onDeleteAllFuture = { viewModel.deleteAlarmeETodosFuturos(alarme) },
                                onEdit = { alarmeParaEditar = alarme }
                            )
                        }
                    }

                    if (tomados.isNotEmpty()) {
                        item { SectionHeader(title = "Concluídos", icon = Icons.Default.Check, color = MaterialTheme.colorScheme.secondary) }
                        items(tomados.sortedBy { it.horario }) { alarme ->
                            AlarmItem(
                                alarme = alarme,
                                onToggle = { viewModel.toggleTomado(alarme) },
                                onDelete = { viewModel.deleteAlarme(alarme) },
                                onDeleteAllFuture = { viewModel.deleteAlarmeETodosFuturos(alarme) },
                                onEdit = { alarmeParaEditar = alarme }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        if (showAddDialog) {
            AddAlarmDialog(
                medicamentos = medicamentos,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, time, medId ->
                    viewModel.addAlarme(title, time, medId)
                    showAddDialog = false
                }
            )
        }

        alarmeParaEditar?.let { alarme ->
            EditSingleAlarmDialog(
                alarme = alarme,
                onDismiss = { alarmeParaEditar = null },
                onConfirm = { newTitle, newTime ->
                    viewModel.updateSingleAlarme(alarme.id, newTitle, newTime)
                    alarmeParaEditar = null
                }
            )
        }
    }
}

@Composable
fun EditSingleAlarmDialog(
    alarme: Alarme,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDateTime) -> Unit
) {
    var titulo by remember { mutableStateOf(alarme.titulo) }
    var selectedTime by remember { mutableStateOf(alarme.horario.toLocalTime()) }
    val isMedicationAlarm = alarme.medicamentoId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Alarme") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (isMedicationAlarm) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Atenção: Você está editando apenas este horário específico. Para alterar todos os horários deste remédio, use a aba 'Remédios'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                TimeSelectorField(
                    label = "Novo Horário",
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDateTime = LocalDateTime.of(alarme.horario.toLocalDate(), selectedTime)
                    onConfirm(titulo, newDateTime)
                },
                enabled = titulo.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun LazyItemScope.EmptyAlarmsState(hasMedications: Boolean) {
    Box(
        modifier = Modifier
            .fillParentMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AlarmOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nenhum alarme para este dia.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (hasMedications) {
                Text(
                    "Você já tem remédios cadastrados! Os alarmes são gerados automaticamente ao adicionar um remédio, ou você pode criar um alarme manual agora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(
                    "Dica: Adicione seus remédios na aba 'Remédios' e os alarmes serão criados automaticamente para você.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AlarmItem(alarme: Alarme, onToggle: () -> Unit, onDelete: () -> Unit, onDeleteAllFuture: () -> Unit, onEdit: () -> Unit) {
    val isCustomAlarm = alarme.medicamentoId == null
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Excluir alarme") },
            text = {
                Text(
                    if (isCustomAlarm)
                        "Deseja excluir o alarme \"${alarme.titulo}\"? Esta ação não pode ser desfeita."
                    else
                        "Você quer excluir só este horário (${alarme.horario.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))}), ou todos os próximos alarmes deste remédio também?"
                )
            },
            confirmButton = {
                if (isCustomAlarm) {
                    Button(
                        onClick = { onDelete(); showConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Excluir") }
                } else {
                    Button(
                        onClick = { onDeleteAllFuture(); showConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Todos os futuros") }
                }
            },
            dismissButton = {
                if (isCustomAlarm) {
                    TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
                } else {
                    Row {
                        TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
                        TextButton(onClick = { onDelete(); showConfirmDialog = false }) { Text("Só este") }
                    }
                }
            }
        )
    }

    // Alarme avulso usa a cor secondary; alarme de remédio usa primary
    val accentColor = if (isCustomAlarm)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarme.tomado)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarme.tomado) 0.dp else 2.dp),
        border = if (alarme.tomado) null else BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone que diferencia o tipo de alarme
            Icon(
                imageVector = if (isCustomAlarm) Icons.Default.Alarm else Icons.Default.MedicalServices,
                contentDescription = if (isCustomAlarm) "Alarme avulso" else "Alarme de remédio",
                tint = if (alarme.tomado) MaterialTheme.colorScheme.outline else accentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarme.horario.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (alarme.tomado) MaterialTheme.colorScheme.outline else accentColor
                )
                Text(
                    text = alarme.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (alarme.tomado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                if (isCustomAlarm) {
                    Text(
                        text = "Alarme avulso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Row {
                // Botão Editar
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar alarme específico",
                        tint = if (alarme.tomado) MaterialTheme.colorScheme.outline else accentColor
                    )
                }

                // Alarme avulso não tem ação "Marcar como tomado"
                if (!isCustomAlarm) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Marcar como tomado",
                            tint = if (alarme.tomado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = { showConfirmDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    medicamentos: List<Medicamento>,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDateTime, Long?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(LocalTime.now().plusMinutes(5)) }
    var selectedMedId by remember { mutableStateOf<Long?>(null) }
    var expandedMedication by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Alarme") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (medicamentos.isNotEmpty()) {
                    Text("Vincular a um remédio (Opcional)", style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedTextField(
                            value = medicamentos.find { it.id == selectedMedId }?.nome ?: "Nenhum (alarme avulso)",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedMedication = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedMedication,
                            onDismissRequest = { expandedMedication = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nenhum (alarme avulso)") },
                                onClick = { selectedMedId = null; expandedMedication = false }
                            )
                            medicamentos.forEach { med ->
                                DropdownMenuItem(
                                    text = { Text(med.nome) },
                                    onClick = {
                                        selectedMedId = med.id
                                        titulo = "Tomar ${med.nome}"
                                        expandedMedication = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título / Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: Reunião com o médico") }
                )

                TimeSelectorField(
                    label = "Horário do Alarme",
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var alarmDateTime = LocalDateTime.of(LocalDateTime.now().toLocalDate(), selectedTime)
                    if (alarmDateTime.isBefore(LocalDateTime.now())) {
                        alarmDateTime = alarmDateTime.plusDays(1)
                    }
                    onConfirm(titulo, alarmDateTime, selectedMedId)
                },
                enabled = titulo.isNotBlank()
            ) { Text("Agendar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
