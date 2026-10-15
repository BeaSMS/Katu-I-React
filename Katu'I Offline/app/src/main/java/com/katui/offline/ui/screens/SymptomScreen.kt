package com.katui.offline.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.R
import com.katui.offline.data.local.dao.MedicamentoCausador
import com.katui.offline.data.local.entities.Sintoma
import com.katui.offline.data.local.entities.SintomaCatalogo
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.TwoColumnGrid
import com.katui.offline.ui.viewmodel.SymptomViewModel
import com.katui.offline.util.UiTextHelper
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomScreen(
    viewModel: SymptomViewModel = hiltViewModel()
) {
    val sintomas by viewModel.filteredSintomas.collectAsState()
    val allSintomas by viewModel.sintomas.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val alertaEfeitoColateral by viewModel.alertaEfeitoColateral.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sintomaParaEditar by remember { mutableStateOf<Sintoma?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Físico, 1: Emocional

    val (fisicos, emocionais) = sintomas.partition { it.tipo == "FISICO" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar")
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
                "Sintomas de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
            } else {
                "Sintomas e Humor"
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
                    allSintomas.any { it.dataHoraRegistro.toLocalDate() == date }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sintomas Físicos") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Humor e Disposição") }
                )
            }

            val displayList = if (selectedTab == 0) fisicos else emocionais

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Thermostat else Icons.Default.Favorite, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp), 
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "Nenhum sintoma físico registrado" else "Como você está se sentindo hoje?\nNada registrado ainda.", 
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList) { sintoma ->
                        SymptomItem(
                            sintoma = sintoma,
                            onDelete = { viewModel.deleteSintoma(sintoma) },
                            onClick = { sintomaParaEditar = sintoma }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddSymptomDialog(
                initialType = if (selectedTab == 0) "FISICO" else "PSICOLOGICO",
                onDismiss = { 
                    showAddDialog = false 
                    viewModel.limparAlertaEfeitoColateral()
                },
                onConfirm = { newSintoma ->
                    viewModel.addSintoma(newSintoma)
                    showAddDialog = false
                    viewModel.limparAlertaEfeitoColateral()
                },
                searchResults = searchResults,
                onSearch = { viewModel.setSearchQuery(it) },
                alertaEfeitoColateral = alertaEfeitoColateral,
                onSintomaCatalogoSelecionado = { catalogoId -> viewModel.verificarEfeitoColateral(catalogoId) }
            )
        }

        sintomaParaEditar?.let { sintoma ->
            AddSymptomDialog(
                editingSintoma = sintoma,
                initialType = sintoma.tipo,
                onDismiss = {
                    sintomaParaEditar = null
                    viewModel.limparAlertaEfeitoColateral()
                },
                onConfirm = { updatedSintoma ->
                    viewModel.updateSintoma(updatedSintoma)
                    sintomaParaEditar = null
                    viewModel.limparAlertaEfeitoColateral()
                },
                searchResults = searchResults,
                onSearch = { viewModel.setSearchQuery(it) },
                alertaEfeitoColateral = alertaEfeitoColateral,
                onSintomaCatalogoSelecionado = { catalogoId -> viewModel.verificarEfeitoColateral(catalogoId) }
            )
        }
    }
}

