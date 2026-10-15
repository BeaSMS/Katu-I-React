package com.katui.offline.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.TipoUsuario
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.data.local.entities.SaudeItemCatalogo
import com.katui.offline.data.local.entities.TipoSaudeItem
import com.katui.offline.ui.theme.*
import com.katui.offline.ui.viewmodel.ProfileViewModel
import com.katui.offline.util.UiTextHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    isOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onClose: () -> Unit = {},
    // Quando preenchido, abre o perfil deste dependente para visualização/edição,
    // SEM trocar o usuário ativo da sessão (alarmes/medicamentos do app continuam
    // sendo os do usuário ativo, não deste).
    dependentIdToView: Long? = null
) {
    LaunchedEffect(dependentIdToView) {
        viewModel.setViewingUser(dependentIdToView)
    }
    val usuario by viewModel.usuario.collectAsState()
    val dependentes by viewModel.dependentes.collectAsState()
    val isViewingOwnProfile by viewModel.isViewingOwnProfile.collectAsState()
    val isActiveProfileDependent by viewModel.isActiveProfileDependent.collectAsState()
    var isEditing by remember { mutableStateOf(isOnboarding) }
    var showAddDependentDialog by remember { mutableStateOf(false) }
    var dependentToDelete by remember { mutableStateOf<Usuario?>(null) }
    val dependentSummary by viewModel.dependentSummary.collectAsState()
    val isDeletingDependent by viewModel.isDeletingDependent.collectAsState()
    val deleteDependentResult by viewModel.deleteDependentResult.collectAsState()

    LaunchedEffect(deleteDependentResult) {
        deleteDependentResult?.let { result ->
            if (result.isSuccess) {
                dependentToDelete = null
            }
            viewModel.resetDeleteDependentResult()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = usuario to isEditing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ProfileTransition"
        ) { (targetUsuario, editing) ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!editing) Modifier.verticalScroll(scrollState) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                targetUsuario?.let { user ->
                    ProfileHeader(
                        user = user,
                        isEditing = editing,
                        isViewingDependent = !isViewingOwnProfile,
                        onBack = {
                            if (editing) isEditing = false
                            else if (!isViewingOwnProfile) viewModel.setViewingUser(null) // sai da visualização, sem trocar a sessão
                            else onClose()
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (editing || (user.nome == "Cuidador Principal" && user.tipo == TipoUsuario.CUIDADOR)) {
                            EditProfileForm(
                                user = user,
                                onSave = { updatedUser ->
                                    viewModel.updateUsuario(updatedUser)
                                    isEditing = false
                                    if (isOnboarding) onOnboardingComplete()
                                },
                                onCancel = { if (!isOnboarding) isEditing = false }
                            )
                        } else {
                            // Seção: Informações Pessoais
                            ProfileSection(title = "Dados Pessoais", icon = Icons.Default.Badge) {
                                ProfileInfoRow(label = "Nome Completo", value = user.nome, icon = Icons.Default.Person)
                                ProfileInfoRow(label = "Idade", value = user.idade?.toString()?.let { "$it anos" } ?: "Não informado", icon = Icons.Default.Cake)
                                ProfileInfoRow(label = "Sexo Biológico", value = when (user.sexo) {
                                    SexoBiologico.FEMININO -> "Feminino"
                                    SexoBiologico.MASCULINO -> "Masculino"
                                    SexoBiologico.INTERSEXO -> "Intersexo"
                                    else -> "Não informado"
                                }, icon = when (user.sexo) {
                                    SexoBiologico.FEMININO -> Icons.Default.Female
                                    SexoBiologico.MASCULINO -> Icons.Default.Male
                                    SexoBiologico.INTERSEXO -> Icons.Default.Transgender
                                    else -> Icons.Default.Transgender
                                })
                            }

                            // Seção: Saúde e Biometria
                            ProfileSection(title = "Saúde e Biometria", icon = Icons.Default.HealthAndSafety) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileInfoRow(label = "Peso", value = user.peso?.toString()?.let { "$it kg" } ?: "--", icon = Icons.Default.MonitorWeight)
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileInfoRow(label = "Altura", value = user.alturaCm?.toString()?.let { "$it cm" } ?: "--", icon = Icons.Default.Height)
                                    }
                                }
                                ProfileInfoRow(label = "Alergias", value = user.alergias ?: "Nenhuma", icon = Icons.Default.Warning, isError = !user.alergias.isNullOrBlank() && user.alergias != "Nenhuma")
                                ProfileInfoRow(label = "Acompanhamento", value = user.condicoesEspeciais ?: "Nenhum", icon = Icons.Default.MedicalInformation)
                            }

                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar Perfil")
                            }

                            if (user.tipo == TipoUsuario.CUIDADOR) {
                                DependentsSection(
                                    dependentes = dependentes,
                                    onAddClick = { showAddDependentDialog = true },
                                    onEditDependent = { viewModel.setViewingUser(it.id) },
                                    onSwitchProfile = { viewModel.switchProfile(it.id) },
                                    onDeleteDependent = { dependentToDelete = it; viewModel.loadDependentSummary(it.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Botões de Ação do Perfil
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Só aparece quando o perfil ATIVO no app é um dependente
                                // (você trocou para ele). Permite voltar ao cuidador
                                // principal sem precisar abrir a gaveta.
                                if (isActiveProfileDependent && isViewingOwnProfile) {
                                    Button(
                                        onClick = { viewModel.switchToPrimaryProfile() },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.SwitchAccount, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Voltar ao Perfil Principal")
                                    }
                                }

                                OutlinedButton(
                                    onClick = onNavigateToAbout,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Help, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Sobre o App")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showAddDependentDialog) {
            AlertDialog(
                onDismissRequest = { showAddDependentDialog = false },
                title = { Text("Novo Dependente") },
                text = {
                    EditProfileForm(
                        user = Usuario(nome = "", tipo = TipoUsuario.PACIENTE),
                        onSave = { newDep ->
                            viewModel.addDependente(newDep)
                            showAddDependentDialog = false
                        },
                        onCancel = { showAddDependentDialog = false }
                    )
                },
                confirmButton = {}
            )
        }

        dependentToDelete?.let { dep ->
            DeleteDependentDialog(
                dependente = dep,
                summary = dependentSummary,
                isDeleting = isDeletingDependent,
                onConfirm = { viewModel.excluirDependente(dep.id) },
                onDismiss = {
                    dependentToDelete = null
                    viewModel.clearDependentSummary()
                }
            )
        }
    }
}

@Composable
fun ProfileHeader(user: Usuario, isEditing: Boolean, isViewingDependent: Boolean = false, onBack: () -> Unit) {
    val initials = user.nome.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifBlank { "?" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val primeiroNome = user.nome.split(" ").firstOrNull() ?: ""
            Text(
                text = if (user.nome == "Cuidador Principal" && isEditing) "Seu Perfil" else primeiroNome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {
                    isViewingDependent -> "Visualizando (perfil não trocado)"
                    user.tipo == TipoUsuario.CUIDADOR -> "Responsável Principal"
                    else -> "Perfil Gerenciado"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isViewingDependent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfileSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun DependentsSection(
    dependentes: List<Usuario>,
    onAddClick: () -> Unit,
    onEditDependent: (Usuario) -> Unit,
    onSwitchProfile: (Usuario) -> Unit,
    onDeleteDependent: (Usuario) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Dependentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        if (dependentes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "Nenhum dependente cadastrado.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dependentes.forEach { dep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dep.nome.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dep.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Gerenciar saúde", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Botão de Trocar Perfil (troca quem está ativo no app)
                            IconButton(onClick = { onSwitchProfile(dep) }) {
                                Icon(Icons.Default.SwitchAccount, contentDescription = "Trocar para este perfil", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Botão de Editar (abre o perfil do dependente para editar, SEM trocar quem está ativo)
                            IconButton(onClick = { onEditDependent(dep) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = MaterialTheme.colorScheme.outline)
                            }

                            // Botão de Excluir (apaga o dependente e todos os dados associados, com confirmação)
                            IconButton(onClick = { onDeleteDependent(dep) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir dependente", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteDependentDialog(
    dependente: Usuario,
    summary: com.katui.offline.util.DependentDataSummary?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var nomeDigitado by remember { mutableStateOf("") }
    val nomeConfere = nomeDigitado.trim().equals(dependente.nome.trim(), ignoreCase = true)

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Excluir ${dependente.nome}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Esta ação é permanente e não pode ser desfeita. Todos os dados de saúde deste dependente serão apagados:",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (summary == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Verificando dados associados...", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (summary.total == 0) {
                    Text("Nenhum dado adicional associado a este perfil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                "Medicamentos" to summary.medicamentos,
                                "Alarmes" to summary.alarmes,
                                "Consultas" to summary.consultas,
                                "Exames" to summary.exames,
                                "Receitas" to summary.receitas,
                                "Sintomas" to summary.sintomas,
                                "Vacinas" to summary.vacinas,
                                "Sinais vitais" to summary.vitals,
                                "Ciclos menstruais" to summary.ciclosMenstruais,
                                "Anticoncepcionais" to summary.anticoncepcionais
                            ).filter { it.second > 0 }.forEach { (label, count) ->
                                Text("• $count $label", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Text(
                    "Para confirmar, digite o nome completo \"${dependente.nome}\" abaixo:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = nomeDigitado,
                    onValueChange = { nomeDigitado = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting,
                    singleLine = true,
                    isError = nomeDigitado.isNotEmpty() && !nomeConfere,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = nomeConfere && !isDeleting && summary != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                } else {
                    Text("Excluir Definitivamente")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ProfileInfoRow(label: String, value: String, icon: ImageVector, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EditProfileForm(user: Usuario, onSave: (Usuario) -> Unit, onCancel: () -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val saudeCatalogo by viewModel.saudeCatalogo.collectAsState()
    
    var nome by remember { mutableStateOf(if (user.nome == "Cuidador Principal" && user.tipo == TipoUsuario.CUIDADOR) "" else user.nome) }
    var idade by remember { mutableStateOf(user.idade?.toString() ?: "") }
    var sexo by remember { mutableStateOf(user.sexo) }
    var peso by remember { mutableStateOf(user.peso?.toString() ?: "") }
    var alturaCm by remember { mutableStateOf(user.alturaCm?.toString() ?: "") }
    
    // Novas listas para Alergias e Condições (armazenadas como nomes em Usuario)
    var selectedAlergias by remember { mutableStateOf(user.alergias?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()) }
    var selectedCondicoes by remember { mutableStateOf(user.condicoesEspeciais?.split(", ")?.filter { it.isNotBlank() } ?: emptyList()) }
    
    var showAlergiaDialog by remember { mutableStateOf(false) }
    var showCondicaoDialog by remember { mutableStateOf(false) }
    var corSelecionada by remember { mutableStateOf(user.corHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: UiTextHelper.getUserColor(user)) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seletor de Cor Tema
        Column {
            Text("Cor do Perfil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UiTextHelper.getProfilePalette().forEach { cor ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cor)
                            .border(
                                width = if (corSelecionada == cor) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { corSelecionada = cor }
                    )
                }
            }
        }

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome Completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = idade,
                onValueChange = { idade = it },
                label = { Text("Idade") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )

            Column(modifier = Modifier.weight(2f)) {
                Text("Sexo Biológico", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sexo == SexoBiologico.FEMININO,
                        onClick = { sexo = SexoBiologico.FEMININO },
                        label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Female, contentDescription = "Feminino", modifier = Modifier.size(24.dp), tint = AccentPink) } },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    FilterChip(
                        selected = sexo == SexoBiologico.MASCULINO,
                        onClick = { sexo = SexoBiologico.MASCULINO },
                        label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Male, contentDescription = "Masculino", modifier = Modifier.size(24.dp), tint = AccentBlue) } },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    FilterChip(
                        selected = sexo == SexoBiologico.INTERSEXO,
                        onClick = { sexo = SexoBiologico.INTERSEXO },
                        label = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Transgender, contentDescription = "Intersexo", modifier = Modifier.size(24.dp), tint = AccentPurple) } },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = peso,
                onValueChange = { peso = it },
                label = { Text("Peso (kg)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = alturaCm,
                onValueChange = { alturaCm = it },
                label = { Text("Altura (cm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )
        }

        // Seção de Alergias (Catálogo)
        Column {
            Text("Alergias (Alimentos, Remédios, etc.)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedAlergias.forEach { alergia ->
                    InputChip(
                        selected = true,
                        onClick = { selectedAlergias = selectedAlergias.filter { it != alergia } },
                        label = { Text(alergia) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                AssistChip(
                    onClick = { showAlergiaDialog = true },
                    label = { Text("Adicionar Alergia") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Seção de Condições (Catálogo)
        Column {
            Text("Condições de Saúde (DCNT)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCondicoes.forEach { condicao ->
                    InputChip(
                        selected = true,
                        onClick = { selectedCondicoes = selectedCondicoes.filter { it != condicao } },
                        label = { Text(condicao) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                AssistChip(
                    onClick = { showCondicaoDialog = true },
                    label = { Text("Adicionar Condição") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    onSave(user.copy(
                        nome = nome,
                        idade = idade.toIntOrNull(),
                        sexo = sexo,
                        peso = peso.toDoubleOrNull(),
                        alturaCm = alturaCm.toIntOrNull(),
                        alergias = selectedAlergias.joinToString(", ").ifBlank { "Nenhuma" },
                        condicoesEspeciais = selectedCondicoes.joinToString(", ").ifBlank { "Nenhum" },
                        corHex = UiTextHelper.colorToHex(corSelecionada)
                    ))
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = nome.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Salvar")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancelar")
            }
        }
    }

    if (showAlergiaDialog) {
        SaudeSelectionDialog(
            title = "Selecionar Alergias",
            items = saudeCatalogo.filter { it.tipo == TipoSaudeItem.ALERGIA },
            selectedItems = selectedAlergias,
            onDismiss = { showAlergiaDialog = false },
            onConfirm = { selectedAlergias = it }
        )
    }

    if (showCondicaoDialog) {
        SaudeSelectionDialog(
            title = "Condições de Saúde",
            items = saudeCatalogo.filter { it.tipo == TipoSaudeItem.CONDICAO },
            selectedItems = selectedCondicoes,
            onDismiss = { showCondicaoDialog = false },
            onConfirm = { selectedCondicoes = it }
        )
    }
}

@Composable
fun SaudeSelectionDialog(
    title: String,
    items: List<SaudeItemCatalogo>,
    selectedItems: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedItems) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredItems = items.filter { 
        it.nome.contains(searchQuery, ignoreCase = true) || 
        it.categoria.contains(searchQuery, ignoreCase = true) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
                
                // NOTA: O diálogo aqui não usa DropdownMenu para a lista, usa LazyColumn.
                // LazyColumn não rouba o foco do teclado, então o fix focado em DropdownMenu
                // se aplica apenas onde o menu flutuante é usado.
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    val grouped = filteredItems.groupBy { it.categoria }
                    grouped.forEach { (categoria, categoryItems) ->
                        item {
                            Text(
                                text = categoria,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(categoryItems) { item ->
                            val isSelected = tempSelected.contains(item.nome)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        tempSelected = if (isSelected) tempSelected - item.nome else tempSelected + item.nome
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(item.nome)
                            }
                        }
                    }
                    
                    if (searchQuery.isNotBlank() && items.none { it.nome.equals(searchQuery, ignoreCase = true) }) {
                        item {
                            TextButton(onClick = { tempSelected = tempSelected + searchQuery; searchQuery = "" }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Adicionar personalizado: \"$searchQuery\"")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelected); onDismiss() }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
