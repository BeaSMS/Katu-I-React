package com.katui.offline.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.data.local.entities.MedicamentoCatalogo
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.MedicationViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onNavigateToImport: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val medicamentos by viewModel.filteredMedicamentos.collectAsState()
    val allMedicamentos by viewModel.medicamentos.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var medicamentoParaEditar by remember { mutableStateOf<Medicamento?>(null) }

    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (fabExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), shadowElevation = 4.dp) {
                            Text("Escanear Receita", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        SmallFloatingActionButton(onClick = { fabExpanded = false; onNavigateToImport() }, containerColor = AccentBlue, contentColor = Color.White, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), shadowElevation = 4.dp) {
                            Text("Adicionar Manual", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        SmallFloatingActionButton(onClick = { fabExpanded = false; showAddDialog = true }, containerColor = AccentTeal, contentColor = Color.White, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                "Remédios de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
            } else {
                "Seus Remédios"
            }
            Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 16.dp, end = 16.dp), textAlign = TextAlign.Center)

            MonthlyCalendar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                hasDataIndicator = { date ->
                    allMedicamentos.any { med ->
                        val start = med.dataInicio
                        val end = med.dataFim
                        (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
                    }
                },
                modifier = Modifier.padding(16.dp)
            )

            if (medicamentos.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum medicamento ativo", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Adicione manualmente ou use a IA para importar de uma foto.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(medicamentos) { med ->
                        MedicationItem(med, onDelete = { viewModel.deleteMedicamento(med) }, onClick = { medicamentoParaEditar = med })
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMedicationDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { m, g -> viewModel.addMedicamento(m, g); showAddDialog = false },
                searchResults = searchResults,
                onSearch = { viewModel.setSearchQuery(it) },
                viewModel = viewModel
            )
        }

        medicamentoParaEditar?.let { med ->
            EditMedicationDialog(
                medicamento = med,
                onDismiss = { medicamentoParaEditar = null },
                onConfirm = { m, r -> viewModel.updateMedicamento(m, r); medicamentoParaEditar = null },
                searchResults = searchResults,
                onSearch = { viewModel.setSearchQuery(it) }
            )
        }
    }
}

