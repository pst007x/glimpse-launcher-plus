package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.model.shaded

/** The WCAG contrast ratio of two solid colours (1 to 21). */
private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** Glimpse's own Text mode switch, by its provider ("package/class"). */
fun isTextModeSwitch(provider: String, packageName: String): Boolean =
    provider == "$packageName/com.glimpsemedia.glimpselauncher.TextModeSwitchWidget"

/** Flips text mode; given by the home screen so a tap on a placed switch works while unlocked too (it sits under the hold layer). */
val LocalToggleTextMode = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/** The Text mode switch widget's colours (ARGB) and words. */
data class SwitchLook(val back: Int, val ink: Int, val label: String)

/** The pill in the theme's bar colour (solid); "On" in the highlight colour, "Off" in the bar's readable text colour. */
fun switchLook(s: Settings): SwitchLook {
    val bar = Palette.of(s.tileColor, s.barColor, s.darkTheme, barShade = s.barShade).bar.copy(alpha = 1f)
    val highlight = Color(shaded((if (s.highlightColor == 0) GlimpseColors.Accent else Color(s.highlightColor)).copy(alpha = 1f).toArgb(), s.highlightShade))
    // A highlight that hardly shows on the bar (dark red on dark grey) gives way to the bar's readable ink.
    val onInk = if (contrast(highlight, bar) >= 3f) highlight else readableOn(bar)
    return SwitchLook(
        back = bar.toArgb(),
        ink = if (s.textMode) onInk.toArgb() else readableOn(bar).toArgb(),
        label = if (s.textMode) "Text mode: On" else "Text mode: Off",
    )
}
