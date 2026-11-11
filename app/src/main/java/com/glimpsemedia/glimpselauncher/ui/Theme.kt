package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.shaded
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Tiles are dark with white text whatever the wallpaper is, so they can always be read. */
object GlimpseColors {
    val Tile = Color(0xE61B2430)
    val TileText = Color.White
    val Bar = Color(0xF2121A24)
    val Accent = Color(0xFFFFC857)
}

/** How opaque a tile is for a transparency of [percent]: 0 is solid, 100 is not drawn at all. */
fun tileOpacity(percent: Int): Float = (100 - percent.coerceIn(0, 100)) / 100f

/** Black or white text, whichever reads better on [c]. */
fun readableOn(c: Color): Color = if (c.copy(alpha = 1f).luminance() > 0.45f) Color.Black else Color.White

/** The colours the set-up person can change: the tiles and the bottom bar, each with readable text. */
data class Palette(
    val tile: Color, val tileText: Color, val bar: Color, val barText: Color, val tileTransparency: Int = 0,
    /** The square behind each icon: solid colour and how see-through it is (100 hides it). */
    val icon: Color = Color(0xFF2C3A4B), val iconTransparency: Int = 100,
) {
    companion object {
        const val STANDARD = 0

        /** [tile] and [bar] are ARGB numbers, or [STANDARD] for the colours that go with the light or dark theme. */
        fun of(
            tile: Int, bar: Int, dark: Boolean, tileTransparency: Int = 0, barTransparency: Int = 0, icon: Int = STANDARD, iconTransparency: Int = 100,
            /** Each colour made darker (−) or lighter (+), applied after the standard colour is chosen. */
            tileShade: Int = 0, barShade: Int = 0, iconShade: Int = 0,
        ): Palette {
            fun text(c: Color) = if (c.copy(alpha = 1f).luminance() > 0.45f) Color.Black else Color.White
            // The tile colour itself is solid; how see-through it is comes from the transparency setting.
            val t = Color(shaded(if (tile == STANDARD) (if (dark) 0xFF1B2430.toInt() else 0xFFFFFFFF.toInt()) else tile, tileShade)).copy(alpha = 1f)
            val solidBar = Color(shaded(if (bar == STANDARD) (if (dark) 0xFF121A24.toInt() else 0xFFE9EDF2.toInt()) else bar, barShade)).copy(alpha = 1f)
            val solidIcon = Color(shaded(if (icon == STANDARD) (if (dark) 0xFF2C3A4B.toInt() else 0xFFDDE3EA.toInt()) else icon, iconShade)).copy(alpha = 1f)
            return Palette(t, text(t), solidBar.copy(alpha = tileOpacity(barTransparency)), text(solidBar), tileTransparency, solidIcon, iconTransparency)
        }
    }
}

/** The highlight colour: selections, chosen swatches, drop targets, buttons, a touched icon. */
val LocalAccent = compositionLocalOf { GlimpseColors.Accent }

val LocalPalette = compositionLocalOf { Palette.of(Palette.STANDARD, Palette.STANDARD, true) }

/** The colours of the menus, lists and settings: dark or light. */
data class MenuColors(
    val background: Color,
    val surface: Color,
    val button: Color,
    val dialog: Color,
    val text: Color,
    val dimText: Color,
    val welcome: Color,
)

val DarkMenu = MenuColors(
    background = Color(0xFF101820), surface = Color(0xFF1B2430), button = Color(0xFF2C3A4B), dialog = Color(0xFF121A24),
    text = Color.White, dimText = Color(0xFFB8C2CC), welcome = Color(0xFF0B3C5D),
)
val LightMenu = MenuColors(
    background = Color(0xFFF4F6F9), surface = Color(0xFFFFFFFF), button = Color(0xFFDDE3EA), dialog = Color(0xFFFFFFFF),
    text = Color(0xFF101820), dimText = Color(0xFF52606D), welcome = Color(0xFFE8F1F8),
)
val LocalMenu = compositionLocalOf { DarkMenu }

