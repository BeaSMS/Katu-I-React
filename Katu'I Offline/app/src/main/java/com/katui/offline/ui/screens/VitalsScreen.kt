package com.katui.offline.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.data.local.entities.Vitals
import com.katui.offline.ui.components.DateSelectorField
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.TimeSelectorField
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.TimeRange
import com.katui.offline.ui.viewmodel.VitalTipo
import com.katui.offline.ui.viewmodel.VitalsViewModel
import com.katui.offline.util.VitalsInterpreter
import com.katui.offline.util.VitalsStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(viewModel: VitalsViewModel = hiltViewModel()) {
    val vitalsList by viewModel.vitals.collectAsState()
    val filteredVitals by viewModel.filteredVitals.collectAsState()
    val historicVitals by viewModel.historicVitals.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTipo by viewModel.selectedTipo.collectAsState()
    val usuario by viewModel.usuario.collectAsState()

    var showAddDialog by remember { mutableStateOf<String?>(null) }
    var selectedChart by remember { mutableStateOf("Pressão") }

    Scaffold(
        floatingActionButton = {
            var isMenuExpanded by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isMenuExpanded) {
                    VitalsQuickAction("Glicose", Icons.Default.Bloodtype, AccentPink) { showAddDialog = "Glicose"; isMenuExpanded = false }
                    VitalsQuickAction("Pressão", Icons.Default.Compress, AccentOrange) { showAddDialog = "Pressão"; isMenuExpanded = false }
                    VitalsQuickAction("Batimentos", Icons.Default.Favorite, Color(0xFFE53935)) { showAddDialog = "Batimentos"; isMenuExpanded = false }
                    VitalsQuickAction("Saturação", Icons.Default.Opacity, Color(0xFF03A9F4)) { showAddDialog = "Saturação"; isMenuExpanded = false }
                    VitalsQuickAction("Temperatura", Icons.Default.DeviceThermostat, AccentPurple) { showAddDialog = "Temperatura"; isMenuExpanded = false }
                    VitalsQuickAction("Peso", Icons.Default.MonitorWeight, AccentBlue) { showAddDialog = "Peso"; isMenuExpanded = false }
                    val idade = usuario?.idade ?: 0
                    if (idade in 1..18) {
                        VitalsQuickAction("Altura (Crescimento)", Icons.Default.Straighten, AccentGreen) { showAddDialog = "Altura"; isMenuExpanded = false }
                    }
                }
                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Adicionar")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                    "Sinais de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
                } else {
                    "Sinais Vitais"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            item {
                val subTitle = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                    "Diário de Saúde de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
                } else {
                    "Diário de Saúde"
                }
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // ── Gráfico de tendência ──────────────────────────────────────────
            if (vitalsList.isNotEmpty()) {
                item {
                    VitalsTrendCard(
                        vitals = filteredVitals,
                        currentChart = selectedChart,
                        selectedTimeRange = selectedTimeRange,
                        onChartSelect = { selectedChart = it },
                        onTimeRangeSelect = { viewModel.setTimeRange(it) }
                    )
                }
            }

            // ── Calendário + Filtro de tipo ───────────────────────────────────
            item {
                MonthlyCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    hasDataIndicator = { date ->
                        vitalsList.any { it.dataHora.toLocalDate() == date }
                    }
                )
            }

            item {
                // Chips de filtro por tipo de sinal
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(VitalTipo.values()) { tipo ->
                        FilterChip(
                            selected = selectedTipo == tipo,
                            onClick = { viewModel.setSelectedTipo(tipo) },
                            label = { Text(tipo.label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (selectedTipo == VitalTipo.TODOS) "Histórico — ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                    else "${selectedTipo.label} — ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Lista de registros filtrados ──────────────────────────────────
            if (historicVitals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (vitalsList.none { it.dataHora.toLocalDate() == selectedDate })
                                    "Nenhum registro neste dia"
                                else "Nenhum registro de ${selectedTipo.label.lowercase()} neste dia",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                items(historicVitals) { item ->
                    VitalsCard(vitals = item, onDelete = { viewModel.deleteVitals(item) })
                }
            }
        }
    }

    showAddDialog?.let { type ->
        AddSpecificVitalsDialog(
            type = type,
            usuario = usuario,
            onDismiss = { showAddDialog = null },
            onConfirm = { peso, altura, sis, dia, fc, glic, sat, temp, obs, dataHora ->
                viewModel.addVitals(peso, altura, sis, dia, fc, glic, sat, temp, obs, dataHora)
                showAddDialog = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gráfico de tendência
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VitalsTrendCard(
    vitals: List<Vitals>,
    currentChart: String,
    selectedTimeRange: TimeRange,
    onChartSelect: (String) -> Unit,
    onTimeRangeSelect: (TimeRange) -> Unit
) {
    val allCharts = listOf("Pressão", "Glicose", "Peso", "Altura", "Batimentos", "Saturação", "Temperatura")

    val availableCharts = allCharts.filter { chart ->
        vitals.count {
            when (chart) {
                "Pressão" -> it.sistolica != null
                "Glicose" -> it.glicose != null
                "Peso" -> it.peso != null
                "Altura" -> it.alturaCm != null
                "Batimentos" -> it.frequenciaCardiaca != null
                "Saturação" -> it.saturacaoOxigenio != null
                "Temperatura" -> it.temperatura != null
                else -> false
            }
        } >= 2
    }

    LaunchedEffect(availableCharts) {
        if (currentChart !in availableCharts && availableCharts.isNotEmpty()) {
            onChartSelect(availableCharts.first())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tendência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeRangeChip("Dia", selectedTimeRange == TimeRange.DAY) { onTimeRangeSelect(TimeRange.DAY) }
                    TimeRangeChip("Semana", selectedTimeRange == TimeRange.WEEK) { onTimeRangeSelect(TimeRange.WEEK) }
                    TimeRangeChip("Mês", selectedTimeRange == TimeRange.MONTH) { onTimeRangeSelect(TimeRange.MONTH) }
                }
            }

            if (availableCharts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ScrollableTabRow(
                    selectedTabIndex = availableCharts.indexOf(currentChart).coerceAtLeast(0),
                    containerColor = Color.Transparent, divider = {}, indicator = {}, edgePadding = 0.dp
                ) {
                    availableCharts.forEach { chart ->
                        FilterChip(
                            selected = currentChart == chart,
                            onClick = { onChartSelect(chart) },
                            label = { Text(chart, fontSize = 11.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                SimpleLineChart(vitals, currentChart, selectedTimeRange)
                Spacer(modifier = Modifier.height(8.dp))
                val count = vitals.count {
                    when (currentChart) {
                        "Pressão" -> it.sistolica != null; "Glicose" -> it.glicose != null
                        "Peso" -> it.peso != null; "Altura" -> it.alturaCm != null
                        "Batimentos" -> it.frequenciaCardiaca != null
                        "Saturação" -> it.saturacaoOxigenio != null
                        "Temperatura" -> it.temperatura != null; else -> false
                    }
                }
                Text("$count registros no período", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Dados insuficientes para este período.", color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TimeRangeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SimpleLineChart(vitals: List<Vitals>, type: String, range: TimeRange) {

    // ── Extrai apenas os registros do tipo selecionado ────────────────────────
    val relevantVitals = vitals.filter {
        when (type) {
            "Pressão" -> it.sistolica != null
            "Glicose" -> it.glicose != null
            "Peso" -> it.peso != null
            "Altura" -> it.alturaCm != null
            "Batimentos" -> it.frequenciaCardiaca != null
            "Saturação" -> it.saturacaoOxigenio != null
            "Temperatura" -> it.temperatura != null
            else -> false
        }
    }.sortedBy { it.dataHora }

    if (relevantVitals.size < 2) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            Text("Poucos dados para este período", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    // ── Agrupa por slot de tempo ──────────────────────────────────────────────
    val minTime = relevantVitals.first().dataHora
    val maxTime = relevantVitals.last().dataHora
    val durationSeconds = java.time.Duration.between(minTime, maxTime).seconds.coerceAtLeast(1)

    val slotKey: (Vitals) -> java.time.LocalDateTime = when {
        durationSeconds < 120 -> { v -> v.dataHora.withNano(0) }
        durationSeconds < 7200 -> { v -> v.dataHora.withSecond(0).withNano(0) }
        durationSeconds < 172800 -> { v -> v.dataHora.withMinute(0).withSecond(0).withNano(0) }
        else -> { v -> v.dataHora.toLocalDate().atStartOfDay() }
    }

    val groupedData = relevantVitals
        .groupBy(slotKey)
        .toSortedMap()
        .mapValues { (_, entries) ->
            if (type == "Pressão") {
                val sis = entries.mapNotNull { it.sistolica?.toFloat() }.average().takeIf { !it.isNaN() }?.toFloat()
                val dia = entries.mapNotNull { it.diastolica?.toFloat() }.average().takeIf { !it.isNaN() }?.toFloat()
                if (sis != null && dia != null) sis to dia else null
            } else {
                val v = entries.mapNotNull {
                    when (type) {
                        "Glicose" -> it.glicose?.toFloat()
                        "Peso" -> it.peso?.toFloat()
                        "Altura" -> it.alturaCm?.toFloat()
                        "Batimentos" -> it.frequenciaCardiaca?.toFloat()
                        "Saturação" -> it.saturacaoOxigenio?.toFloat()
                        "Temperatura" -> it.temperatura?.toFloat()
                        else -> null
                    }
                }.average().takeIf { !it.isNaN() }?.toFloat()
                if (v != null) v to null else null
            }
        }
        .filterValues { it != null }

    val keys = groupedData.keys.toList()
    val dataPoints = groupedData.values.mapNotNull { it }

    if (dataPoints.size < 2) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            Text("Registros muito próximos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val allValues = dataPoints.flatMap { listOfNotNull(it.first, it.second) }
    val maxV = allValues.maxOrNull() ?: 100f
    val minV = (allValues.minOrNull() ?: 0f)
    val amplitude = (maxV - minV).coerceAtLeast(10f)
    
    // Ajuste para não colar no topo/fundo
    val chartMin = minV - (amplitude * 0.1f)
    val chartMax = maxV + (amplitude * 0.1f)
    val chartAmplitude = chartMax - chartMin

    val colorPrimary = when (type) {
        "Pressão" -> AccentOrange; "Glicose" -> AccentPink; "Peso" -> AccentBlue
        "Altura" -> AccentGreen; "Batimentos" -> Color(0xFFE53935)
        "Saturação" -> Color(0xFF03A9F4); else -> AccentPurple
    }
    val colorSecondary = Color(0xFF2196F3)
    val labelColor = MaterialTheme.colorScheme.outline
    
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = labelColor)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val w = size.width
            val h = size.height
            
            // Área de labels Y (esquerda) e X (fundo)
            val leftPadding = 35.dp.toPx()
            val bottomPadding = 25.dp.toPx()
            val topPadding = 10.dp.toPx()
            val rightPadding = 15.dp.toPx()
            
            val chartW = w - leftPadding - rightPadding
            val chartH = h - bottomPadding - topPadding

            val n = dataPoints.size
            val spacing = chartW / (n - 1)

            fun xOf(index: Int) = leftPadding + index * spacing
            fun yOf(value: Float) = topPadding + chartH - ((value - chartMin) / chartAmplitude * chartH)

            // ── Desenha Eixos e Grid ──────────────────────────────────────────
            // Linha Y
            drawLine(labelColor.copy(alpha = 0.3f), Offset(leftPadding, topPadding), Offset(leftPadding, topPadding + chartH))
            // Linha X
            drawLine(labelColor.copy(alpha = 0.3f), Offset(leftPadding, topPadding + chartH), Offset(leftPadding + chartW, topPadding + chartH))

            // Labels Y (3 níveis)
            val yLevels = listOf(chartMin, chartMin + chartAmplitude/2, chartMax)
            yLevels.forEach { valV ->
                val y = yOf(valV)
                val label = String.format(Locale.getDefault(), "%.1f", valV)
                val textLayout = textMeasurer.measure(label, labelStyle)
                
                drawContext.canvas.nativeCanvas.drawText(
                    label, 
                    leftPadding - 5.dp.toPx() - textLayout.size.width, 
                    y + textLayout.size.height/2, 
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
                // Grid horizontal
                drawLine(labelColor.copy(alpha = 0.1f), Offset(leftPadding, y), Offset(leftPadding + chartW, y))
            }

            // Labels X (Datas - Primeiro, Meio, Último)
            val xIndices = if (n > 2) listOf(0, n/2, n-1) else listOf(0, n-1)
            xIndices.forEach { idx ->
                val x = xOf(idx)
                val date = keys[idx]
                val pattern = when {
                    durationSeconds < 3600 -> "HH:mm:ss"
                    durationSeconds < 86400 -> "HH:mm"
                    else -> "dd/MM"
                }
                val label = date.format(DateTimeFormatter.ofPattern(pattern))
                
                drawContext.canvas.nativeCanvas.drawText(
                    label, 
                    x, 
                    topPadding + chartH + 15.dp.toPx(), 
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // ── Desenha as Linhas ─────────────────────────────────────────────
            if (type == "Pressão") {
                val pathSis = Path()
                val pathDia = Path()
                dataPoints.forEachIndexed { i, pair ->
                    val x = xOf(i)
                    val yS = yOf(pair.first)
                    if (i == 0) pathSis.moveTo(x, yS) else pathSis.lineTo(x, yS)
                    drawCircle(colorPrimary, 3.dp.toPx(), Offset(x, yS))

                    pair.second?.let { d ->
                        val yD = yOf(d)
                        if (i == 0) pathDia.moveTo(x, yD) else pathDia.lineTo(x, yD)
                        drawCircle(colorSecondary, 3.dp.toPx(), Offset(x, yD))
                    }
                }
                drawPath(pathSis, colorPrimary, style = Stroke(2.dp.toPx()))
                drawPath(pathDia, colorSecondary, style = Stroke(2.dp.toPx()))

                // Labels nos pontos (Pressão)
                dataPoints.forEachIndexed { i, pair ->
                    val x = xOf(i)
                    val labelS = String.format(Locale.getDefault(), "%.0f", pair.first)
                    val textLayoutS = textMeasurer.measure(labelS, labelStyle.copy(fontWeight = FontWeight.Bold))
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        labelS,
                        x,
                        yOf(pair.first) - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 8.sp.toPx()
                            color = colorPrimary.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )

                    pair.second?.let { d ->
                        val labelD = String.format(Locale.getDefault(), "%.0f", d)
                        drawContext.canvas.nativeCanvas.drawText(
                            labelD,
                            x,
                            yOf(d) + 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 8.sp.toPx()
                                color = colorSecondary.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            } else {
                val path = Path()
                val fillPath = Path()
                dataPoints.forEachIndexed { i, pair ->
                    val x = xOf(i)
                    val y = yOf(pair.first)
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, topPadding + chartH)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    drawCircle(colorPrimary, 3.dp.toPx(), Offset(x, y))
                }
                fillPath.lineTo(xOf(n-1), topPadding + chartH)
                fillPath.close()
                
                drawPath(path, colorPrimary, style = Stroke(2.dp.toPx()))
                drawPath(fillPath, Brush.verticalGradient(listOf(colorPrimary.copy(alpha = 0.2f), Color.Transparent)))

                // Labels nos pontos (Outros)
                dataPoints.forEachIndexed { i, pair ->
                    val x = xOf(i)
                    val y = yOf(pair.first)
                    val label = if (type == "Temperatura" || type == "Peso") 
                        String.format(Locale.getDefault(), "%.1f", pair.first)
                    else 
                        String.format(Locale.getDefault(), "%.0f", pair.first)
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        y - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 8.sp.toPx()
                            color = colorPrimary.toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )
                }
            }
        }
        
        // Legenda se for Pressão
        if (type == "Pressão") {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 35.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Sistólica", colorPrimary)
                LegendItem("Diastólica", colorSecondary)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cards e helpers (sem mudança de lógica)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VitalsQuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium)
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = color, contentColor = Color.White) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun VitalsCard(vitals: Vitals, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Registro?") },
            text = { Text("Deseja excluir este registro de sinais vitais? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = vitals.dataHora.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (vitals.sistolica != null) {
                    val status = VitalsInterpreter.getPressureStatus(vitals.sistolica, vitals.diastolica)
                    VitalsRowInfo("Pressão", "${vitals.sistolica}/${vitals.diastolica}", "mmHg", AccentOrange, status)
                }
                if (vitals.glicose != null) {
                    val status = VitalsInterpreter.getGlucoseStatus(vitals.glicose)
                    VitalsRowInfo("Glicose", "${vitals.glicose}", "mg/dL", AccentPink, status)
                }
                if (vitals.saturacaoOxigenio != null) {
                    val status = VitalsInterpreter.getSpO2Status(vitals.saturacaoOxigenio)
                    VitalsRowInfo("SpO2", "${vitals.saturacaoOxigenio}", "%", Color(0xFF03A9F4), status)
                }
                if (vitals.temperatura != null) {
                    val status = VitalsInterpreter.getTempStatus(vitals.temperatura)
                    VitalsRowInfo("Temp", "${vitals.temperatura}", "°C", AccentPurple, status)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (vitals.peso != null) VitalsMiniInfo("Peso", "${vitals.peso}", "kg", AccentBlue, Modifier.weight(1f))
                    if (vitals.alturaCm != null) VitalsMiniInfo("Altura", "${vitals.alturaCm}", "cm", AccentGreen, Modifier.weight(1f))
                    if (vitals.frequenciaCardiaca != null) VitalsMiniInfo("BPM", "${vitals.frequenciaCardiaca}", "bpm", Color(0xFFE53935), Modifier.weight(1f))
                }
            }
            if (!vitals.observacoes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(vitals.observacoes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun VitalsRowInfo(label: String, value: String, unit: String, color: Color, status: VitalsStatus) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        VitalsMiniInfo(label, value, unit, color)
        if (status != VitalsStatus.NORMAL && status != VitalsStatus.DESCONHECIDO) {
            Surface(color = status.color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, status.color.copy(alpha = 0.5f))) {
                Text(status.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = status.color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VitalsMiniInfo(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
fun AddSpecificVitalsDialog(
    type: String,
    usuario: com.katui.offline.data.local.entities.Usuario? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double?, Int?, Int?, Int?, Int?, Int?, Int?, Double?, String?, java.time.LocalDateTime) -> Unit
) {
    var peso by remember { mutableStateOf(if (type == "Peso") usuario?.peso?.toString() ?: "" else "") }
    var altura by remember { mutableStateOf("") }
    var sistolica by remember { mutableStateOf("") }
    var diastolica by remember { mutableStateOf("") }
    var frequencia by remember { mutableStateOf("") }
    var glicose by remember { mutableStateOf("") }
    var saturacao by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    
    val finalDateTime = java.time.LocalDateTime.of(selectedDate, selectedTime)
    val isFuture = finalDateTime.isAfter(java.time.LocalDateTime.now())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(when (type) {
                    "Glicose" -> Icons.Default.Bloodtype; "Pressão" -> Icons.Default.Compress
                    "Peso" -> Icons.Default.MonitorWeight; "Altura" -> Icons.Default.Straighten
                    else -> Icons.Default.DeviceThermostat
                }, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (type == "Altura") "Registrar Altura" else "Registrar $type")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Seleção de Data e Hora
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateSelectorField(
                        label = "Data",
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        modifier = Modifier.weight(1f),
                        maxDate = LocalDate.now()
                    )
                    TimeSelectorField(
                        label = "Hora",
                        selectedTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                when (type) {
                    "Glicose" -> OutlinedTextField(
                        value = glicose, 
                        onValueChange = { glicose = it },
                        label = { Text("Glicose (mg/dL)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null) }
                    )
                    "Pressão" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sistolica, 
                            onValueChange = { sistolica = it },
                            label = { Text("Sistólica") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = diastolica, 
                            onValueChange = { diastolica = it },
                            label = { Text("Diastólica") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    "Batimentos" -> OutlinedTextField(
                        value = frequencia, 
                        onValueChange = { frequencia = it },
                        label = { Text("Batimentos (BPM)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.MonitorHeart, contentDescription = null) }
                    )
                    "Saturação" -> OutlinedTextField(
                        value = saturacao, 
                        onValueChange = { saturacao = it },
                        label = { Text("Saturação %") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Opacity, contentDescription = null) }
                    )
                    "Temperatura" -> OutlinedTextField(
                        value = temperatura,
                        onValueChange = { temperatura = it.replace(',', '.') },
                        label = { Text("Temperatura °C") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.DeviceThermostat, contentDescription = null) }
                    )
                    "Peso" -> OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it.replace(',', '.') },
                        label = { Text("Peso (kg)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) }
                    )
                    "Altura" -> {
                        Text("Acompanhamento de Crescimento", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = altura, 
                            onValueChange = { altura = it },
                            label = { Text("Altura (cm)") }, 
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) }
                        )
                    }
                }
                OutlinedTextField(
                    value = obs, 
                    onValueChange = { obs = it },
                    label = { Text("Observações (Opcional)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )

                if (isFuture) {
                    Text(
                        text = "Não é possível registrar dados no futuro.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        peso.replace(',', '.').toDoubleOrNull(), altura.toIntOrNull(),
                        sistolica.toIntOrNull(), diastolica.toIntOrNull(), frequencia.toIntOrNull(),
                        glicose.toIntOrNull(), saturacao.toIntOrNull(),
                        temperatura.replace(',', '.').toDoubleOrNull(), obs.ifBlank { null },
                        finalDateTime
                    )
                },
                enabled = !isFuture
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
