package com.katui.offline.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.katui.offline.data.local.entities.Medicamento
import com.katui.offline.ui.components.DateSelectorField
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.viewmodel.MedicationViewModel
import com.katui.offline.util.PdfUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionImportScreen(
    onImportComplete: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val extractedMeds by viewModel.extractedMeds.collectAsState()
    
    var isExtracting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    var dataReceita by remember { mutableStateOf(LocalDate.now()) }
    var observacao by remember { mutableStateOf("") }
    var useAI by remember { mutableStateOf(true) }

    val saveStatus by viewModel.saveStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveStatus) {
        saveStatus?.let { result ->
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("Receita e medicamentos salvos com sucesso!")
                viewModel.clearExtractedMeds()
                onImportComplete()
            } else {
                errorMessage = "Erro ao salvar: ${result.exceptionOrNull()?.message}"
                isSaving = false
            }
            viewModel.resetSaveStatus()
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedImageUri = uri
            errorMessage = null
            viewModel.clearExtractedMeds()
        }
    )

    editingIndex?.let { index ->
        val medAtual = extractedMeds.getOrNull(index)
        if (medAtual != null) {
            EditMedicamentoDialog(
                medicamento = medAtual,
                onDismiss = { editingIndex = null },
                onConfirm = { medEditado ->
                    viewModel.updateExtractedMed(index, medEditado)
                    editingIndex = null
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título Integrado
            Text(
                text = "Importar Receita",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Selecione a receita (Foto ou PDF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedImageUri != null) {
                        val isPdf = PdfUtils.isPdf(context, selectedImageUri!!)
                        if (isPdf) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.AutoMirrored.Filled.Note, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
                                    Text("Documento PDF selecionado", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { pickerLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Mudar Arquivo")
                        }
                    } else {
                        Button(onClick = { pickerLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Selecionar Foto ou PDF")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Informações Gerais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    DateSelectorField(label = "Data da Receita", selectedDate = dataReceita, onDateSelected = { dataReceita = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = observacao, 
                        onValueChange = { observacao = it }, 
                        label = { Text("Observações") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        minLines = 2,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usar Inteligência Artificial?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = useAI, onCheckedChange = { useAI = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Configuração de IA Necessária") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (errorMessage!!.contains("Chave", ignoreCase = true)) 
                                    "Para ler receitas automaticamente, você precisa configurar sua própria chave da IA."
                                else errorMessage!!,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Por que isso? O Katu'I é focado em PRIVACIDADE. Não enviamos seus dados para nossos servidores porque não temos servidores! O app funciona totalmente offline no seu celular. Para usar a IA, você se conecta diretamente ao Google usando sua chave pessoal, garantindo que ninguém além de você e do Google tenha acesso às imagens.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        if (errorMessage!!.contains("Chave", ignoreCase = true)) {
                            Button(onClick = { errorMessage = null; onNavigateToSettings() }) {
                                Text("Configurar Agora")
                            }
                        } else {
                            Button(onClick = { errorMessage = null }) { Text("Entendi") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { errorMessage = null }) { Text("Fechar") }
                    }
                )
            }

            if (isExtracting) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("A IA está analisando sua receita...", textAlign = TextAlign.Center)
            } else if (extractedMeds.isEmpty()) {
                Button(
                    onClick = {
                        if (selectedImageUri == null) { errorMessage = "Selecione uma imagem."; return@Button }
                        if (useAI) {
                            scope.launch {
                                isExtracting = true
                                errorMessage = null
                                try {
                                    val result = viewModel.extractMedications(context, selectedImageUri!!)
                                    viewModel.setExtractedMeds(result)
                                    result.firstOrNull()?.observacoes?.let { obs ->
                                        if (obs.startsWith("Data Receita: ")) {
                                            try { dataReceita = LocalDate.parse(obs.substringAfter("Data Receita: ").substringBefore(". ")) } catch (e: Exception) {}
                                        }
                                    }
                                } catch (e: Exception) { errorMessage = e.message } finally { isExtracting = false }
                            }
                        } else {
                            isSaving = true
                            viewModel.saveImportedMedications(emptyList(), false, selectedImageUri, "Data: $dataReceita. $observacao")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedImageUri != null && !isSaving
                ) {
                    Text(if (useAI) "Analisar com IA" else "Salvar Receita")
                }
            }

            if (extractedMeds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Medicamentos Detectados:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

                extractedMeds.forEachIndexed { index, med ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !isSaving) { editingIndex = index }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(med.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Dose: ${med.dosePorTomada} ${med.unidadeMedida} | Horário: ${med.horario}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { editingIndex = index }, enabled = !isSaving) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    Text("Salvando medicamentos e alarmes...", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        isSaving = true
                        viewModel.saveImportedMedications(extractedMeds, true, selectedImageUri, "Data: $dataReceita. $observacao (IA)")
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    enabled = !isSaving && !isExtracting
                ) {
                    Text(if (isSaving) "Salvando..." else "Confirmar e Gerar Alarmes")
                }

                TextButton(onClick = { viewModel.clearExtractedMeds() }, enabled = !isSaving && !isExtracting) { Text("Tentar outra imagem") }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@Composable
fun EditMedicamentoDialog(medicamento: Medicamento, onDismiss: () -> Unit, onConfirm: (Medicamento) -> Unit) {
    var nome by remember { mutableStateOf(medicamento.nome) }
    var dosagem by remember { mutableStateOf(medicamento.dosagem ?: "") }
    var horario by remember { mutableStateOf(try { LocalTime.parse(medicamento.horario) } catch (e: Exception) { LocalTime.of(8,0) }) }
    var estoqueAtual by remember { mutableStateOf(medicamento.estoqueAtual.toString()) }
    var dosePorTomada by remember { mutableStateOf(medicamento.dosePorTomada.toString()) }
    var unidadeMedida by remember { mutableStateOf(medicamento.unidadeMedida) }
    
    // Novos campos para edição de frequência na revisão pós-IA
    var tipoFrequencia by remember { mutableStateOf(medicamento.tipoFrequencia) }
    var valorFrequencia by remember { mutableStateOf(medicamento.valorFrequencia?.toString() ?: "8") }

    // Uso contínuo: quando a receita não define um prazo de tratamento (comum em
    // medicação crônica), o usuário liga esse switch e a duração é calculada a partir
    // de quanto estoque ele tem e quantas doses toma por dia — em vez de pedir pra
    // digitar um número de dias "no chute".
    var usoContinuo by remember { mutableStateOf((medicamento.dias ?: 0) <= 0) }
    var dias by remember { mutableStateOf((medicamento.dias?.takeIf { it > 0 } ?: 7).toString()) }

    // Quantas doses por dia o medicamento prevê, usado tanto para o cálculo de
    // "dias restantes de estoque" quanto para a geração real dos alarmes.
    val vezesPorDia = when (tipoFrequencia) {
        "VEZES_DIA" -> (valorFrequencia.toIntOrNull() ?: 1).coerceAtLeast(1)
        "INTERVALO_HORAS" -> (24 / (valorFrequencia.toIntOrNull() ?: 8).coerceAtLeast(1)).coerceAtLeast(1)
        else -> 1
    }

    // Dias que o estoque atual cobre, dado o consumo diário (dosePorTomada x vezesPorDia).
    // Atualiza em tempo real conforme o usuário edita estoque ou dose.
    val diasCalculadosPeloEstoque by remember(estoqueAtual, dosePorTomada, vezesPorDia) {
        derivedStateOf {
            val estoqueNum = estoqueAtual.toDoubleOrNull() ?: 0.0
            val doseNum = dosePorTomada.toDoubleOrNull() ?: 1.0
            val consumoDiario = doseNum * vezesPorDia
            if (consumoDiario > 0.0) kotlin.math.ceil(estoqueNum / consumoDiario).toInt().coerceAtLeast(1) else 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Medicamento Detectado") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome, 
                    onValueChange = { nome = it }, 
                    label = { Text("Nome") }, 
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) }
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dosagem, 
                        onValueChange = { dosagem = it }, 
                        label = { Text("Dosagem (ex: 500mg)") }, 
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) }
                    )
                    TimeSelectorField(label = "Horário", selectedTime = horario, onTimeSelected = { horario = it }, modifier = Modifier.weight(1f))
                }

                // Edição de Frequência
                Text("Frequência", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tipoFrequencia == "INTERVALO_HORAS",
                        onClick = { tipoFrequencia = "INTERVALO_HORAS" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Intervalo") }
                    SegmentedButton(
                        selected = tipoFrequencia == "VEZES_DIA",
                        onClick = { tipoFrequencia = "VEZES_DIA" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Vezes/Dia") }
                }

                if (tipoFrequencia == "INTERVALO_HORAS") {
                    var expandedHours by remember { mutableStateOf(false) }
                    val hourOptions = listOf(1, 2, 4, 6, 8, 12, 24)
                    Box {
                        OutlinedTextField(
                            value = "A cada $valorFrequencia horas",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selecione o Intervalo") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { IconButton(onClick = { expandedHours = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = expandedHours, onDismissRequest = { expandedHours = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                            hourOptions.forEach { hour ->
                                DropdownMenuItem(text = { Text("A cada $hour horas") }, onClick = { valorFrequencia = hour.toString(); expandedHours = false })
                            }
                        }
                    }
                } else {
                    var expandedTimes by remember { mutableStateOf(false) }
                    val timesOptions = (1..12).toList()
                    Box {
                        OutlinedTextField(
                            value = "$valorFrequencia vezes ao dia",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vezes por dia") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { IconButton(onClick = { expandedTimes = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = expandedTimes, onDismissRequest = { expandedTimes = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                            timesOptions.forEach { times ->
                                DropdownMenuItem(text = { Text("$times vezes ao dia") }, onClick = { valorFrequencia = times.toString(); expandedTimes = false })
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = estoqueAtual, onValueChange = { estoqueAtual = it }, label = { Text("Estoque", maxLines = 1) }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                    OutlinedTextField(value = dosePorTomada, onValueChange = { dosePorTomada = it }, label = { Text("Dose", maxLines = 1) }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                }
                
                OutlinedTextField(value = unidadeMedida, onValueChange = { unidadeMedida = it }, label = { Text("Unidade (ml, un, gotas)", maxLines = 1) }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Uso contínuo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Duração calculada pelo estoque", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(checked = usoContinuo, onCheckedChange = { usoContinuo = it })
                }

                if (usoContinuo) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Com esse estoque, dura aproximadamente $diasCalculadosPeloEstoque dia(s) (baseado na dose e frequência informadas)",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = dias,
                        onValueChange = { dias = it },
                        label = { Text("Duração (dias)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val diasFinal = if (usoContinuo) diasCalculadosPeloEstoque else (dias.toIntOrNull() ?: 7)
                    onConfirm(
                        medicamento.copy(
                            nome = nome,
                            dosagem = dosagem,
                            horario = horario.format(DateTimeFormatter.ofPattern("HH:mm")),
                            tipoFrequencia = tipoFrequencia,
                            valorFrequencia = valorFrequencia.toIntOrNull(),
                            estoqueAtual = estoqueAtual.toDoubleOrNull() ?: 0.0,
                            dosePorTomada = dosePorTomada.toDoubleOrNull() ?: 1.0,
                            unidadeMedida = unidadeMedida,
                            dias = diasFinal,
                            dataInicio = LocalDate.now(),
                            dataFim = LocalDate.now().plusDays((diasFinal.toLong() - 1).coerceAtLeast(0))
                        )
                    )
                },
                enabled = nome.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
