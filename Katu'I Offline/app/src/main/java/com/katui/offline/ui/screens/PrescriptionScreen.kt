package com.katui.offline.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.katui.offline.data.local.entities.Receita
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.ui.components.FullScreenImageDialog
import com.katui.offline.ui.components.MonthlyCalendar
import com.katui.offline.ui.components.PdfViewerDialog
import com.katui.offline.ui.viewmodel.PrescriptionViewModel
import com.katui.offline.util.TimeScope
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScreen(
    onNavigateToImport: () -> Unit,
    viewModel: PrescriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val receitas by viewModel.filteredReceitas.collectAsState()
    val allReceitas by viewModel.receitas.collectAsState()
    val usuario by viewModel.usuario.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val timeScope by viewModel.timeScope.collectAsState()
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var pdfViewerPath by remember { mutableStateOf<String?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }

    // Launcher para PDF direto na tela de receitas
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addReceitaFromPdf(context, it, null) }
        fabExpanded = false
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (fabExpanded) {
                    // Opção: PDF direto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Importar PDF",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Importar PDF")
                        }
                    }
                    // Opção: foto/IA
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Escanear com IA",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { fabExpanded = false; onNavigateToImport() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Escanear Receita")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Adicionar receita"
                    )
                }
            }
        }
    ) { padding ->
        // FIX: antes o calendário ficava fixo (fora do grid), sobrando pouco espaço pra
        // lista de receitas rolar. Agora o calendário entra como item de largura total
        // dentro do próprio LazyVerticalGrid (mesma ideia aplicada em ExameScreen), e tudo
        // rola junto — mesmo padrão usado no FemaleHealthScreen.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                // Título Integrado
                val title = if (usuario?.cuidadorId != null || (usuario?.tipo != null && usuario?.tipo != TipoUsuario.CUIDADOR)) {
                    "Receitas de ${usuario?.nome?.split(" ")?.firstOrNull() ?: ""}"
                } else {
                    "Suas Receitas"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp, start = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            item(span = { GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Seletor de Escopo Temporal
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
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
                                allReceitas.any { receita ->
                                    val rDate = receita.dataRegistro.toLocalDate()
                                    if (timeScope == TimeScope.DAY) rDate == date
                                    else rDate.year == date.year && rDate.month == date.month
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            if (receitas.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (timeScope) {
                                TimeScope.ALL -> "Nenhuma receita registrada."
                                TimeScope.MONTH -> "Nenhuma receita para este mês."
                                TimeScope.DAY -> "Nenhuma receita para este dia."
                            }
                        )
                    }
                }
            } else {
                items(receitas) { receita ->
                    PrescriptionCard(
                        receita = receita,
                        onDelete = { viewModel.deleteReceita(receita) },
                        onImageClick = { fullScreenImageUri = it },
                        onPdfClick = { pdfViewerPath = it }
                    )
                }
            }
        }
    }

    fullScreenImageUri?.let { uri ->
        FullScreenImageDialog(imageUri = uri, onDismiss = { fullScreenImageUri = null })
    }

    pdfViewerPath?.let { path ->
        PdfViewerDialog(filePath = path, onDismiss = { pdfViewerPath = null })
    }
}

@Composable
fun PrescriptionCard(
    receita: Receita,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    onPdfClick: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Receita?") },
            text = { Text("Tem certeza que deseja excluir esta receita? Esta ação não pode ser desfeita.") },
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                receita.imagemPath?.let { path ->
                    if (receita.isPdf) {
                        // Banner clicável para PDF
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPdfClick(path) },
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "PDF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = java.io.File(path),
                            contentDescription = "Imagem da receita",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onImageClick(path) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = receita.dataRegistro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                receita.observacao?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
