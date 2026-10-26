package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode
import java.util.Calendar

data class LifeOsThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surfaceGlass: Color,
    val surfaceGlassHighlight: Color,
    val borderGlass: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val ambientGlow: Color
)

val LocalLifeOsColors = staticCompositionLocalOf {
    LifeOsThemeColors(
        isDark = true,
        background = GraphiteDark,
        surfaceGlass = DayInk.Surface,
        surfaceGlassHighlight = DayInk.Raised,
        borderGlass = DayInk.Line,
        primaryAccent = IrisPrimary,
        secondaryAccent = IrisRadiant,
        textPrimary = DayInk.Text,
        textSecondary = DayInk.Muted,
        textTertiary = DayInk.Quiet,
        ambientGlow = IrisGlow
    )
}

private val NightGraphiteScheme = darkColorScheme(
    primary = IrisRadiant,
    onPrimary = InkDeep,
    primaryContainer = GraphiteSurface,
    onPrimaryContainer = IrisSoft,
    secondary = IrisSoft,
    onSecondary = InkDeep,
    background = GraphiteDark,
    onBackground = Color.White,
    surface = GraphiteCard,
    onSurface = Color.White,
    surfaceVariant = GraphiteSurface,
    onSurfaceVariant = DayInk.Muted,
    outline = GraphiteBorder
)

private val SunsetAfternoonScheme = darkColorScheme(
    primary = SunsetAmber,
    onPrimary = Color(0xFF261200),
    primaryContainer = SunsetOrange,
    onPrimaryContainer = Color.White,
    secondary = SunsetWarmGlow,
    onSecondary = Color.White,
    background = SunsetBackground,
    onBackground = Color(0xFFFFFBEB),
    surface = SunsetSurface,
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = Color(0xFF33241C),
    onSurfaceVariant = Color(0xFFFDE68A),
    outline = SunsetBorder
)

private val DayCrispScheme = lightColorScheme(
    primary = DayIris,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = DayIris,
    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,
    background = DayBackground,
    onBackground = DayTextPrimary,
    surface = DaySurface,
    onSurface = DayTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = DayTextSecondary,
    outline = DayBorder
)

fun resolveActiveTheme(mode: AppThemeMode): Pair<ColorScheme, LifeOsThemeColors> {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val effectiveMode = when (mode) {
        AppThemeMode.AUTO_TIME -> {
            when {
                currentHour in 6..13 -> AppThemeMode.LIGHT_DAY
                currentHour in 14..19 -> AppThemeMode.SUNSET_AFTERNOON
                else -> AppThemeMode.NIGHT_GRAPHITE
            }
        }
        else -> mode
    }

    return when (effectiveMode) {
        AppThemeMode.LIGHT_DAY -> {
            DayCrispScheme to LifeOsThemeColors(
                isDark = false,
                background = DayBackground,
                surfaceGlass = Color(0xCCFFFFFF),
                surfaceGlassHighlight = Color(0x66FFFFFF),
                borderGlass = Color(0x334F46E5),
                primaryAccent = DayIris,
                secondaryAccent = Color(0xFF6366F1),
                textPrimary = DayTextPrimary,
                textSecondary = DayTextSecondary,
                textTertiary = Color(0xFF94A3B8),
                ambientGlow = Color(0x1A4F46E5)
            )
        }
        AppThemeMode.SUNSET_AFTERNOON -> {
            SunsetAfternoonScheme to LifeOsThemeColors(
                isDark = true,
                background = SunsetBackground,
                surfaceGlass = Color(0x99271E18),
                surfaceGlassHighlight = Color(0x33F59E0B),
                borderGlass = Color(0x4DF59E0B),
                primaryAccent = SunsetAmber,
                secondaryAccent = SunsetOrange,
                textPrimary = Color(0xFFFFFBEB),
                textSecondary = Color(0xFFFDE68A),
                textTertiary = Color(0xFFD97706),
                ambientGlow = SunsetWarmGlow
            )
        }
        AppThemeMode.NIGHT_GRAPHITE -> {
            NightGraphiteScheme to LifeOsThemeColors(
                isDark = true,
                background = GraphiteDark,
                surfaceGlass = DayInk.Surface,
                surfaceGlassHighlight = DayInk.Raised,
                borderGlass = DayInk.Line,
                primaryAccent = IrisRadiant,
                secondaryAccent = IrisPrimary,
                textPrimary = DayInk.Text,
                textSecondary = DayInk.Muted,
                textTertiary = DayInk.Quiet,
                ambientGlow = IrisGlow
            )
        }
        else -> {
            NightGraphiteScheme to LifeOsThemeColors(
                isDark = true,
                background = GraphiteDark,
                surfaceGlass = DayInk.Surface,
                surfaceGlassHighlight = DayInk.Raised,
                borderGlass = DayInk.Line,
                primaryAccent = IrisRadiant,
                secondaryAccent = IrisPrimary,
                textPrimary = DayInk.Text,
                textSecondary = DayInk.Muted,
                textTertiary = DayInk.Quiet,
                ambientGlow = IrisGlow
            )
        }
    }
}

@Composable
fun LifeOsTheme(
    mode: AppThemeMode = AppThemeMode.AUTO_TIME,
    content: @Composable () -> Unit
) {
    val (colorScheme, customColors) = resolveActiveTheme(mode)

    CompositionLocalProvider(LocalLifeOsColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
