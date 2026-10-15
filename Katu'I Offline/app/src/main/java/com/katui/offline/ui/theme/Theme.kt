package com.katui.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.katui.offline.R

private val OpenDyslexic = FontFamily(
    Font(R.font.opendyslexic_regular, FontWeight.Normal),
    Font(R.font.opendyslexic_bold, FontWeight.Bold),
    Font(R.font.opendyslexic_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.opendyslexic_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

private val AbhayaLibre = FontFamily(
    Font(R.font.abhaya_libre_regular, FontWeight.Normal),
    Font(R.font.abhaya_libre_medium, FontWeight.Medium),
    Font(R.font.abhaya_libre_semibold, FontWeight.SemiBold),
    Font(R.font.abhaya_libre_bold, FontWeight.Bold),
    Font(R.font.abhaya_libre_extrabold, FontWeight.ExtraBold)
)

private val LightColorScheme = lightColorScheme(
    primary = CorPrincipal,
    onPrimary = Color.White,
    primaryContainer = CorSecundariaClaro,
    onPrimaryContainer = CorPrincipal,
    secondary = CorSecundaria,
    onSecondary = Color.White,
    background = CorFundo,
    surface = CorCard,
    onBackground = CorTexto,
    onSurface = CorTexto,
    error = CorErro,
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = CorTextoClaro
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D3C9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF9CF0E5),
    secondary = Color(0xFFB1CCC8),
    onSecondary = Color(0xFF1D3531),
    background = Color(0xFF191C1C),
    surface = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E2),
    onSurface = Color(0xFFE0E3E2),
    error = Color(0xFFFFB4AB),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBFC9C7)
)

private fun getDynamicColorScheme(seed: Color, isDark: Boolean): ColorScheme {
    val argb = seed.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    
    val hue = hsl[0]
    
    return if (isDark) {
        darkColorScheme(
            primary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.7f))),
            onPrimary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.1f))),
            primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.2f))),
            onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.4f, 0.9f))),
            secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.15f, 0.6f))),
            onSecondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.15f, 0.1f))),
            background = Color(0xFF191C1C), // Mantemos o fundo escuro padrão
            surface = Color(0xFF191C1C),
            onBackground = Color(0xFFE0E3E2),
            onSurface = Color(0xFFE0E3E2),
            error = Color(0xFFFFB4AB),
            surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.1f, 0.3f))),
            onSurfaceVariant = Color(0xFFBFC9C7)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.3f, 0.9f))),
            onPrimaryContainer = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.6f, 0.2f))),
            secondary = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.2f, 0.4f))),
            onSecondary = Color.White,
            background = CorFundo,
            surface = CorCard,
            onBackground = CorTexto,
            onSurface = CorTexto,
            error = CorErro,
            surfaceVariant = Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0.1f, 0.95f))),
            onSurfaceVariant = CorTextoClaro
        )
    }
}

private const val DYSLEXIC_FONT_SCALE = 0.85f
private const val ABHAYA_FONT_SCALE = 1.10f

@Composable
fun KatuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontSize: String = "Normal",
    dyslexiaFont: Boolean = false,
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        seedColor != null -> getDynamicColorScheme(seedColor, darkTheme)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val userSizeMultiplier = when (fontSize) {
        "Pequeno" -> 0.85f
        "Grande" -> 1.3f
        "Extra Grande" -> 1.6f
        else -> 1.0f
    }

    val fontSizeMultiplier = when {
        dyslexiaFont -> userSizeMultiplier * DYSLEXIC_FONT_SCALE
        else -> userSizeMultiplier * ABHAYA_FONT_SCALE
    }

    val lineHeightMultiplier = if (dyslexiaFont) 1.5f else 1.0f

    val family = if (dyslexiaFont) OpenDyslexic else AbhayaLibre

    val defaultTypography = Typography()

    val typography = Typography(
        displayLarge = defaultTypography.displayLarge.copy(
            fontFamily = family,
            fontSize = (defaultTypography.displayLarge.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.displayLarge.lineHeight.value * lineHeightMultiplier).sp
        ),
        displayMedium = defaultTypography.displayMedium.copy(
            fontFamily = family,
            fontSize = (defaultTypography.displayMedium.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.displayMedium.lineHeight.value * lineHeightMultiplier).sp
        ),
        displaySmall = defaultTypography.displaySmall.copy(
            fontFamily = family,
            fontSize = (defaultTypography.displaySmall.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.displaySmall.lineHeight.value * lineHeightMultiplier).sp
        ),
        headlineLarge = defaultTypography.headlineLarge.copy(
            fontFamily = family,
            fontSize = (defaultTypography.headlineLarge.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.headlineLarge.lineHeight.value * lineHeightMultiplier).sp
        ),
        headlineMedium = defaultTypography.headlineMedium.copy(
            fontFamily = family,
            fontSize = (defaultTypography.headlineMedium.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.headlineMedium.lineHeight.value * lineHeightMultiplier).sp
        ),
        headlineSmall = defaultTypography.headlineSmall.copy(
            fontFamily = family,
            fontSize = (defaultTypography.headlineSmall.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.headlineSmall.lineHeight.value * lineHeightMultiplier).sp
        ),
        titleLarge = defaultTypography.titleLarge.copy(
            fontFamily = family,
            fontSize = (defaultTypography.titleLarge.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.titleLarge.lineHeight.value * lineHeightMultiplier).sp
        ),
        titleMedium = defaultTypography.titleMedium.copy(
            fontFamily = family,
            fontSize = (defaultTypography.titleMedium.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.titleMedium.lineHeight.value * lineHeightMultiplier).sp
        ),
        titleSmall = defaultTypography.titleSmall.copy(
            fontFamily = family,
            fontSize = (defaultTypography.titleSmall.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.titleSmall.lineHeight.value * lineHeightMultiplier).sp
        ),
        bodyLarge = defaultTypography.bodyLarge.copy(
            fontFamily = family,
            fontSize = (defaultTypography.bodyLarge.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.bodyLarge.lineHeight.value * lineHeightMultiplier).sp
        ),
        bodyMedium = defaultTypography.bodyMedium.copy(
            fontFamily = family,
            fontSize = (defaultTypography.bodyMedium.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.bodyMedium.lineHeight.value * lineHeightMultiplier).sp
        ),
        bodySmall = defaultTypography.bodySmall.copy(
            fontFamily = family,
            fontSize = (defaultTypography.bodySmall.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.bodySmall.lineHeight.value * lineHeightMultiplier).sp
        ),
        labelLarge = defaultTypography.labelLarge.copy(
            fontFamily = family,
            fontSize = (defaultTypography.labelLarge.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.labelLarge.lineHeight.value * lineHeightMultiplier).sp
        ),
        labelMedium = defaultTypography.labelMedium.copy(
            fontFamily = family,
            fontSize = (defaultTypography.labelMedium.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.labelMedium.lineHeight.value * lineHeightMultiplier).sp
        ),
        labelSmall = defaultTypography.labelSmall.copy(
            fontFamily = family,
            fontSize = (defaultTypography.labelSmall.fontSize.value * fontSizeMultiplier).sp,
            lineHeight = (defaultTypography.labelSmall.lineHeight.value * lineHeightMultiplier).sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
