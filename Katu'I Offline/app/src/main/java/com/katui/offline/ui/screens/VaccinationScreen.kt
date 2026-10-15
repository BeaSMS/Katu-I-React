package com.katui.offline.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Vacina
import com.katui.offline.ui.components.DateSelectorField
import com.katui.offline.ui.viewmodel.VaccinationViewModel
import com.katui.offline.util.VacinaScheduleHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Estrutura para exibição — agrupa doses de uma mesma vacina
// ─────────────────────────────────────────────────────────────────────────────

internal sealed class VacinaItem {
    /** Vacina sem grupo — exibida como card simples (comportamento original) */
    data class Simples(val vacina: Vacina) : VacinaItem()
    /** Grupo de doses de uma mesma vacina (COVID-19, Hepatite B, etc.) */
    data class Grupo(val nomeGrupo: String, val doses: List<Vacina>) : VacinaItem()
}

private fun agruparVacinas(vacinas: List<Vacina>): List<VacinaItem> {
    val grupos = mutableMapOf<String, MutableList<Vacina>>()
    val simples = mutableListOf<Vacina>()

    for (v in vacinas) {
        val grupo = v.grupoVacina
        if (grupo != null) {
            grupos.getOrPut(grupo) { mutableListOf() }.add(v)
        } else {
            simples.add(v)
        }
    }

    val resultado = mutableListOf<VacinaItem>()
    // Grupos primeiro (já ordenados pela query do DAO)
    grupos.forEach { (nome, doses) ->
        resultado.add(VacinaItem.Grupo(nome, doses.sortedBy { it.ordemDose }))
    }
    // Depois as simples
    simples.forEach { resultado.add(VacinaItem.Simples(it)) }
    return resultado
}

