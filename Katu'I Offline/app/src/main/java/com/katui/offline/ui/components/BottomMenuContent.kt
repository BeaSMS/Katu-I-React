package com.katui.offline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katui.offline.Screen
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.Usuario
import com.katui.offline.ui.viewmodel.DashboardViewModel
import com.katui.offline.util.UiTextHelper

@Composable
fun BottomMenuContent(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val usuarioAtual by viewModel.usuario.collectAsState()
    val dependentes by viewModel.dependentes.collectAsState()

    val isSaudeFemininaEnabled = usuarioAtual?.sexo == SexoBiologico.FEMININO || usuarioAtual?.sexo == SexoBiologico.INTERSEXO

    val healthItems = remember(isSaudeFemininaEnabled) {
        mutableListOf(
            Screen.Medications,
            Screen.Symptoms,
            Screen.Vitals,
            Screen.Consultations,
            Screen.Exames
        ).apply {
            if (isSaudeFemininaEnabled) {
                add(Screen.FemaleHealth)
            }
            add(Screen.Receitas)
            add(Screen.Vaccination)
        }
    }

    val systemItems = listOf(
        Screen.Settings,
        Screen.About
    )

    // Scroll em toda a parte interna da gaveta
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 48.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            text = "Menu Principal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 20.dp)
        )

        // Itens Principais CENTRALIZADOS
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            Box(modifier = Modifier.width(90.dp)) {
                MenuCircleItem(Screen.Dashboard, MaterialTheme.colorScheme.primary, onNavigate)
            }
            // Perfil (Agora junto com Home)
            Box(modifier = Modifier.width(90.dp)) {
                MenuCircleItem(Screen.Profile, MaterialTheme.colorScheme.secondary, onNavigate)
            }
            // Alarmes
            Box(modifier = Modifier.width(90.dp)) {
                MenuCircleItem(Screen.Alarms, MaterialTheme.colorScheme.tertiary, onNavigate)
            }
        }

        // SEÇÃO DE TROCA DE PERFIL
        // Mostramos se houver pelo menos 1 perfil (ele mesmo ou dependentes) 
        // para que a troca seja sempre acessível e visível.
        if (dependentes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Trocar perfil ativo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Toque em um nome para usar o app como essa pessoa",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Lista horizontal de perfis para troca rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                dependentes.forEach { user ->
                    ProfileShortcutItem(
                        user = user,
                        isActive = user.id == usuarioAtual?.id,
                        userColor = UiTextHelper.getUserColor(user),
                        onClick = { 
                            viewModel.switchProfile(user.id)
                            onDismiss() // Fecha a gaveta ao trocar
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text(
            text = "Área de Saúde",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        // Itens de Saúde em 2 colunas
        val rows = healthItems.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { screen ->
                    val color = when(screen) {
                        is Screen.Medications -> Color(0xFF009688)
                        is Screen.Symptoms -> Color(0xFF673AB7)
                        is Screen.Exames -> Color(0xFFE91E63)
                        is Screen.Vaccination -> Color(0xFF4CAF50)
                        is Screen.Consultations -> Color(0xFF00B0FF)
                        is Screen.Receitas -> Color(0xFFFF9800)
                        is Screen.Vitals -> Color(0xFFF44336)
                        is Screen.FemaleHealth -> Color(0xFFFF4081)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    val subtitle = when(screen) {
                        is Screen.Medications -> "Controle seus remédios"
                        is Screen.Symptoms -> "Registre o que sente"
                        is Screen.Exames -> "Histórico de exames"
                        is Screen.Vaccination -> "Carteira de vacinação"
                        is Screen.Consultations -> "Agendamentos médicos"
                        is Screen.Receitas -> "Receitas e prescrições"
                        is Screen.Vitals -> "Peso, PA e glicose"
                        is Screen.FemaleHealth -> "Ciclo e saúde íntima"
                        else -> ""
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MenuCardItem(screen, color, onNavigate, isLarge = true, subtitle = subtitle)
                    }
                }
                // Se a linha tiver apenas 1 item, preenche o espaço
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Itens de sistema
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            systemItems.forEach { screen ->
                Box(modifier = Modifier.weight(1f)) {
                    MenuCircleItem(screen, MaterialTheme.colorScheme.secondary, onNavigate, isExtraBold = true)
                }
            }
        }
    }
}

@Composable
private fun ProfileShortcutItem(
    user: Usuario,
    isActive: Boolean,
    userColor: Color,
    onClick: () -> Unit
) {
    // Detectamos se o tema é escuro para ajustar o contraste
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // A cor base é a cor do usuário, mas no modo escuro garantimos que não seja muito escura
    val baseColor = if (isActive) userColor else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isActive) baseColor else baseColor.copy(alpha = 0.3f)
    val containerBg = if (isActive) baseColor.copy(alpha = if (isDark) 0.25f else 0.15f) else Color.Transparent
    
    // Para o texto do nome, usamos a cor onSurface direta para máxima legibilidade
    val textColor = if (isActive) {
        if (isDark) Color.White else baseColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(
                width = if (isActive) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) baseColor else baseColor.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.nome.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = user.nome.split(" ").firstOrNull() ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MenuCircleItem(
    screen: Screen,
    color: Color,
    onNavigate: (String) -> Unit,
    isExtraBold: Boolean = false // Novo parâmetro para o sistema
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigate(screen.route) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = screen.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isExtraBold) FontWeight.ExtraBold else FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MenuCardItem(
    screen: Screen, 
    color: Color, 
    onNavigate: (String) -> Unit,
    isLarge: Boolean = false,
    subtitle: String? = null
) {
    // FIX: no tema escuro, fundo/borda com alpha baixo (0.08f/0.2f) ficavam quase
    // invisíveis contra o background escuro do app, e o subtítulo com alpha 0.7f
    // perdia contraste. Reforçamos fundo e borda no escuro, mantendo a mesma cor
    // de identidade de cada categoria. Detectamos o tema pelo brilho do background
    // REAL do tema ativo (não isSystemInDarkTheme), já que o app permite forçar
    // claro/escuro manualmente nas Configurações, podendo divergir do sistema.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerAlpha = if (isDark) 0.28f else 0.08f
    val borderAlpha = if (isDark) 0.55f else 0.2f
    val iconBgAlpha = if (isDark) 0.4f else 0.15f
    val subtitleAlpha = if (isDark) 0.85f else 0.7f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (isLarge) 1.05f else 1f) // Ajustado para dar um pouco mais de espaço vertical
            .clip(RoundedCornerShape(20.dp))
            .clickable { onNavigate(screen.route) },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = containerAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = borderAlpha))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = iconBgAlpha),
                modifier = Modifier.size(if (isLarge) 44.dp else 40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(if (isLarge) 24.dp else 22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = if (isLarge) 16.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = subtitleAlpha),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp, // Aumentado de 10sp para 12sp
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