/** The hues of the colour chart, each from dark to light (18 hues of 7 shades). */
val ColourGrid: List<List<Int>> = listOf(
    listOf(0xFF2D2A79.toInt(), 0xFF312E89.toInt(), 0xFF3A3093.toInt(), 0xFF4B3E9A.toInt(), 0xFF5E4FA2.toInt(), 0xFF7B6AB0.toInt(), 0xFFA995CA.toInt()),
    listOf(0xFF253980.toInt(), 0xFF223E92.toInt(), 0xFF24429C.toInt(), 0xFF2D56A6.toInt(), 0xFF4B6BB6.toInt(), 0xFF708DC7.toInt(), 0xFF899DCF.toInt()),
    listOf(0xFF272F7A.toInt(), 0xFF283289.toInt(), 0xFF293696.toInt(), 0xFF35439C.toInt(), 0xFF4959A6.toInt(), 0xFF708DC7.toInt(), 0xFF9CAFDA.toInt()),
    listOf(0xFF094D8C.toInt(), 0xFF0957A2.toInt(), 0xFF1A5FAD.toInt(), 0xFF1576BB.toInt(), 0xFF458CCC.toInt(), 0xFF6AA6DA.toInt(), 0xFF95C1E6.toInt()),
    listOf(0xFF11679A.toInt(), 0xFF1073AA.toInt(), 0xFF137FBF.toInt(), 0xFF1E91CA.toInt(), 0xFF3FA3D4.toInt(), 0xFF6EBADA.toInt(), 0xFF99CFE7.toInt()),
    listOf(0xFF0E80A2.toInt(), 0xFF1592B2.toInt(), 0xFF0EA0C5.toInt(), 0xFF11AECF.toInt(), 0xFF46BED7.toInt(), 0xFF70CFE3.toInt(), 0xFFA0DBEB.toInt()),
    listOf(0xFF0E9553.toInt(), 0xFF16A85F.toInt(), 0xFF26B566.toInt(), 0xFF3CBB78.toInt(), 0xFF6DC59D.toInt(), 0xFF7BC8A6.toInt(), 0xFFA1D7BF.toInt()),
    listOf(0xFF148C44.toInt(), 0xFF189D4A.toInt(), 0xFF21AF4B.toInt(), 0xFF38B64A.toInt(), 0xFF5DBB4B.toInt(), 0xFF7DC66C.toInt(), 0xFFA5D599.toInt()),
    listOf(0xFF3B9A46.toInt(), 0xFF41A948.toInt(), 0xFF48B848.toInt(), 0xFF6CBD47.toInt(), 0xFF85C451.toInt(), 0xFF9CCF72.toInt(), 0xFFAFCF90.toInt()),
    listOf(0xFF6FA33F.toInt(), 0xFF7EB540.toInt(), 0xFF89C541.toInt(), 0xFF9ECC38.toInt(), 0xFFABD34E.toInt(), 0xFFC3DD72.toInt(), 0xFFE9EDA3.toInt()),
    listOf(0xFFCECC2A.toInt(), 0xFFE7E517.toInt(), 0xFFF3EC18.toInt(), 0xFFF5ED32.toInt(), 0xFFF5EF59.toInt(), 0xFFF8F284.toInt(), 0xFFFAF6AF.toInt()),
    listOf(0xFFCE8C2B.toInt(), 0xFFE39D23.toInt(), 0xFFFBAA1A.toInt(), 0xFFFCBA28.toInt(), 0xFFFDC854.toInt(), 0xFFFFD67E.toInt(), 0xFFFEE3AC.toInt()),
    listOf(0xFFCE5028.toInt(), 0xFFE65A25.toInt(), 0xFFF5781E.toInt(), 0xFFF67B2E.toInt(), 0xFFF79558.toInt(), 0xFFF9B07D.toInt(), 0xFFFED2AB.toInt()),
    listOf(0xFFCD2026.toInt(), 0xFFE71E26.toInt(), 0xFFEC1D23.toInt(), 0xFFEF3133.toInt(), 0xFFEF5658.toInt(), 0xFFF28183.toInt(), 0xFFF9A7AB.toInt()),
    listOf(0xFFB31E3B.toInt(), 0xFFCD1F44.toInt(), 0xFFE5194A.toInt(), 0xFFEB2364.toInt(), 0xFFEC4F84.toInt(), 0xFFEE779F.toInt(), 0xFFF7A4C0.toInt()),
    listOf(0xFFA02168.toInt(), 0xFFB52074.toInt(), 0xFFC31A7F.toInt(), 0xFFCB1D8C.toInt(), 0xFFD14E9E.toInt(), 0xFFD976AF.toInt(), 0xFFEA9CC4.toInt()),
    listOf(0xFF8A288D.toInt(), 0xFF962C91.toInt(), 0xFF9F3894.toInt(), 0xFFA1419A.toInt(), 0xFF824F9E.toInt(), 0xFFB16FAF.toInt(), 0xFFD6A2C9.toInt()),
    listOf(0xFF502B7B.toInt(), 0xFF592D8E.toInt(), 0xFF612D91.toInt(), 0xFF723D97.toInt(), 0xFF824F9E.toInt(), 0xFF9A6BAF.toInt(), 0xFFBB97C7.toInt()),
)

/** Plain colours for the tiles, the bar and the clock: white, greys and black. */
val GreyChoices: List<Pair<String, Int>> = listOf(
    "White" to 0xFFFFFFFF.toInt(),
    "Light grey" to 0xFFD6DAE0.toInt(),
    "Grey" to 0xFF9AA0A6.toInt(),
    "Dark grey" to 0xFF3A3F47.toInt(),
    "Black" to 0xFF000000.toInt(),
)

@Composable
fun GlimpseTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = darkColorScheme(primary = GlimpseColors.Accent, surface = GlimpseColors.Tile),
        content = content,
    )
}

/**
 * Whether a wallpaper was really set: a phone that doesn't support wallpapers, or a managed one that forbids
 * changing it, refuses quietly (no error, an id of 0), so all three must be checked.
 */
fun wallpaperWasSet(supported: Boolean, allowed: Boolean, id: Int): Boolean = supported && allowed && id != 0

/**
 * How menus and pop-ups (Settings, menus, choosers, Help, app lists) size their text, against the phone's own text
 * size: step 0 (the usual) is 90%, slightly smaller than the home screens; the person can choose 80% to 140%.
 */
fun menuTextScale(step: Int): Float = MENU_SCALES[step.coerceIn(-1, 4) + 1]
private val MENU_SCALES = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.25f, 1.4f)

/** The menu text scale chosen in Settings, given by the home screen. */
val LocalMenuTextScale = androidx.compose.runtime.compositionLocalOf { menuTextScale(0) }

private val LocalMenuTextScaled = androidx.compose.runtime.compositionLocalOf { false }

/** Draws [content] with menu-sized text; a menu inside a menu is not made smaller twice. */
@androidx.compose.runtime.Composable
fun MenuText(content: @androidx.compose.runtime.Composable () -> Unit) {
    if (LocalMenuTextScaled.current) {
        content()
    } else {
        val d = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, d.fontScale * LocalMenuTextScale.current),
            LocalMenuTextScaled provides true,
        ) { content() }
    }
}