// ─────────────────────────────────────────────────────────────────────────────
// Tela principal
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationScreen(viewModel: VaccinationViewModel = hiltViewModel()) {
    val vacinas by viewModel.vacinas.collectAsState()
    val usuario by viewModel.usuario.collectAsState()

    var vacinaParaEditar by remember { mutableStateOf<Vacina?>(null) }
    var mostrarDialogoManual by remember { mutableStateOf(false) }
    var grupoParaNovasDoses by remember { mutableStateOf<VacinaItem.Grupo?>(null) }

    // Ajuda
    var helpTitle by remember { mutableStateOf<String?>(null) }
    var helpMessage by remember { mutableStateOf<String?>(null) }

    if (helpTitle != null && helpMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                helpTitle = null 
                helpMessage = null 
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(helpTitle!!)
                }
            },
            text = { Text(helpMessage!!) },
            confirmButton = {
                TextButton(onClick = { 
                    helpTitle = null 
                    helpMessage = null 
                }) {
                    Text("Entendi")
                }
            }
        )
    }

    val idade = usuario?.idade ?: 0
    val categoriaFoco = when {
        idade >= 60 -> "Idoso"; idade >= 20 -> "Adulto"; idade >= 11 -> "Adolescente"; else -> "Criança"
    }

    val categoriasOrdenadas = remember(categoriaFoco) {
        val base = listOf("Anual", "Idoso", "Adulto", "Adolescente", "Criança")
        val idx = base.indexOf(categoriaFoco)
        if (idx != -1) listOf(base[idx]) + base.filterIndexed { i, _ -> i != idx } else base
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoManual = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Vacina")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Título Integrado
                Text(
                    text = "Vacinas e Reforços",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Acompanhe suas vacinas e reforços.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp).weight(1f)
                    )
                    IconButton(onClick = { 
                        helpTitle = "Calendário de Vacinação"
                        helpMessage = "O Katu'I agrupa vacinas com múltiplas doses (como COVID-19 ou Hepatite B) em um único card.\n\nVacinas recorrentes (como a da Gripe) mostram automaticamente a sugestão da próxima dose com base na data da última tomada."
                    }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            val todasCategorias = (categoriasOrdenadas + "Outras").distinct()

            todasCategorias.forEach { categoria ->
                val vacinasDaCategoria = vacinas.filter { it.categoria == categoria }
                if (vacinasDaCategoria.isEmpty()) return@forEach

                item {
                    Text(
                        text = when (categoria) {
                            categoriaFoco -> "$categoria (Sua Categoria)"
                            "Anual" -> "Anuais e Recorrentes"
                            "Outras" -> "Outras Vacinas"
                            else -> categoria
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (categoria == categoriaFoco || categoria == "Anual")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val itens = agruparVacinas(vacinasDaCategoria)
                items(itens) { item ->
                    when (item) {
                        is VacinaItem.Simples -> VaccineItem(
                            vacina = item.vacina,
                            onToggle = { viewModel.toggleVacina(item.vacina) },
                            onEditDate = { vacinaParaEditar = item.vacina }
                        )
                        is VacinaItem.Grupo -> VaccineGroupCard(
                            grupo = item,
                            onToggle = { viewModel.toggleVacina(it) },
                            onEditDate = { vacinaParaEditar = it },
                            onDelete = { viewModel.deletarVacina(it) },
                            onAdicionarDose = { grupoParaNovasDoses = item }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Dialogs
        if (mostrarDialogoManual) {
            AddVacinaManualDialog(
                onDismiss = { mostrarDialogoManual = false },
                onConfirm = { nome, data, obs ->
                    viewModel.adicionarVacinaManual(nome, data, obs)
                    mostrarDialogoManual = false
                }
            )
        }

        vacinaParaEditar?.let { vacina ->
            DatePickerDialog(
                onDismiss = { vacinaParaEditar = null },
                onConfirm = { date ->
                    viewModel.registrarVacinaComData(vacina, date)
                    vacinaParaEditar = null
                }
            )
        }

        grupoParaNovasDoses?.let { grupo ->
            AdicionarDoseDialog(
                nomeGrupo = grupo.nomeGrupo,
                proximaOrdem = (grupo.doses.maxOfOrNull { it.ordemDose } ?: 0) + 1,
                // Herda recorrência/intervalo da última dose do grupo que seja recorrente
                templateRecorrente = grupo.doses.lastOrNull { it.recorrente },
                onDismiss = { grupoParaNovasDoses = null },
                onConfirm = { data, obs ->
                    val ultima = grupo.doses.lastOrNull()
                    viewModel.adicionarDoseAoGrupo(
                        grupoVacina = grupo.nomeGrupo,
                        categoria = ultima?.categoria ?: "Outras",
                        data = data,
                        observacao = obs,
                        recorrente = ultima?.recorrente ?: false,
                        mesesParaReforco = ultima?.mesesParaReforco,
                        mesCampanha = ultima?.mesCampanha
                    )
                    grupoParaNovasDoses = null
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card agrupado (ex: COVID-19 com todas as doses)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun VaccineGroupCard(
    grupo: VacinaItem.Grupo,
    onToggle: (Vacina) -> Unit,
    onEditDate: (Vacina) -> Unit,
    onDelete: (Vacina) -> Unit,
    onAdicionarDose: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val tomadasCount = grupo.doses.count { it.tomada }
    val totalCount = grupo.doses.size
    val allDone = tomadasCount == totalCount
    val algumaEmAtraso = grupo.doses.any { v ->
        v.tomada && VacinaScheduleHelper.proximaDoseLabel(v)?.startsWith("Dose em atraso") == true
    }

    // Última dose tomada (para mostrar próxima dose do grupo)
    val ultimaTomadaRecorrente = grupo.doses
        .filter { it.tomada && it.recorrente }
        .maxByOrNull { it.ordemDose }

    val proximaLabel = ultimaTomadaRecorrente?.let {
        VacinaScheduleHelper.proximaDoseLabel(it)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                algumaEmAtraso -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                allDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            algumaEmAtraso -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            !allDone -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Cabeçalho do grupo ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Vaccines,
                    contentDescription = null,
                    tint = if (algumaEmAtraso) MaterialTheme.colorScheme.error
                    else if (allDone) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grupo.nomeGrupo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$tomadasCount de $totalCount doses registradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // ── Chip próxima dose (visível mesmo recolhido) ───────────────────
            if (proximaLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ProximaDoseChip(label = proximaLabel)
            }

            // ── Lista de doses (expansível) ───────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    grupo.doses.forEach { dose ->
                        DoseRow(
                            dose = dose,
                            onToggle = { onToggle(dose) },
                            onEditDate = { onEditDate(dose) },
                            onDelete = { onDelete(dose) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Botão "Adicionar dose" apenas se o grupo permitir
                    val grupoPermite = grupo.doses.any { it.permiteNovasDoses }
                    if (grupoPermite) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onAdicionarDose,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar nova dose")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Linha de dose individual dentro do card agrupado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DoseRow(dose: Vacina, onToggle: () -> Unit, onEditDate: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remover dose?") },
            text = { Text("Remover \"${dose.dose}\" de ${dose.nome}?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); confirmDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }

    Surface(
        color = if (dose.tomada)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditDate() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.dose,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (dose.tomada && dose.dataTomada != null) {
                    Text(
                        text = dose.dataTomada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (!dose.tomada) {
                    Text(
                        text = dose.observacao ?: "Toque para registrar data",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
                if (dose.tomada && dose.observacao != null) {
                    Text(
                        text = dose.observacao,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Botão deletar (apenas para doses adicionadas manualmente, ou seja ordemDose > 5 no COVID)
            if (dose.permiteNovasDoses || dose.categoria == "Outras") {
                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover dose",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp))
                }
            }

            IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (dose.tomada) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (dose.tomada) "Desmarcar" else "Marcar",
                    tint = if (dose.tomada) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card simples (vacinas sem grupo)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaccineItem(vacina: Vacina, onToggle: () -> Unit, onEditDate: () -> Unit) {
    val proximaDoseLabel = remember(vacina.tomada, vacina.dataTomada, vacina.recorrente) {
        if (vacina.tomada) VacinaScheduleHelper.proximaDoseLabel(vacina) else null
    }
    val emAtraso = proximaDoseLabel?.startsWith("Dose em atraso") == true

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEditDate() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                emAtraso -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                vacina.tomada -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            emAtraso -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            !vacina.tomada -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Vaccines,
                    contentDescription = null,
                    tint = when {
                        emAtraso -> MaterialTheme.colorScheme.error
                        vacina.tomada -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(vacina.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        if (vacina.tomada) "REGISTRADA" else vacina.dose,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vacina.tomada) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    vacina.observacao?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (vacina.tomada && vacina.dataTomada != null) {
                        Text(
                            "Tomada em: ${vacina.dataTomada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (!vacina.tomada) {
                        Text("Toque para registrar data",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (vacina.tomada) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (vacina.tomada) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (proximaDoseLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ProximaDoseChip(label = proximaDoseLabel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip reutilizável de próxima dose
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProximaDoseChip(label: String) {
    val emAtraso = label.startsWith("Dose em atraso")
    Surface(
        color = if (emAtraso) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (emAtraso) Icons.Default.Warning else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (emAtraso) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (emAtraso) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddVacinaManualDialog(onDismiss: () -> Unit, onConfirm: (String, LocalDate, String?) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now()) }
    var observacao by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Vacina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome, 
                    onValueChange = { nome = it },
                    label = { Text("Nome da Vacina") }, 
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Vaccines, contentDescription = null) }
                )
                DateSelectorField(
                    label = "Data que foi tomada", 
                    selectedDate = data,
                    onDateSelected = { data = it },
                    maxDate = LocalDate.now()
                )
                OutlinedTextField(
                    value = observacao, 
                    onValueChange = { observacao = it },
                    label = { Text("Observações (Opcional)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nome.isNotBlank()) onConfirm(nome, data, observacao.ifBlank { null }) },
                enabled = nome.isNotBlank()
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AdicionarDoseDialog(
    nomeGrupo: String,
    proximaOrdem: Int,
    templateRecorrente: Vacina?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, String?) -> Unit
) {
    var dataTomada by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var semData by remember { mutableStateOf(false) }
    var observacao by remember { mutableStateOf("") }

    val labelDose = when (proximaOrdem) {
        1 -> "1ª Dose"; 2 -> "2ª Dose"; 3 -> "3ª Dose"; 4 -> "4ª Dose"; 5 -> "5ª Dose"
        else -> "${proximaOrdem}ª Dose"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova dose — $nomeGrupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Adicionando: $labelDose",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)

                if (nomeGrupo == "COVID-19") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "Grupos prioritários: 1 dose/ano (mín. 3 meses da última).\n" +
                            "Idosos ≥60, imunocomprometidos, gestantes e puérperas: 2 doses/ano com intervalo de 6 meses.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = semData, onCheckedChange = { semData = it })
                    Text("Não sei a data exata", style = MaterialTheme.typography.bodyMedium)
                }

                if (!semData) {
                    DateSelectorField(
                        label = "Data que foi tomada",
                        selectedDate = dataTomada ?: LocalDate.now(),
                        onDateSelected = { dataTomada = it },
                        maxDate = LocalDate.now()
                    )
                }

                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it },
                    label = { Text("Observações (lote, local, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(if (semData) null else dataTomada, observacao.ifBlank { null })
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onConfirm(java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.of("UTC")).toLocalDate())
                }
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state = datePickerState) }
}