@Composable
fun SymptomItem(sintoma: Sintoma, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val (clima, nuances) = remember(sintoma.qualidade) { parseMood(sintoma.qualidade) }

    val moodColor = when (clima) {
        "Positivo" -> Color(0xFF2E7D32)
        "Baixo" -> Color(0xFF1565C0)
        "Tenso" -> Color(0xFFC62828)
        "Neutro" -> Color(0xFF546E7A)
        else -> MaterialTheme.colorScheme.primary
    }

    val cardBgColor = if (sintoma.tipo == "PSICOLOGICO") moodColor.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Registro?") },
            text = { Text("Tem certeza que deseja excluir este registro? Esta ação não pode ser desfeita.") },
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
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(20.dp),
        border = if (sintoma.tipo == "PSICOLOGICO") {
            androidx.compose.foundation.BorderStroke(1.dp, moodColor.copy(alpha = 0.1f))
        } else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sintoma.tipo == "FISICO") Icons.Default.Thermostat else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = moodColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sintoma.dataHoraRegistro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IntensityBadge(sintoma.intensidadeEscala ?: 0, sintoma.tipo, clima)
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (sintoma.tipo == "PSICOLOGICO") {
                Text(
                    text = clima,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = moodColor
                )
                
                if (nuances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        nuances.forEach { nuance ->
                            Surface(
                                color = moodColor.copy(alpha = 0.1f),
                                contentColor = moodColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = nuance,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                sintoma.fatoresAssociados?.let { if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = moodColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = Color(0xFFFFA000))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gatilho: $it", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                } }
            } else {
                Text(
                    text = sintoma.qualidade ?: "Não especificado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                sintoma.localizacao?.let { if (it.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1E88E5)); Spacer(modifier = Modifier.width(4.dp)); Text(text = it, style = MaterialTheme.typography.bodyMedium) } }
                sintoma.padraoTempo?.let { if (it.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF8E24AA)); Spacer(modifier = Modifier.width(4.dp)); Text(text = it, style = MaterialTheme.typography.bodySmall) } }
                sintoma.fatoresAssociados?.let { if (it.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF00897B)); Spacer(modifier = Modifier.width(4.dp)); Text(text = it, style = MaterialTheme.typography.bodySmall) } }
                sintoma.impactoFuncional?.let { if (it.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE53935)); Spacer(modifier = Modifier.width(4.dp)); Text(text = it, style = MaterialTheme.typography.bodySmall) } }
                if (sintoma.incapacitante == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "INCAPACITANTE", 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntensityBadge(intensidade: Int, tipo: String = "FISICO", clima: String = "") {
    val color = when {
        tipo == "PSICOLOGICO" -> when (clima) {
            "Positivo" -> Color(0xFF4CAF50)
            "Baixo" -> Color(0xFF1E88E5)
            "Tenso" -> Color(0xFFE53935)
            "Neutro" -> Color(0xFF78909C)
            else -> Color(0xFFFFC107)
        }
        intensidade <= 3 -> Color(0xFF4CAF50)
        intensidade <= 7 -> Color(0xFFFFC107)
        else -> Color(0xFFE53935)
    }
    
    val icon = when {
        tipo == "FISICO" -> when {
            intensidade <= 3 -> Icons.Default.SentimentSatisfied
            intensidade <= 7 -> Icons.Default.SentimentNeutral
            else -> Icons.Default.SentimentVeryDissatisfied
        }
        else -> when (clima) {
            "Positivo" -> Icons.Default.WbSunny
            "Neutro" -> Icons.Default.WbCloudy
            "Baixo" -> Icons.Default.SentimentDissatisfied
            "Tenso" -> Icons.Default.Psychology
            else -> Icons.Default.WbCloudy
        }
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = intensidade.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun parseMood(qualidade: String?): Pair<String, List<String>> {
    if (qualidade == null) return "" to emptyList()
    val regex = Regex("\\[(.*?)\\](?: \\((.*?)\\))?")
    val match = regex.find(qualidade)
    val clima = match?.groups?.get(1)?.value ?: qualidade
    val nuances = match?.groups?.get(2)?.value?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()
    return clima to nuances
}

@Composable
fun SymptomAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<SintomaCatalogo>,
    onSuggestionSelected: (SintomaCatalogo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text("O que você está sentindo?") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex: Dor de cabeça, enjoo...") }
        )

        if (expanded && suggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(suggestion.nome)
                                if (suggestion.categoria != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            suggestion.categoria!!, 
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddSymptomDialog(
    editingSintoma: Sintoma? = null,
    initialType: String, 
    onDismiss: () -> Unit, 
    onConfirm: (Sintoma) -> Unit,
    searchResults: List<SintomaCatalogo>,
    onSearch: (String) -> Unit,
    alertaEfeitoColateral: List<MedicamentoCausador> = emptyList(),
    onSintomaCatalogoSelecionado: (Long?) -> Unit = {}
) {
    var tipo by remember { mutableStateOf(editingSintoma?.tipo ?: initialType) }
    var qualidade by remember { mutableStateOf(editingSintoma?.qualidade ?: "") }
    var catalogoId by remember { mutableStateOf(editingSintoma?.catalogoId) }
    var intensidade by remember { mutableFloatStateOf(editingSintoma?.intensidadeEscala?.toFloat() ?: 5f) }
    var localizacao by remember { mutableStateOf(editingSintoma?.localizacao ?: "") }
    var padraoTempo by remember { mutableStateOf(editingSintoma?.padraoTempo ?: "") }
    var fatoresAssociados by remember { mutableStateOf(editingSintoma?.fatoresAssociados ?: "") }
    var impactoFuncional by remember { mutableStateOf(editingSintoma?.impactoFuncional ?: "") }
    var incapacitante by remember { mutableStateOf(editingSintoma?.incapacitante ?: false) }

    // Estado para os diálogos de ajuda
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

    // Novas variáveis para o sistema de humor por camadas
    var climaSelecionado by remember { 
        mutableStateOf(
            when {
                qualidade.contains("Positivo") -> "Positivo"
                qualidade.contains("Neutro") -> "Neutro"
                qualidade.contains("Baixo") -> "Baixo"
                qualidade.contains("Tenso") -> "Tenso"
                else -> null
            }
        )
    }
    
    val tagsSelecionadas = remember { 
        mutableStateListOf<String>().apply {
            if (editingSintoma?.tipo == "PSICOLOGICO") {
                val base = qualidade.substringAfter("] ").split(", ").filter { it.isNotBlank() }
                addAll(base)
            }
        }
    }

    // Efeito colateral check se ja tiver catalogoId (ao editar)
    LaunchedEffect(editingSintoma) {
        if (editingSintoma?.catalogoId != null) {
            onSintomaCatalogoSelecionado(editingSintoma.catalogoId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSintoma != null) "Editar Registro" else if (tipo == "FISICO") "Registrar Sintoma Físico" else "Como você está se sentindo?") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                if (editingSintoma == null) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = tipo == "FISICO",
                            onClick = { tipo = "FISICO" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Físico")
                        }
                        SegmentedButton(
                            selected = tipo == "PSICOLOGICO",
                            onClick = { tipo = "PSICOLOGICO" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Humor")
                        }
                    }
                }

                if (tipo == "FISICO") {
                    SymptomAutocompleteField(
                        value = qualidade,
                        onValueChange = { 
                            qualidade = it 
                            onSearch(it)
                            catalogoId = null
                            onSintomaCatalogoSelecionado(null)
                        },
                        suggestions = searchResults,
                        onSuggestionSelected = {
                            qualidade = it.nome
                            catalogoId = it.id
                            onSintomaCatalogoSelecionado(it.id)
                        }
                    )

                    if (alertaEfeitoColateral.isNotEmpty()) {
                        EfeitoColateralAlertCard(alertaEfeitoColateral)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1E88E5))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Localização", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { 
                                    helpTitle = "Localização"
                                    helpMessage = "Indique onde você está sentindo o sintoma. Você pode escolher uma parte do corpo sugerida ou digitar livremente (ex: 'Lado esquerdo da cabeça')."
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        val bodyParts = listOf<Pair<String, Any>>(
                            "Geral" to (R.drawable.endocrine_system_4785730 as Any),
                            "Cabeça" to (Icons.Default.Face as Any),
                            "Costas" to (R.drawable.back_6969709 as Any),
                            "Peito" to (R.drawable.bra_2517200 as Any),
                            "Estômago" to (R.drawable.stomach_11468493 as Any),
                            "Braços" to (R.drawable.arm_9172477 as Any),
                            "Pernas" to (R.drawable.calf_9172603 as Any)
                        )
                        TwoColumnGrid(items = bodyParts) { (loc, icon) ->
                            FilterChip(
                                selected = localizacao == loc,
                                onClick = { localizacao = loc },
                                label = { Text(loc, maxLines = 1) },
                                leadingIcon = {
                                    when (icon) {
                                        is Int -> Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
                                        is androidx.compose.ui.graphics.vector.ImageVector -> Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        OutlinedTextField(
                            value = localizacao, 
                            onValueChange = { localizacao = it }, 
                            label = { Text("Ou digite o local...") }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1E88E5)) }
                        )
                    }

                    OutlinedTextField(value = padraoTempo, onValueChange = { padraoTempo = it }, label = { Text("Quando começou / Quanto dura?") }, leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF8E24AA)) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = fatoresAssociados, onValueChange = { fatoresAssociados = it }, label = { Text("O que piora ou melhora?") }, leadingIcon = { Icon(Icons.Default.ManageSearch, contentDescription = null, tint = Color(0xFF00897B)) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    OutlinedTextField(value = impactoFuncional, onValueChange = { impactoFuncional = it }, label = { Text("Como afeta seu dia?") }, leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFE53935)) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = incapacitante, onCheckedChange = { incapacitante = it })
                        Text("Isso te impede de fazer algo?")
                        IconButton(
                            onClick = { 
                                helpTitle = "Incapacitante"
                                helpMessage = "Marque esta opção se o sintoma for forte o suficiente para impedir você de realizar atividades normais (como trabalhar, estudar ou fazer tarefas domésticas)."
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    // SISTEMA DE HUMOR POR CAMADAS
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("O Clima Geral", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        IconButton(
                            onClick = { 
                                helpTitle = "Diário de Humor"
                                helpMessage = "Primeiro escolha o 'Clima' geral do seu dia. Depois, selecione as 'Nuances' para descrever melhor o que está sentindo."
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Positivo", "Neutro").forEach { clima ->
                                val isSelected = climaSelecionado == clima
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        climaSelecionado = clima
                                        tagsSelecionadas.clear()
                                    },
                                    label = { Text(clima) },
                                    leadingIcon = { Icon(if (clima == "Positivo") Icons.Default.WbSunny else Icons.Default.WbCloudy, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (clima == "Positivo") Color(0xFFFFA000) else Color(0xFF78909C)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Baixo", "Tenso").forEach { clima ->
                                val isSelected = climaSelecionado == clima
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        climaSelecionado = clima
                                        tagsSelecionadas.clear()
                                    },
                                    label = { Text(clima) },
                                    leadingIcon = { Icon(if (clima == "Baixo") Icons.Default.SentimentDissatisfied else Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (clima == "Baixo") Color(0xFF1E88E5) else Color(0xFFE53935)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (climaSelecionado != null) {
                        Text("Nuances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        val tags = when (climaSelecionado) {
                            "Positivo" -> listOf(
                                "Alegre" to Icons.Default.WbSunny,
                                "Calmo" to Icons.Default.Air,
                                "Motivado" to Icons.Default.Bolt,
                                "Produtivo" to Icons.Default.LocalFireDepartment,
                                "Grato" to Icons.Default.Favorite,
                                "Energizado" to Icons.Default.Bolt,
                                "Eufórico" to Icons.Default.LocalFireDepartment
                            )
                            "Neutro" -> listOf(
                                "Normal" to Icons.Default.Circle,
                                "Pensativo" to Icons.Default.AcUnit,
                                "Cansado" to Icons.Default.BatteryAlert,
                                "Apático" to Icons.Default.AcUnit
                            )
                            "Baixo" -> listOf(
                                "Triste" to Icons.Default.WbCloudy,
                                "Desanimado" to Icons.Default.BatteryAlert,
                                "Solitário" to Icons.Default.AcUnit,
                                "Sensível" to Icons.Default.WbCloudy,
                                "Sem energia" to Icons.Default.BatteryAlert,
                                "Depressivo" to Icons.Default.MoodBad
                            )
                            "Tenso" -> listOf(
                                "Ansioso" to Icons.Default.Thunderstorm,
                                "Irritado" to Icons.Default.Bolt,
                                "Estressado" to Icons.Default.Thunderstorm,
                                "Agitado" to Icons.Default.Air,
                                "Sob pressão" to Icons.Default.Thunderstorm,
                                "Confuso" to Icons.Default.Psychology
                            )
                            else -> emptyList()
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { (tag, icon) ->
                                val isSelected = tagsSelecionadas.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        if (isSelected) tagsSelecionadas.remove(tag) else tagsSelecionadas.add(tag)
                                    },
                                    label = { Text(tag) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fatoresAssociados, 
                        onValueChange = { fatoresAssociados = it }, 
                        label = { Text("Gatilhos ou Motivos") }, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Opcional: Por que se sente assim?") },
                        shape = MaterialTheme.shapes.medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                val intensityLabel = when {
                    intensidade <= 3 -> if (tipo == "FISICO") "Leve" else "Estável"
                    intensidade <= 7 -> if (tipo == "FISICO") "Moderada" else "Presente"
                    else -> if (tipo == "FISICO") "Intensa" else "Muito Forte"
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tipo == "FISICO") "Intensidade da dor" else "Força do sentimento",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(
                            onClick = { 
                                helpTitle = if (tipo == "FISICO") "Intensidade" else "Força"
                                helpMessage = if (tipo == "FISICO") 
                                    "1-3: Dor leve (quase não incomoda).\n4-7: Dor moderada (incomoda mas dá para seguir).\n8-10: Dor intensa (difícil de suportar)."
                                else 
                                    "Indique o quanto esse sentimento está presente ou impactando seu dia agora."
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text(
                        text = "$intensityLabel (${intensidade.roundToInt()})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            intensidade <= 3 -> Color(0xFF4CAF50)
                            intensidade <= 7 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        }
                    )
                }
                
                Slider(
                    value = intensidade, 
                    onValueChange = { intensidade = it }, 
                    valueRange = 1f..10f, 
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = when {
                            intensidade <= 3 -> Color(0xFF4CAF50)
                            intensidade <= 7 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        },
                        activeTrackColor = when {
                            intensidade <= 3 -> Color(0xFF4CAF50)
                            intensidade <= 7 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQualidade = if (tipo == "FISICO") qualidade else {
                        val base = climaSelecionado ?: "Sem clima"
                        val nuances = if (tagsSelecionadas.isEmpty()) "" else " (" + tagsSelecionadas.joinToString(", ") + ")"
                        "[$base]$nuances"
                    }

                    
                    onConfirm(Sintoma(
                        id = editingSintoma?.id ?: 0,
                        usuarioId = editingSintoma?.usuarioId ?: 0,
                        dataHoraRegistro = editingSintoma?.dataHoraRegistro ?: java.time.LocalDateTime.now(),
                        catalogoId = catalogoId,
                        localizacao = if (tipo == "FISICO") localizacao else null,
                        qualidade = finalQualidade,
                        intensidadeEscala = intensidade.roundToInt(),
                        incapacitante = if (tipo == "FISICO") incapacitante else null,
                        padraoTempo = if (tipo == "FISICO") padraoTempo else null,
                        fatoresAssociados = fatoresAssociados,
                        impactoFuncional = impactoFuncional,
                        tipo = tipo
                    ))
                }, 
                enabled = if (tipo == "FISICO") qualidade.isNotBlank() else climaSelecionado != null
            ) {
                Text(if (editingSintoma != null) "Salvar Alterações" else "Registrar")
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
fun EfeitoColateralAlertCard(medicamentos: List<MedicamentoCausador>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF3CD),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color(0xFF8A6D00),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Pode estar relacionado a um medicamento",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B5300)
                )
                Spacer(modifier = Modifier.height(2.dp))
                medicamentos.forEach { med ->
                    Text(
                        text = "• ${med.nomeMedicamento}: efeito colateral ${med.frequencia.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B5300)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Se persistir ou incomodar, converse com seu médico.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B5300).copy(alpha = 0.8f)
                )
            }
        }
    }
}
