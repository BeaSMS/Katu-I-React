package com.katui.offline.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.Gestacao
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.PregnancyViewModel
import com.katui.offline.util.PregnancyContentProvider
import com.katui.offline.util.PregnancyWeekInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyScreen(
    onBack: () -> Unit,
    onNavigateToSymptoms: () -> Unit = {},
    onNavigateToConsultations: () -> Unit = {},
    viewModel: PregnancyViewModel = hiltViewModel()
) {
    val gestacao by viewModel.gestacao.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gestacao?.nomeBebe?.let { "Acompanhando $it" } ?: "Minha Gestação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        gestacao?.let { g ->
            val dum = g.dataUltimaMenstruacao ?: LocalDate.now().minusWeeks(4)
            val currentWeek = PregnancyContentProvider.calculateGestationalWeek(dum)
            val weekInfo = PregnancyContentProvider.getWeekInfo(currentWeek)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header com Progresso
                PregnancyHeader(currentWeek, dum, g.dataPartoEstimada)

                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Desenvolvimento") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sintomas") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Agenda") })
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> DevelopmentTab(weekInfo)
                        1 -> SymptomsTab(weekInfo, onNavigateToSymptoms)
                        2 -> AgendaTab(currentWeek, dum, onNavigateToConsultations)
                    }
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PregnancyHeader(week: Int, dum: LocalDate, edd: LocalDate?) {
    val progress = week / 40f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = week.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " semanas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Início (DUM)", style = MaterialTheme.typography.labelSmall)
                    Text(dum.format(DateTimeFormatter.ofPattern("dd/MM/yy")), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Previsão de Parto", style = MaterialTheme.typography.labelSmall)
                    Text(edd?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "--", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DevelopmentTab(info: PregnancyWeekInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Tamanho aproximado:", style = MaterialTheme.typography.labelMedium)
                        Text(info.sizeComparison, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("O que está acontecendo:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }

        item {
            Text("Dicas para esta semana:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(info.recommendedActions) { action ->
            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(action, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SymptomsTab(info: PregnancyWeekInfo, onNavigateToSymptoms: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Sintomas comuns agora:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(info.commonSymptoms) { symptom ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(symptom, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onNavigateToSymptoms,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar como me sinto")
            }
        }
    }
}

@Composable
fun AgendaTab(week: Int, dum: LocalDate, onNavigateToConsultations: () -> Unit) {
    val trimester = PregnancyContentProvider.getTrimester(week)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Acompanhamento Médico - ${trimester}º Trimestre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val agenda = when (trimester) {
            1 -> listOf(
                "Consulta de Pré-natal (Mensal)",
                "Ultrassonografia Obstétrica Inicial",
                "Exames de Sangue e Urina (Rotina 1)",
                "Ultrassonografia Morfológica (11-13 semanas)"
            )
            2 -> listOf(
                "Consulta de Pré-natal (Mensal)",
                "Ultrassonografia Morfológica (18-22 semanas)",
                "Teste de Tolerância à Glicose (TOTG)",
                "Monitorar movimentos fetais"
            )
            else -> listOf(
                "Consultas Quinzenais (até 36 sem)",
                "Consultas Semanais (após 36 sem)",
                "Pesquisa de Estreptococo B (GBS)",
                "Preparação para o Parto"
            )
        }

        items(agenda) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToConsultations,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agendar Consulta de Pré-natal")
            }
        }
    }
}
