package com.katui.offline.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun MonthlyCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    collapsible: Boolean = true,
    showOnlyMonthSelector: Boolean = false,
    hasDataIndicator: (LocalDate) -> Boolean = { false },
    // Cor de fundo opcional por data (ex: menstruação, dias férteis)
    dayBackground: ((LocalDate) -> Color)? = null
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var isExpanded by remember { mutableStateOf(if (collapsible) (if (showOnlyMonthSelector) false else initialExpanded) else true) }

    // Sincroniza o mês exibido se a data selecionada mudar externamente (ex: switch de scope)
    LaunchedEffect(selectedDate) {
        currentMonth = YearMonth.from(selectedDate)
    }
    
    // Se mudarmos o mês via setas, atualizamos a data selecionada para o dia 1 desse mês
    // para que os filtros reajam à mudança de mês imediatamente.
    fun changeMonth(delta: Long) {
        currentMonth = currentMonth.plusMonths(delta)
        onDateSelected(currentMonth.atDay(1))
    }
    
    val headerText = remember(currentMonth, isExpanded, selectedDate) {
        if (isExpanded || showOnlyMonthSelector) {
            "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${currentMonth.year}"
        } else {
            val mes = selectedDate.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
            "$mes ${selectedDate.year}"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = collapsible) { isExpanded = !isExpanded }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded && collapsible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                }

                if (isExpanded || showOnlyMonthSelector) {
                    Row {
                        IconButton(onClick = { changeMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês Anterior")
                        }
                        IconButton(onClick = { changeMonth(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo Mês")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded && !showOnlyMonthSelector) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Cabeçalho dos dias da semana
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val daysInMonth = getDaysInMonth(currentMonth)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .height(240.dp)
                            .padding(top = 8.dp),
                        userScrollEnabled = false
                    ) {
                        items(daysInMonth) { date ->
                            if (date == null) {
                                Spacer(modifier = Modifier.fillMaxSize())
                            } else {
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()
                                val hasData = hasDataIndicator(date)
                                val bgColor = dayBackground?.invoke(date) ?: Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected || isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (hasData) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (collapsible && !showOnlyMonthSelector) {
                        TextButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recolher Calendário")
                        }
                    }
                }
            }

            if (!isExpanded && collapsible && !showOnlyMonthSelector) {
                Surface(
                    onClick = { isExpanded = true },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toque para expandir o calendário",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

private fun getDaysInMonth(yearMonth: YearMonth): List<LocalDate?> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOfFirstDay = firstDayOfMonth.dayOfWeek.value % 7 // 0 = domingo
    val daysInMonth = yearMonth.lengthOfMonth()

    val list = mutableListOf<LocalDate?>()
    repeat(dayOfWeekOfFirstDay) { list.add(null) }
    for (i in 1..daysInMonth) {
        list.add(yearMonth.atDay(i))
    }
    return list
}
