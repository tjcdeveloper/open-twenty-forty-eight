package com.tjcdeveloper.open2048.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class OpenColors(
    val isDark: Boolean,
    val page: Color,
    val boardContainer: Color,
    val emptyCell: Color,
    val settingsCard: Color,
    val controlTrack: Color,
    val primaryButton: Color,
    val onPrimary: Color,
    val text: Color,
    val secondary: Color,
    val scoreCard: Color,
    val scoreLabel: Color,
    val scoreValue: Color,
    val disabledButton: Color,
    val disabledIcon: Color,
    val chipUnselectedOnPage: Color,
    val chipUnselectedOnCard: Color,
    val chipUnselectedText: Color,
    val link: Color,
    val logo: Color,
)

val LightOpenColors = OpenColors(
    isDark = false,
    page = Color(0xFFFAF8EF),
    boardContainer = Color(0xFFBBADA0),
    emptyCell = Color(0xFFCDC1B4),
    settingsCard = Color(0xFFF2ECE1),
    controlTrack = Color(0xFFEEE4DA),
    primaryButton = Color(0xFF8F7A66),
    onPrimary = Color(0xFFF9F6F2),
    text = Color(0xFF776E65),
    secondary = Color(0xFFA1937F),
    scoreCard = Color(0xFFBBADA0),
    scoreLabel = Color(0xFFEEE4DA),
    scoreValue = Color(0xFFFFFFFF),
    disabledButton = Color(0xFFD6C9BB),
    disabledIcon = Color(0xFFA99A89),
    chipUnselectedOnPage = Color(0xFFEEE4DA),
    chipUnselectedOnCard = Color(0xFFEEE4DA),
    chipUnselectedText = Color(0xFFA1937F),
    link = Color(0xFF8F7A66),
    logo = Color(0xFFEDC22E),
)

val DarkOpenColors = OpenColors(
    isDark = true,
    page = Color(0xFF000000),
    boardContainer = Color(0xFF1C1712),
    emptyCell = Color(0xFF2A251F),
    settingsCard = Color(0xFF16120E),
    controlTrack = Color(0xFF241F18),
    primaryButton = Color(0xFF8F7A66),
    onPrimary = Color(0xFFF9F6F2),
    text = Color(0xFFD8CFC2),
    secondary = Color(0xFF8D8579),
    scoreCard = Color(0xFF1C1712),
    scoreLabel = Color(0xFF8D8579),
    scoreValue = Color(0xFFF5EFE6),
    disabledButton = Color(0xFF2A241D),
    disabledIcon = Color(0xFF6F665A),
    chipUnselectedOnPage = Color(0xFF1C1712),
    chipUnselectedOnCard = Color(0xFF241F18),
    chipUnselectedText = Color(0xFF8D8579),
    link = Color(0xFFC4A183),
    logo = Color(0xFFEDC22E),
)

val LocalOpenColors = staticCompositionLocalOf { LightOpenColors }

/** Classic 2048 tile ramp: background to text color. */
fun tileColors(value: Int, isDark: Boolean): Pair<Color, Color> =
    if (isDark) darkTileColors(value) else lightTileColors(value)

private fun lightTileColors(value: Int): Pair<Color, Color> = when (value) {
    2 -> Color(0xFFEEE4DA) to Color(0xFF776E65)
    4 -> Color(0xFFEDE0C8) to Color(0xFF776E65)
    8 -> Color(0xFFF2B179) to Color(0xFFF9F6F2)
    16 -> Color(0xFFF59563) to Color(0xFFF9F6F2)
    32 -> Color(0xFFF67C5F) to Color(0xFFF9F6F2)
    64 -> Color(0xFFF65E3B) to Color(0xFFF9F6F2)
    128 -> Color(0xFFEDCF72) to Color(0xFFF9F6F2)
    256 -> Color(0xFFEDCC61) to Color(0xFFF9F6F2)
    512 -> Color(0xFFEDC850) to Color(0xFFF9F6F2)
    1024 -> Color(0xFFEDC53F) to Color(0xFFF9F6F2)
    2048 -> Color(0xFFEDC22E) to Color(0xFFF9F6F2)
    else -> Color(0xFF3C3A32) to Color(0xFFF9F6F2)
}

/** Same hues as the light ramp but dimmed for OLED dark so 4 -> 8 is not jarring. */
private fun darkTileColors(value: Int): Pair<Color, Color> = when (value) {
    2 -> Color(0xFF3C3830) to Color(0xFFD6CDC1)
    4 -> Color(0xFF4A4336) to Color(0xFFE0D5C2)
    8 -> Color(0xFFA06A3C) to Color(0xFFF9F6F2)
    16 -> Color(0xFFA55A35) to Color(0xFFF9F6F2)
    32 -> Color(0xFFA84A33) to Color(0xFFF9F6F2)
    64 -> Color(0xFFA63A22) to Color(0xFFF9F6F2)
    128 -> Color(0xFFA08A35) to Color(0xFFF9F6F2)
    256 -> Color(0xFF9D852C) to Color(0xFFF9F6F2)
    512 -> Color(0xFF9A8024) to Color(0xFFF9F6F2)
    1024 -> Color(0xFF977B1C) to Color(0xFFF9F6F2)
    2048 -> Color(0xFF947614) to Color(0xFFF9F6F2)
    else -> Color(0xFF3C3A32) to Color(0xFFF9F6F2)
}

@Composable
fun Open2048Theme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkOpenColors else LightOpenColors
    val scheme = if (darkTheme) {
        darkColorScheme(surface = colors.page, background = colors.page, primary = colors.primaryButton)
    } else {
        lightColorScheme(surface = colors.page, background = colors.page, primary = colors.primaryButton)
    }
    CompositionLocalProvider(LocalOpenColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
