package com.katui.offline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun WeeklyCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    dayIntensityProvider: (LocalDate) -> Int, // 0 a 10
    // Cor de fundo opcional por data (ex: menstruação, dias férteis)
    dayBackground: ((LocalDate) -> Color)? = null
) {
    // Início da semana = domingo
    val startDate = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() % 7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..6).forEach { dayOffset ->
                val date = startDate.plusDays(dayOffset.toLong())
                val isSelected = date == selectedDate
                val intensity = dayIntensityProvider(date)
                val bgColor = dayBackground?.invoke(date)

                DayItem(
                    date = date,
                    isSelected = isSelected,
                    intensity = intensity,
                    extraBackground = bgColor,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    isSelected: Boolean,
    intensity: Int,
    extraBackground: Color? = null,
    onClick: () -> Unit
) {
    val dayName = date.dayOfWeek
        .getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
        .take(3)
    val dayNumber = date.dayOfMonth.toString()
    val isToday = date == LocalDate.now()

    val intensityColor = when {
        intensity == 0 -> Color.Transparent
        intensity <= 3 -> Color(0xFF4CAF50)  // Verde: Leve
        intensity <= 7 -> Color(0xFFFFC107)  // Amarelo: Moderado
        else -> Color(0xFFF44336)             // Vermelho: Grave
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = dayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected || isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                // Camada 1: cor feminina (menstruação / fertil) — fica abaixo da seleção
                .then(
                    if (extraBackground != null && extraBackground != Color.Transparent)
                        Modifier.background(extraBackground, CircleShape)
                    else Modifier
                )
                // Camada 2: seleção ou "hoje"
                .background(
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            // Indicador de intensidade de sintomas
            if (intensity > 0) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                        .background(intensityColor, CircleShape)
                )
            }
        }
    }
}
