package com.example.ui.theme

import com.example.platform.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.ui.data.AppTheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BentoTitle,
    secondary = BrightCyan,
    tertiary = Pink,
    background = TextBlack,
    surface = TextBlack,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = White
)


private val OceanColorScheme = lightColorScheme(
    primary = Color(0xFF0277BD),
    secondary = Color(0xFF039BE5),
    tertiary = Color(0xFF4FC3F7),
    background = Color(0xFFE1F5FE),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF01579B),
    onSurface = Color(0xFF01579B)
)

private val ForestColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF43A047),
    tertiary = Color(0xFF81C784),
    background = Color(0xFFE8F5E9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20)
)

private val SunsetColorScheme = lightColorScheme(
    primary = Color(0xFFE64A19),
    secondary = Color(0xFFF4511E),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFFFBE9E7),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFBF360C),
    onSurface = Color(0xFFBF360C)
)

private val LightColorScheme = lightColorScheme(
    primary = BentoTitle,
    secondary = BrightCyan,
    tertiary = BrightGreen,
    background = BentoBackground,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = BentoTextPrimary,
    onSurface = BentoTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        appTheme == AppTheme.OCEAN -> OceanColorScheme
        appTheme == AppTheme.FOREST -> ForestColorScheme
        appTheme == AppTheme.SUNSET -> SunsetColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