@Composable
fun MedicationItem(medicamento: Medicamento, onDelete: () -> Unit, onClick: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val isIntegrated = medicamento.finalidade == "Anticoncepcional"

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Excluir medicamento?") },
            text = { Text("Deseja excluir \"${medicamento.nome}\"?") },
            confirmButton = { Button(onClick = { onDelete(); showConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (isIntegrated) Icon(Icons.Default.Female, null, Modifier.size(24.dp), AccentPink)
                    else Icon(Icons.Default.Medication, null, Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(medicamento.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isIntegrated) AccentPink else MaterialTheme.colorScheme.primary)
                }
                if (!isIntegrated) {
                    IconButton(onClick = { showConfirmDialog = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                } else {
                    Surface(color = AccentPink.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("Saúde Feminina", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = AccentPink, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("Horário: ${medicamento.horario}", style = MaterialTheme.typography.bodyMedium)
            if (!isIntegrated) {
                if (medicamento.estoqueAtual <= medicamento.estoqueMinimo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, Modifier.size(16.dp), AccentOrange)
                        Spacer(Modifier.width(4.dp))
                        Text("Estoque Crítico: ${medicamento.estoqueAtual} ${medicamento.unidadeMedida}", style = MaterialTheme.typography.bodySmall, color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Estoque: ${medicamento.estoqueAtual} ${medicamento.unidadeMedida}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            medicamento.dosagem?.let { if (it.isNotBlank()) Text("Dosagem: $it", style = MaterialTheme.typography.bodySmall) }
            val freq = when (medicamento.tipoFrequencia) {
                "INTERVALO_HORAS" -> "A cada ${medicamento.valorFrequencia} horas"
                "VEZES_DIA" -> "${medicamento.valorFrequencia} vezes ao dia"
                else -> medicamento.tipoFrequencia
            }
            Text("Frequência: $freq", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EditMedicationDialog(medicamento: Medicamento, onDismiss: () -> Unit, onConfirm: (Medicamento, Boolean) -> Unit, searchResults: List<MedicamentoCatalogo>, onSearch: (String) -> Unit) {
    var nome by remember { mutableStateOf(medicamento.nome) }
    var catalogoId by remember { mutableStateOf(medicamento.catalogoId) }
    var selectedTime by remember { mutableStateOf(try { LocalTime.parse(medicamento.horario) } catch (e: Exception) { LocalTime.of(8, 0) }) }
    var dosagem by remember { mutableStateOf(medicamento.dosagem ?: "") }
    var finalidade by remember { mutableStateOf(medicamento.finalidade ?: "") }
    var tipoFrequencia by remember { mutableStateOf(medicamento.tipoFrequencia) }
    var valorFrequencia by remember { mutableStateOf(medicamento.valorFrequencia?.toString() ?: "8") }
    var usoContinuo by remember { mutableStateOf((medicamento.dias ?: 0) <= 0) }
    var dias by remember { mutableStateOf((medicamento.dias?.takeIf { it > 0 } ?: 7).toString()) }
    var regenerateAlarms by remember { mutableStateOf(true) }
    var estoqueAtual by remember { mutableStateOf(medicamento.estoqueAtual.toString()) }
    var estoqueMinimo by remember { mutableStateOf(medicamento.estoqueMinimo.toString()) }
    var dosePorTomada by remember { mutableStateOf(medicamento.dosePorTomada.toString()) }
    var unidadeMedida by remember { mutableStateOf(medicamento.unidadeMedida) }

    val isIntegrated = medicamento.finalidade == "Anticoncepcional"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isIntegrated) Icons.Default.Female else Icons.Default.Edit, null, tint = if (isIntegrated) AccentPink else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(if (isIntegrated) "Anticoncepcional" else "Editar Medicamento")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isIntegrated) {
                    Surface(color = AccentPink.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Este medicamento está integrado com a Saúde Feminina. Alterações devem ser feitas por lá.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = AccentPink)
                    }
                }
                AutocompleteField(value = nome, onValueChange = { nome = it; onSearch(it) }, label = "Nome do Remédio", suggestions = searchResults, enabled = !isIntegrated, onSuggestionSelected = { nome = it.nome; catalogoId = it.id; finalidade = it.classeTerapeutica ?: "" })
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = dosagem, onValueChange = { dosagem = it }, label = { Text("Dosagem") }, modifier = Modifier.fillMaxWidth(), enabled = !isIntegrated)
                OutlinedTextField(value = finalidade, onValueChange = { finalidade = it }, label = { Text("Finalidade") }, modifier = Modifier.fillMaxWidth(), enabled = !isIntegrated)
                Spacer(Modifier.height(16.dp))
                TimeSelectorField(label = if (isIntegrated) "Horário de Tomada" else "Horário Inicial", selectedTime = selectedTime, onTimeSelected = { selectedTime = it })
                
                if (!isIntegrated) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Uso contínuo", fontWeight = FontWeight.Bold)
                            Text("Duração baseada no estoque", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(usoContinuo, { usoContinuo = it })
                    }
                    if (!usoContinuo) OutlinedTextField(value = dias, onValueChange = { dias = it }, label = { Text("Duração (dias)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = estoqueAtual, onValueChange = { estoqueAtual = it }, label = { Text("Quantidade") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = unidadeMedida, onValueChange = { unidadeMedida = it }, label = { Text("Unidade") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val diasInt = if (usoContinuo) 0 else (dias.toIntOrNull() ?: 7)
                onConfirm(medicamento.copy(nome = nome, catalogoId = catalogoId, horario = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")), dosagem = dosagem, finalidade = finalidade, dias = diasInt, dataInicio = LocalDate.now(), dataFim = if (diasInt > 0) LocalDate.now().plusDays(diasInt.toLong()) else null, estoqueAtual = estoqueAtual.toDoubleOrNull() ?: 0.0, estoqueMinimo = estoqueMinimo.toDoubleOrNull() ?: 0.0, dosePorTomada = dosePorTomada.toDoubleOrNull() ?: 1.0, unidadeMedida = unidadeMedida), regenerateAlarms)
            }, modifier = Modifier.fillMaxWidth()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
    )
}

@Composable
fun AutocompleteField(value: String, onValueChange: (String) -> Unit, label: String, suggestions: List<MedicamentoCatalogo>, onSuggestionSelected: (MedicamentoCatalogo) -> Unit, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = { onValueChange(it); expanded = true }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
        if (expanded && suggestions.isNotEmpty()) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = false), modifier = Modifier.fillMaxWidth(0.9f)) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(text = { Column { Text(suggestion.nome, fontWeight = FontWeight.Bold); Text(suggestion.principioAtivo, style = MaterialTheme.typography.labelSmall) } }, onClick = { onSuggestionSelected(suggestion); expanded = false })
                }
            }
        }
    }
}

@Composable
fun AddMedicationDialog(onDismiss: () -> Unit, onConfirm: (Medicamento, Boolean) -> Unit, searchResults: List<MedicamentoCatalogo>, onSearch: (String) -> Unit, viewModel: MedicationViewModel = hiltViewModel()) {
    var nome by remember { mutableStateOf("") }
    var catalogoId by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var dosagem by remember { mutableStateOf("") }
    var finalidade by remember { mutableStateOf("") }
    var tipoFrequencia by remember { mutableStateOf("INTERVALO_HORAS") }
    var valorFrequencia by remember { mutableStateOf("8") }
    var usoContinuo by remember { mutableStateOf(false) }
    var dias by remember { mutableStateOf("7") }
    var generateAlarms by remember { mutableStateOf(true) }
    var estoqueAtual by remember { mutableStateOf("30") }
    var estoqueMinimo by remember { mutableStateOf("5") }
    var dosePorTomada by remember { mutableStateOf("1") }
    var unidadeMedida by remember { mutableStateOf("un") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Medicamento") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AutocompleteField(value = nome, onValueChange = { nome = it; onSearch(it) }, label = "Nome do Remédio", suggestions = searchResults, onSuggestionSelected = { nome = it.nome; catalogoId = it.id; finalidade = it.classeTerapeutica ?: ""; unidadeMedida = if (it.formaFarmaceutica?.contains("Gotas", ignoreCase = true) == true) "gotas" else if (it.formaFarmaceutica?.contains("Xarope", ignoreCase = true) == true) "ml" else "un" })
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = dosagem, onValueChange = { dosagem = it }, label = { Text("Dosagem") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = finalidade, onValueChange = { finalidade = it }, label = { Text("Finalidade") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                TimeSelectorField(label = "Horário Inicial", selectedTime = selectedTime, onTimeSelected = { selectedTime = it })
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Uso contínuo", fontWeight = FontWeight.Bold)
                        Text("Duração baseada no estoque", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(usoContinuo, { usoContinuo = it })
                }
                if (!usoContinuo) OutlinedTextField(value = dias, onValueChange = { dias = it }, label = { Text("Duração (dias)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = estoqueAtual, onValueChange = { estoqueAtual = it }, label = { Text("Quantidade") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = unidadeMedida, onValueChange = { unidadeMedida = it }, label = { Text("Unidade") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            val userState by viewModel.usuario.collectAsState()
            Button(onClick = {
                val diasInt = if (usoContinuo) 0 else (dias.toIntOrNull() ?: 1)
                onConfirm(Medicamento(nome = nome, catalogoId = catalogoId, horario = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")), dosagem = dosagem, usuarioId = userState?.id ?: 0L, tipoFrequencia = tipoFrequencia, valorFrequencia = valorFrequencia.toIntOrNull(), dias = diasInt, finalidade = finalidade, dataInicio = LocalDate.now(), dataFim = if (diasInt > 0) LocalDate.now().plusDays(diasInt.toLong()) else null, observacoes = null, estoqueAtual = estoqueAtual.toDoubleOrNull() ?: 0.0, estoqueMinimo = estoqueMinimo.toDoubleOrNull() ?: 0.0, dosePorTomada = dosePorTomada.toDoubleOrNull() ?: 1.0, unidadeMedida = unidadeMedida), generateAlarms)
            }, enabled = nome.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") } }
    )
}
