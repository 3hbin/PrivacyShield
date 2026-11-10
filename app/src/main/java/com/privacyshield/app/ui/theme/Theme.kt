package com.privacyshield.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.privacyshield.app.data.SeedColor
import com.privacyshield.app.data.ThemeMode

private val TealLight = lightColorScheme(
    primary = Color(0xFF0F6E56),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0DE),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF3D5A80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C30),
    tertiary = Color(0xFF8A4B12),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC3),
    onTertiaryContainer = Color(0xFF2F1500),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF6F7F6),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF6F7F6),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE5E1),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707974)
)

private val TealDark = darkColorScheme(
    primary = Color(0xFF8AD8C2),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005140),
    onPrimaryContainer = Color(0xFFB7F0DE),
    secondary = Color(0xFFB7C7E6),
    onSecondary = Color(0xFF213146),
    secondaryContainer = Color(0xFF38485E),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFFFB77D),
    onTertiary = Color(0xFF4E2600),
    tertiaryContainer = Color(0xFF6D3900),
    onTertiaryContainer = Color(0xFFFFDCC3),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C4),
    outline = Color(0xFF8A938E)
)

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF13193A),
    tertiary = Color(0xFF00695C),
    background = Color(0xFFF7F7FB),
    surface = Color(0xFFF7F7FB),
    surfaceVariant = Color(0xFFE2E1EC)
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF00218C),
    primaryContainer = Color(0xFF223590),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC0C6F5),
    onSecondary = Color(0xFF282F50),
    secondaryContainer = Color(0xFF3F4668),
    tertiary = Color(0xFF7DCEC0),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF45464F)
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0D3310),
    secondary = Color(0xFF558B2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    tertiary = Color(0xFF1565C0),
    background = Color(0xFFF5F7F4),
    surface = Color(0xFFF5F7F4),
    surfaceVariant = Color(0xFFDDE5DB)
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFFA5D6A7),
    onPrimary = Color(0xFF0D3310),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFC5E1A5),
    onSecondary = Color(0xFF213812),
    secondaryContainer = Color(0xFF33691E),
    tertiary = Color(0xFF90CAF9),
    background = Color(0xFF111411),
    surface = Color(0xFF111411),
    surfaceVariant = Color(0xFF414941)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

object StatusColors {
    val safe = Color(0xFF2E7D32)
    val attention = Color(0xFFEF6C00)
    val location = Color(0xFF1565C0)
}

@Composable
fun PrivacyShieldTheme(
    themeMode: ThemeMode,
    seedColor: SeedColor,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = when (seedColor) {
        SeedColor.TEAL -> if (dark) TealDark else TealLight
        SeedColor.INDIGO -> if (dark) IndigoDark else IndigoLight
        SeedColor.FOREST -> if (dark) ForestDark else ForestLight
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
