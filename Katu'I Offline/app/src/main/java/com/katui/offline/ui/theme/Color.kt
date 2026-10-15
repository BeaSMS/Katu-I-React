package com.katui.offline.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Katui Design System Colors
// -------------------------

// Dashboard Summary & FAB Action Colors
// Consistent mapping across the app:
// Remédios: Teal (0xFF009688)
// Sintomas: Deep Purple / Violet (0xFF673AB7)
// Exames: Pink / Deep Pink (0xFFE91E63)
// Vacinas: Green (0xFF4CAF50)
// Consultas: Sky Blue / Cyan (0xFF00B0FF) - Updated to be more distinct from Purple
// Receitas: Orange (0xFFFF9800)
// Sinais Vitais: Red (0xFFF44336)
// Saúde Feminina: Light Pink / Magenta (0xFFFF4081)

val AccentTeal = Color(0xFF009688)      // Remédios
val AccentPurple = Color(0xFF673AB7)    // Sintomas
val AccentPink = Color(0xFFE91E63)      // Exames
val AccentGreen = Color(0xFF4CAF50)     // Vacinas
val AccentBlue = Color(0xFF00B0FF)       // Consultas (Updated to Sky Blue 0xFF00B0FF)
val AccentOrange = Color(0xFFFF9800)    // Receitas
val AccentRed = Color(0xFFF44336)       // Sinais Vitais
val AccentMagenta = Color(0xFFFF4081)   // Saúde Feminina

// Base Colors used in Theme.kt
val CorPrincipal = AccentTeal
val CorSecundaria = AccentBlue
val CorSecundariaClaro = Color(0xFFE0F2F1)
val CorFundo = Color(0xFFF8FBFB)
val CorCard = Color.White
val CorTexto = Color(0xFF191C1C)
val CorTextoClaro = Color(0xFF3F4948)
val CorErro = Color(0xFFBA1A1A)

// Specific Health Colors
val PrevisaoMenstruacao = Color(0xFFFFCDD2)
val EscalaSintomaBaixa = Color(0xFF8BC34A)
val EscalaSintomaMedia = Color(0xFFFFB300)
val EscalaSintomaAlta = Color(0xFFF44336)
val EscalaSintomaCritica = Color(0xFFB71C1C)
