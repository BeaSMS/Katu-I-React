package com.katui.offline.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.katui.offline.Screen
import com.katui.offline.data.local.entities.Exame
import com.katui.offline.data.local.entities.ExameResultado
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.*
import com.katui.offline.ui.theme.AccentPink
import com.katui.offline.ui.viewmodel.ExameViewModel
import com.katui.offline.util.ExamCategorizer
import com.katui.offline.util.TimeScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExameScreen(
    viewModel: ExameViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val exames by viewModel.filteredExames.collectAsState()
    val allExames by viewModel.exames.collectAsState()
    val metricasComDados by viewModel.metricasComDadosSuficientes.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val timeScope by viewModel.timeScope.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val extractedResults by viewModel.extractedResults.collectAsState()
    val extractionError by viewModel.extractionError.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var pdfViewerPath by remember { mutableStateOf<String?>(null) }
    
    val consultaPdfPath by viewModel.consultaPdfPath.collectAsState()
    // Gráficos
    var showTrendGraph by remember { mutableStateOf(false) }
    var trendMetricToOpen by remember { mutableStateOf<String?>(null) }
    var showConsultaDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Exame")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item {
                    val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                        "Exames de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
                    } else {
                        "Meus Exames"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            TimeScope.entries.forEachIndexed { index, scope ->
                                SegmentedButton(
                                    selected = timeScope == scope,
                                    onClick = { viewModel.selectTimeScope(scope) },
                                    shape = SegmentedButtonDefaults.itemShape(index, TimeScope.entries.size),
                                    label = { Text(scope.label) }
                                )
                            }
                        }

                        if (timeScope != TimeScope.ALL) {
                            MonthlyCalendar(
                                selectedDate = selectedDate,
                                onDateSelected = { viewModel.selectDate(it) },
                                collapsible = true,
                                initialExpanded = timeScope == TimeScope.DAY,
                                showOnlyMonthSelector = timeScope == TimeScope.MONTH,
                                hasDataIndicator = { date ->
                                    allExames.any { exame ->
                                        val eDate = exame.dataRegistro.toLocalDate()
                                        if (timeScope == TimeScope.DAY) eDate == date
                                        else eDate.year == date.year && eDate.month == date.month
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filtros:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { viewModel.selectCategory(null) },
                                        label = { Text("Todos") },
                                        leadingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                    )
                                }
                                items(ExamCategorizer.CATEGORIES) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = { Text(category) },
                                        leadingIcon = { 
                                            Icon(
                                                ExamCategorizer.getCategoryIcon(category), 
                                                contentDescription = null, 
                                                modifier = Modifier.size(16.dp),
                                                tint = if (selectedCategory == category) LocalContentColor.current else ExamCategorizer.getCategoryColor(category)
                                            ) 
                                        }
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (metricasComDados.isNotEmpty()) {
                                    Button(
                                        onClick = { showTrendGraph = true },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tendências", maxLines = 1)
                                    }
                                }
                                
                                Button(
                                    onClick = { showConsultaDialog = true },
                                    modifier = if (metricasComDados.isNotEmpty()) Modifier.weight(1f).heightIn(min = 48.dp) else Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Modo Consulta", maxLines = 1)
                                }
                                IconButton(
                                    onClick = { 
                                        helpTitle = "Modo Consulta"
                                        helpMessage = "O Modo Consulta gera um PDF organizado com seus exames e gráficos de tendência para você mostrar ao seu médico.\n\nVocê escolhe a especialidade e o Katu'I sugere quais exames anexar automaticamente."
                                    }
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                if (exames.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedCategory != null) "Nenhum exame de $selectedCategory neste dia." else "Nenhum exame para este dia.",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(exames) { exame ->
                        ExameCard(
                            exame = exame,
                            viewModel = viewModel,
                            metricasComDados = metricasComDados,
                            onOpenTrend = { metrica ->
                                trendMetricToOpen = metrica
                                showTrendGraph = true
                            },
                            onDelete = { viewModel.deleteExame(exame) },
                            onImageClick = { fullScreenImageUri = it },
                            onPdfClick = { pdfViewerPath = it }
                        )
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Processando...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExameDialog(
            onDismiss = { 
                showAddDialog = false
                viewModel.clearExtractedResults()
                viewModel.clearExtractionError()
            },
            onConfirmImage = { nome, cat, obs, path, res ->
                viewModel.addExame(nome, cat, obs, path, res)
                showAddDialog = false
                viewModel.clearExtractedResults()
            },
            onConfirmPdf = { nome, cat, obs, pdfUri, res ->
                viewModel.addExameFromPdf(context, nome, cat, obs, pdfUri, res)
                showAddDialog = false
                viewModel.clearExtractedResults()
            },
            onConfirmMultipleImages = { nome, cat, obs, uris, res ->
                viewModel.addExameFromImages(context, nome, cat, obs, uris, res)
                showAddDialog = false
                viewModel.clearExtractedResults()
            },
            onExtractMetrics = { uri ->
                viewModel.extractMetrics(context, uri)
            },
            extractedResults = extractedResults,
            extractionError = extractionError,
            onDismissExtractionError = { viewModel.clearExtractionError() },
            isExtracting = isProcessing,
            onNavigateToSettings = { onNavigate(Screen.Settings.route) }
        )
    }

    if (showConsultaDialog) {
        ConsultaDialog(
            allExames = viewModel.exames.collectAsState().value,
            isProcessing = isProcessing,
            onDismiss = { showConsultaDialog = false },
            onGerarPdf = { especialidade, examesSel ->
                viewModel.gerarPdfConsulta(context, "Paciente", especialidade, examesSel)
            },
            onVerGaleria = { uris -> fullScreenImageUri = uris.firstOrNull() }
        )
    }

    consultaPdfPath?.let { path ->
        LaunchedEffect(path) {
            pdfViewerPath = path
            viewModel.clearConsultaPdf()
            showConsultaDialog = false
        }
    }

    if (showTrendGraph) {
        ExamTrendDialog(
            viewModel = viewModel,
            initialMetric = trendMetricToOpen,
            onDismiss = { 
                showTrendGraph = false
                trendMetricToOpen = null
            }
        )
    }

    fullScreenImageUri?.let { uri ->
        FullScreenImageDialog(imageUri = uri, onDismiss = { fullScreenImageUri = null })
    }

    pdfViewerPath?.let { path ->
        PdfViewerDialog(filePath = path, onDismiss = { pdfViewerPath = null })
    }
}

@Composable
fun ExameCard(
    exame: Exame,
    viewModel: ExameViewModel,
    metricasComDados: Set<String> = emptySet(),
    onOpenTrend: (String) -> Unit = {},
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val resultados by viewModel.selectedExameResultados.collectAsState()

    LaunchedEffect(expanded) {
        if (expanded) {
            viewModel.loadResultados(exame.id)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Exame?") },
            text = { Text("Tem certeza que deseja excluir \"${exame.nome}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            exame.arquivoPath?.let { path ->
                if (exame.isPdf) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { onPdfClick(path) },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text("Ver PDF Anexo", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    AsyncImage(
                        model = java.io.File(path),
                        contentDescription = "Imagem do exame",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (expanded) 200.dp else 100.dp)
                            .clickable { onImageClick(path) },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ExamCategorizer.getCategoryColor(exame.categoria).copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            ExamCategorizer.getCategoryIcon(exame.categoria),
                            contentDescription = null,
                            tint = ExamCategorizer.getCategoryColor(exame.categoria),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exame.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = exame.categoria,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (exame.observacao?.isNotBlank() == true) {
                        Text(
                            text = exame.observacao,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (resultados.isNotEmpty()) {
                        Text(
                            "Resultados Registrados:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        resultados.forEach { res ->
                            val temTendencia = res.metrica in metricasComDados
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = temTendencia) { onOpenTrend(res.metrica) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(res.metrica, style = MaterialTheme.typography.bodyMedium)
                                    if (temTendencia) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            contentDescription = "Ver tendência",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                Text("${res.valor} ${res.unidade ?: ""}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excluir Exame", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExameDialog(
    onDismiss: () -> Unit,
    onConfirmImage: (String, String, String, String?, List<ExameResultado>) -> Unit,
    onConfirmPdf: (String, String, String, android.net.Uri, List<ExameResultado>) -> Unit,
    onConfirmMultipleImages: (String, String, String, List<android.net.Uri>, List<ExameResultado>) -> Unit,
    onExtractMetrics: (android.net.Uri) -> Unit,
    extractedResults: List<ExameResultado>,
    extractionError: String? = null,
    onDismissExtractionError: () -> Unit = {},
    isExtracting: Boolean = false,
    onNavigateToSettings: (() -> Unit)? = null
) {
    var nome by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Outros") }
    var observacao by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var tipo by remember { mutableStateOf(ExameTipo.IMAGEM_UNICA) }
    var isCategoryManuallySelected by remember { mutableStateOf(false) }

    var resultados by remember { mutableStateOf(listOf<ExameResultado>()) }
    var showAddResultField by remember { mutableStateOf(false) }

    // Ajuda interna
    var helpTitle by remember { mutableStateOf<String?>(null) }
    var helpMessage by remember { mutableStateOf<String?>(null) }

    if (extractionError != null) {
        AlertDialog(
            onDismissRequest = onDismissExtractionError,
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Configuração de IA Necessária") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (extractionError.contains("Chave", ignoreCase = true)) 
                            "Para ler exames automaticamente, você precisa configurar sua própria chave da IA."
                        else extractionError,
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
                    
                    if (!extractionError.contains("Chave", ignoreCase = true)) {
                        Text("Erro técnico: $extractionError", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (extractionError.contains("Chave", ignoreCase = true) && onNavigateToSettings != null) {
                    Button(onClick = { onDismissExtractionError(); onDismiss(); onNavigateToSettings() }) {
                        Text("Configurar Agora")
                    }
                } else {
                    Button(onClick = onDismissExtractionError) { Text("Entendi") }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExtractionError) { Text("Fechar") }
            }
        )
    }

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

    LaunchedEffect(extractedResults) {
        if (extractedResults.isNotEmpty()) {
            resultados = (resultados + extractedResults).distinctBy { it.metrica }
        }
    }

    var singleImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val singleImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> singleImageUri = uri }
    var multipleImages by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val multipleImagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> multipleImages = uris }
    var pdfUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> pdfUri = uri }

    val dateString = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Exame") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isExtracting) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Analisando exame com IA... isso pode levar alguns segundos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { 
                        nome = it 
                        if (!isCategoryManuallySelected) {
                            categoria = ExamCategorizer.suggestCategory(it)
                        }
                    },
                    label = { Text("Nome do Exame (ex: Hemograma)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Categoria", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ExamCategorizer.CATEGORIES) { cat ->
                            FilterChip(
                                selected = categoria == cat,
                                onClick = { categoria = cat; isCategoryManuallySelected = true },
                                label = { Text(cat) },
                                leadingIcon = { 
                                    Icon(
                                        ExamCategorizer.getCategoryIcon(cat), 
                                        contentDescription = null, 
                                        modifier = Modifier.size(16.dp),
                                        tint = if (categoria == cat) LocalContentColor.current else ExamCategorizer.getCategoryColor(cat)
                                    ) 
                                }
                            )
                        }
                    }
                }

                DateSelectorField(
                    label = "Data do Exame", 
                    selectedDate = selectedDate, 
                    onDateSelected = { selectedDate = it },
                    maxDate = LocalDate.now()
                )

                OutlinedTextField(value = observacao, onValueChange = { observacao = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Valores do Exame (Opcional)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            IconButton(
                                onClick = { 
                                    helpTitle = "Valores do Exame"
                                    helpMessage = "Você pode extrair os valores automaticamente usando o botão 'IA' abaixo ou adicioná-los manualmente clicando no '+'.\n\nExtrair valores permite que o Katu'I gere gráficos de tendência para você acompanhar sua saúde."
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Ajuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        IconButton(onClick = { showAddResultField = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Adicionar Valor", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (resultados.isEmpty()) {
                        Text("Nenhum valor adicionado. Use a IA ou adicione manualmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        resultados.forEachIndexed { index, res ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(res.metrica, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${res.valor} ${res.unidade ?: ""}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { resultados = resultados.toMutableList().apply { removeAt(index) } }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddResultField) {
                    var metricaNome by remember { mutableStateOf("") }
                    var valorText by remember { mutableStateOf("") }
                    var unidadeNome by remember { mutableStateOf("") }
                    
                    val suggestions = ExamCategorizer.getSuggestedMetrics(categoria)

                    AlertDialog(
                        onDismissRequest = { showAddResultField = false },
                        title = { Text("Adicionar Valor") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (suggestions.isNotEmpty() && metricaNome.isBlank()) {
                                    Text("Sugestões para $categoria:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(suggestions) { (nome, unidade) ->
                                            SuggestionChip(
                                                onClick = { 
                                                    metricaNome = nome
                                                    unidadeNome = unidade
                                                },
                                                label = { Text(nome, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(value = metricaNome, onValueChange = { metricaNome = it }, label = { Text("Métrica") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = valorText, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) valorText = it }, label = { Text("Valor") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                OutlinedTextField(value = unidadeNome, onValueChange = { unidadeNome = it }, label = { Text("Unidade") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                resultados = resultados + ExameResultado(exameId = 0, metrica = metricaNome, valor = valorText.toDoubleOrNull() ?: 0.0, unidade = unidadeNome)
                                showAddResultField = false
                            }, enabled = metricaNome.isNotBlank() && valorText.isNotBlank()) { Text("Adicionar") }
                        },
                        dismissButton = { TextButton(onClick = { showAddResultField = false }) { Text("Cancelar") } }
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = tipo == ExameTipo.IMAGEM_UNICA, onClick = { tipo = ExameTipo.IMAGEM_UNICA }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Foto") }
                    SegmentedButton(selected = tipo == ExameTipo.MULTIPLAS_IMAGENS, onClick = { tipo = ExameTipo.MULTIPLAS_IMAGENS }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Fotos→PDF") }
                    SegmentedButton(selected = tipo == ExameTipo.PDF, onClick = { tipo = ExameTipo.PDF }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("PDF") }
                }

                when (tipo) {
                    ExameTipo.IMAGEM_UNICA -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { singleImageLauncher.launch("image/*") }, enabled = !isExtracting, modifier = Modifier.weight(1f)) { Text(if (singleImageUri == null) "Selecionar" else "Trocar") }
                            if (singleImageUri != null) {
                                Button(onClick = { onExtractMetrics(singleImageUri!!) }, enabled = !isExtracting, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("IA")
                                }
                            }
                        }
                    }
                    ExameTipo.PDF -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { pdfLauncher.launch("application/pdf") }, enabled = !isExtracting, modifier = Modifier.weight(1f)) { Text(if (pdfUri == null) "Selecionar" else "Trocar") }
                            if (pdfUri != null) {
                                Button(onClick = { onExtractMetrics(pdfUri!!) }, enabled = !isExtracting, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("IA")
                                }
                            }
                        }
                    }
                    ExameTipo.MULTIPLAS_IMAGENS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { multipleImagesLauncher.launch("image/*") }, enabled = !isExtracting, modifier = Modifier.weight(1f)) {
                                    Text(if (multipleImages.isEmpty()) "Selecionar Fotos" else "Trocar (${multipleImages.size} selecionadas)")
                                }
                                if (multipleImages.isNotEmpty()) {
                                    Button(onClick = { onExtractMetrics(multipleImages.first()) }, enabled = !isExtracting, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary), modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("IA")
                                    }
                                }
                            }
                            if (multipleImages.isNotEmpty()) {
                                Text(
                                    "A extração por IA analisa apenas a primeira foto selecionada.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            val isValid = nome.isNotBlank()
            Button(
                onClick = {
                    val obs = "Data: $dateString. $observacao".trim()
                    when (tipo) {
                        ExameTipo.IMAGEM_UNICA -> onConfirmImage(nome, categoria, obs, singleImageUri?.toString(), resultados)
                        ExameTipo.MULTIPLAS_IMAGENS -> onConfirmMultipleImages(nome, categoria, obs, multipleImages, resultados)
                        ExameTipo.PDF -> onConfirmPdf(nome, categoria, obs, pdfUri!!, resultados)
                    }
                },
                enabled = isValid
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ExamTrendDialog(
    viewModel: ExameViewModel,
    initialMetric: String? = null,
    onDismiss: () -> Unit
) {
    val metrics by viewModel.metricasComDadosSuficientes.collectAsState()
    var selectedMetric by remember { mutableStateOf(initialMetric) }

    LaunchedEffect(metrics, initialMetric) {
        if (selectedMetric == null || selectedMetric !in metrics) {
            selectedMetric = if (initialMetric != null && initialMetric in metrics) initialMetric else metrics.firstOrNull()
        }
    }

    val historico by if (selectedMetric != null) {
        viewModel.getHistoricoMetrica(selectedMetric!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<ExameResultado>()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tendências de Saúde") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (metrics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Nenhum dado registrado para gerar gráficos. Adicione valores aos seus exames (manualmente ou com a IA) para acompanhar a evolução ao longo do tempo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = metrics.indexOf(selectedMetric).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {},
                        edgePadding = 0.dp
                    ) {
                        metrics.forEach { metric ->
                            FilterChip(
                                selected = selectedMetric == metric,
                                onClick = { selectedMetric = metric },
                                label = { Text(metric, fontSize = 12.sp) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (historico.size < 2) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Text("Dados insuficientes para este gráfico.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        SimpleLineChartExames(historico, selectedMetric!!)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun SimpleLineChartExames(data: List<ExameResultado>, metrica: String) {
    val dataPoints = data.map { it.valor.toFloat() }
    val maxPoint = dataPoints.maxOrNull() ?: 100f
    val minPoint = dataPoints.minOrNull() ?: 0f
    
    val normalRange = com.katui.offline.util.MedicalUnitConverter.parseReferenceRange(data.firstOrNull { it.referencia != null }?.referencia)
        ?: ExamCategorizer.getNormalRange(metrica)
    val yMinBase = minOf(minPoint, normalRange?.first?.toFloat() ?: minPoint)
    val yMaxBase = maxOf(maxPoint, normalRange?.second?.toFloat() ?: maxPoint)
    
    val amplitudeBase = (yMaxBase - yMinBase).coerceAtLeast(10f)
    val chartMin = yMinBase - (amplitudeBase * 0.1f)
    val chartMax = yMaxBase + (amplitudeBase * 0.1f)
    val chartAmplitude = chartMax - chartMin
    
    val color = AccentPink
    val labelColor = MaterialTheme.colorScheme.outline
    
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val w = size.width
            val h = size.height
            
            val leftPadding = 40.dp.toPx()
            val bottomPadding = 30.dp.toPx()
            val topPadding = 20.dp.toPx()
            val rightPadding = 20.dp.toPx()
            
            val chartW = w - leftPadding - rightPadding
            val chartH = h - bottomPadding - topPadding

            val n = dataPoints.size
            val spacing = if (n > 1) chartW / (n - 1) else chartW

            fun xOf(index: Int) = leftPadding + index * spacing
            fun yOf(value: Float) = topPadding + chartH - ((value - chartMin) / chartAmplitude * chartH)

            normalRange?.let { (nMin, nMax) ->
                val topN = yOf(nMax.toFloat())
                val bottomN = yOf(nMin.toFloat())
                drawRect(
                    color = Color.Green.copy(alpha = 0.08f),
                    topLeft = Offset(leftPadding, topN),
                    size = androidx.compose.ui.geometry.Size(chartW, (bottomN - topN).coerceAtLeast(0f))
                )
                drawLine(Color.Green.copy(alpha = 0.2f), Offset(leftPadding, topN), Offset(leftPadding + chartW, topN), strokeWidth = 1.dp.toPx())
                drawLine(Color.Green.copy(alpha = 0.2f), Offset(leftPadding, bottomN), Offset(leftPadding + chartW, bottomN), strokeWidth = 1.dp.toPx())
            }

            drawLine(labelColor.copy(alpha = 0.3f), Offset(leftPadding, topPadding), Offset(leftPadding, topPadding + chartH))
            drawLine(labelColor.copy(alpha = 0.3f), Offset(leftPadding, topPadding + chartH), Offset(leftPadding + chartW, topPadding + chartH))

            val yLevels = listOf(chartMin, chartMin + chartAmplitude/2, chartMax)
            yLevels.forEach { valV ->
                val y = yOf(valV)
                val label = String.format(Locale.getDefault(), "%.1f", valV)
                
                drawContext.canvas.nativeCanvas.drawText(
                    label, 
                    leftPadding - 8.dp.toPx(), 
                    y + 4.dp.toPx(), 
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        this.color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
                drawLine(labelColor.copy(alpha = 0.05f), Offset(leftPadding, y), Offset(leftPadding + chartW, y))
            }

            val step = (n / 3).coerceAtLeast(1)
            for (i in 0 until n step step) {
                val x = xOf(i)
                val label = "Ex ${i+1}" 
                
                drawContext.canvas.nativeCanvas.drawText(
                    label, 
                    x, 
                    topPadding + chartH + 18.dp.toPx(), 
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        this.color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            val path = Path()
            val fillPath = Path()
            dataPoints.forEachIndexed { i, value ->
                val x = xOf(i)
                val y = yOf(value)
                
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, topPadding + chartH)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                
                val label = String.format(Locale.getDefault(), "%.1f", value)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    y - 10.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        this.color = color.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }
            fillPath.lineTo(xOf(n-1), topPadding + chartH)
            fillPath.close()

            if (dataPoints.size > 1) {
                drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
                drawPath(fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
            }
        }
        
        val unit = data.firstOrNull()?.unidade ?: ""
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Text(text = "Evolução de $metrica ($unit)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class ExameTipo { IMAGEM_UNICA, MULTIPLAS_IMAGENS, PDF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultaDialog(
    allExames: List<com.katui.offline.data.local.entities.Exame>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onGerarPdf: (especialidade: String, exames: List<com.katui.offline.data.local.entities.Exame>) -> Unit,
    onVerGaleria: (uris: List<String>) -> Unit
) {
    var especialidade by remember { mutableStateOf(ExamCategorizer.ESPECIALIDADES.first()) }
    var expandedEsp by remember { mutableStateOf(false) }

    val sugeridos = remember(especialidade, allExames) {
        val cats = ExamCategorizer.categoriasPorEspecialidade(especialidade).toSet()
        cats.flatMap { cat -> allExames.filter { it.categoria == cat }.take(2) }.distinctBy { it.id }
    }

    val selecionados = remember(sugeridos) { mutableStateListOf<com.katui.offline.data.local.entities.Exame>().apply { addAll(sugeridos) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preparar para Consulta")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Especialidade do médico", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Box {
                    OutlinedButton(
                        onClick = { expandedEsp = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(especialidade, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expandedEsp, onDismissRequest = { expandedEsp = false }) {
                        ExamCategorizer.ESPECIALIDADES.forEach { esp ->
                            DropdownMenuItem(
                                text = { Text(esp) },
                                onClick = {
                                    especialidade = esp
                                    expandedEsp = false
                                    selecionados.clear()
                                    selecionados.addAll(sugeridos)
                                }
                            )
                        }
                    }
                }

                if (sugeridos.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Sugestão inteligente: pré-selecionamos os exames mais relevantes para $especialidade.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "${selecionados.size} de ${allExames.size} exames selecionados",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (allExames.isEmpty()) {
                    Text("Nenhum exame registrado ainda.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                } else {
                    allExames.forEach { exame ->
                        val checked = exame in selecionados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selecionados.remove(exame) else selecionados.add(exame)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { if (it) selecionados.add(exame) else selecionados.remove(exame) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exame.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        ExamCategorizer.getCategoryIcon(exame.categoria),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = ExamCategorizer.getCategoryColor(exame.categoria)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        exame.categoria,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (exame in sugeridos) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        "sugerido",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Gerando PDF...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onGerarPdf(especialidade, selecionados.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selecionados.isNotEmpty() && !isProcessing
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gerar PDF para Consulta")
                }

                val comImagem = selecionados.filter { it.arquivoPath != null && !it.isPdf }
                if (comImagem.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onVerGaleria(comImagem.mapNotNull { it.arquivoPath }) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galeria em Tela Cheia (${comImagem.size} imagens)")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        }
    )
}
