package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.shaded

/**
 * A ready-made theme from four colours: [c1] the bottom bar and the clock panel, [c2] the tiles, [c3] the highlight,
 * [c4] the square behind each icon. Light themes have light menus, dark themes dark ones.
 */
data class ThemePreset(val name: String, val dark: Boolean, val c1: Int, val c2: Int, val c3: Int, val c4: Int)

private fun theme(name: String, dark: Boolean, c1: Long, c2: Long, c3: Long, c4: Long) =
    ThemePreset(name, dark, c1.toInt(), c2.toInt(), c3.toInt(), c4.toInt())

/** Four light themes, then four dark ones. */
val THEME_PRESETS: List<ThemePreset> = listOf(
    theme("Coral", false, 0xFFFAF8F5, 0xFFF3E9D2, 0xFFFF6F61, 0xFFFF9F1C),
    theme("Sage", false, 0xFFF7F9F7, 0xFFE3E7E3, 0xFF8FA67A, 0xFF3EB489),
    theme("Indigo", false, 0xFFFFFFFF, 0xFFE6E8EC, 0xFF3F48CC, 0xFF1E6FFF),
    theme("Sand", false, 0xFFEBDDC5, 0xFFF6EEDC, 0xFFC9A227, 0xFFFFB000),
    theme("Cyan", true, 0xFF0D0F12, 0xFF24282E, 0xFF00E5FF, 0xFF2962FF),
    theme("Violet", true, 0xFF26282C, 0xFF0F1A3A, 0xFF7B2CBF, 0xFFA06CFF),
    theme("Neon", true, 0xFF2B2D31, 0xFF0E0F11, 0xFFFF2E88, 0xFFD500F9),
    theme("Gold", true, 0xFF0C0C0C, 0xFF2A2A2A, 0xFFD4AF37, 0xFFFFBF00),
)

/** The settings with this theme's colours; the clock style, names, grid, locking and the rest are kept. */
fun Settings.withTheme(p: ThemePreset): Settings = copy(
    darkTheme = p.dark,
    barColor = p.c1, clockBackColor = p.c1, tileColor = p.c2, highlightColor = p.c3, iconColor = p.c4,
    clockColor = if (p.dark) 0xFFFFFFFF.toInt() else 0xFF101820.toInt(),
    clockBackground = true, clockBackTransparency = 50,
    tileTransparency = 0, barTransparency = 0, iconTransparency = 0, clockTransparency = 0,
    tileShade = 0, iconShade = 0, barShade = 0, clockShade = 0, clockBackShade = 0, highlightShade = 0,
    // A theme's own look: the text colours go back to automatic.
    tileTextColor = 0, tileTextShade = 0, textModeColor = 0, textModeShade = 0,
)

/** Whether the settings are exactly this theme. */
fun Settings.isTheme(p: ThemePreset): Boolean = withTheme(p) == this

/**
 * "My colours now": the four theme colours from the current settings, standard colours resolved and shades applied —
 * the bar, the tiles, the highlight, the icon square.
 */
fun Settings.currentThemeColours(): List<Int> {
    fun pick(own: Int, darkStandard: Long, lightStandard: Long) = if (own != 0) own else (if (darkTheme) darkStandard else lightStandard).toInt()
    return listOf(
        shaded(pick(barColor, 0xFF121A24, 0xFFE9EDF2), barShade),
        shaded(pick(tileColor, 0xFF1B2430, 0xFFFFFFFF), tileShade),
        shaded(pick(highlightColor, 0xFFFFC857, 0xFFFFC857), highlightShade),
        shaded(pick(iconColor, 0xFF2C3A4B, 0xFFDDE3EA), iconShade),
    )
}
