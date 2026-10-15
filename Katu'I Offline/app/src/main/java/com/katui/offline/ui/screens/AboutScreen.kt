package com.katui.offline.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katui.offline.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sobre o Katu'I",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = "Seu assistente de saúde inteligente, seguro e totalmente offline.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        HelpSection(
            title = "Gestão de Medicamentos",
            description = "Cadastre seus remédios com auxílio de um catálogo nacional de 300+ itens. O app utiliza autocomplete para facilitar o preenchimento de nomes e princípios ativos.",
            icon = Icons.Default.MedicalServices,
            color = AccentTeal
        )

        HelpSection(
            title = "Estoque Inteligente",
            description = "O sistema controla automaticamente a quantidade de remédios. Ao marcar um alarme como 'Tomado', a dose é subtraída do estoque e você recebe alertas de reposição quando estiver acabando.",
            icon = Icons.Default.Inventory,
            color = AccentOrange
        )

        HelpSection(
            title = "Diário de Sintomas e Sinais Vitais",
            description = "Registre pressão, glicose, temperatura, saturação e mais. Acompanhe a evolução em gráficos detalhados por hora, dia ou mês.",
            icon = Icons.Default.MonitorHeart,
            color = AccentPink
        )

        HelpSection(
            title = "Correlação Inteligente",
            description = "O Katu'I cruza seus dados silenciosamente para identificar padrões, como efeitos colaterais comuns de medicamentos ou sintomas recorrentes em fases do ciclo menstrual.",
            icon = Icons.Default.AutoGraph,
            color = AccentPurple
        )

        HelpSection(
            title = "Saúde Feminina",
            description = "Monitoramento completo do ciclo menstrual, previsões de período fértil e controle de anticoncepcionais, tudo integrado ao seu calendário de saúde.",
            icon = Icons.Default.Woman,
            color = AccentPink
        )

        HelpSection(
            title = "Segurança e Privacidade",
            description = "Todos os seus dados são criptografados e armazenados apenas no seu celular. Nenhuma informação de saúde sai do dispositivo.",
            icon = Icons.Default.Shield,
            color = AccentBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Perguntas Frequentes (FAQ)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        FaqSection()

        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Importante",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "O Katu'I é uma ferramenta de auxílio e não substitui o aconselhamento médico profissional. Sempre consulte seu médico antes de realizar alterações no seu tratamento.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun HelpSection(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class FaqItem(val question: String, val answer: String)

@Composable
fun FaqSection() {
    val faqItems = listOf(
        FaqItem(
            "Como faço para o alarme tocar?",
            "Basta cadastrar seu remédio na tela 'Remédios' e escolher o horário. O app vai tocar um som e mostrar uma tela cheia na hora exata. Se o seu celular for da marca Xiaomi, lembre-se de ativar o 'Início Automático' nas configurações para garantir que ele sempre funcione."
        ),
        FaqItem(
            "O que eu faço quando o remédio toca?",
            "Quando a tela do alarme aparecer, você deve deslizar o botão para confirmar que tomou o remédio. Se precisar de mais 5 minutinhos, clique em 'Adiar'. O app vai baixar uma unidade do seu estoque toda vez que você confirmar que tomou."
        ),
        FaqItem(
            "O que são as cores no calendário?",
            "No calendário da tela inicial, os pontos coloridos indicam o que aconteceu no dia: Azul para consultas, Verde para remédios tomados, e Laranja se você esqueceu algum. As cores de fundo (Rosa e Roxo) são para o acompanhamento do ciclo feminino."
        ),
        FaqItem(
            "Como registro minha pressão ou glicose?",
            "Vá na tela 'Sinais Vitais' (ícone de coração). Lá você pode anotar sua pressão, açúcar no sangue, peso e temperatura. Se você digitar a pressão como '12 por 8', o app entende sozinho e salva como 120 por 80."
        ),
        FaqItem(
            "Posso cuidar de outra pessoa pelo app?",
            "Sim! No menu 'Perfil', você pode adicionar seus dependentes (como marido, esposa ou netos). Depois, é só clicar no nome da pessoa no topo da tela inicial para trocar de perfil e ver os remédios e consultas dela."
        ),
        FaqItem(
            "Como coloco a foto da minha receita?",
            "Na tela de 'Remédios', clique no botão '+' e escolha 'Escanear Receita'. Você tira uma foto e, se tiver configurado a 'Chave da IA', o app lê os nomes dos remédios e horários sozinho para você."
        ),
        FaqItem(
            "Por que preciso dessa 'Chave da IA'?",
            "O Katu'I é focado em segurança e funciona todo dentro do seu celular. Não mandamos suas fotos para nossos computadores. Por isso, você usa sua própria chave gratuita do Google para que a inteligência artificial funcione com total privacidade."
        ),
        FaqItem(
            "Meus dados estão seguros?",
            "Sim! O Katu'I funciona 'offline', o que significa que nada sai do seu celular. Seus dados de saúde ficam guardados apenas com você. A única exceção é se você escolher fazer um backup no seu próprio Google Drive para não perder as informações se trocar de aparelho."
        ),
        FaqItem(
            "Onde vejo minhas próximas consultas?",
            "No 'Dashboard' (tela inicial), existe uma seção chamada 'Próximos compromissos'. Lá aparecem suas consultas e também os próximos remédios que você precisa tomar."
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        faqItems.forEach { item ->
            ExpandableFaqCard(item)
        }
    }
}

@Composable
fun ExpandableFaqCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (!expanded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
