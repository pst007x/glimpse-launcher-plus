package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetsTest {
    @Test fun fourLightThenFourDark() {
        assertEquals(listOf("Coral", "Sage", "Indigo", "Sand", "Cyan", "Violet", "Neon", "Gold"), THEME_PRESETS.map { it.name })
        assertEquals(listOf(false, false, false, false, true, true, true, true), THEME_PRESETS.map { it.dark })
    }

    @Test fun exactColours() {
        val cyan = THEME_PRESETS.first { it.name == "Cyan" }
        assertEquals(listOf(0xFF0D0F12, 0xFF24282E, 0xFF00E5FF, 0xFF2962FF).map { it.toInt() }, listOf(cyan.c1, cyan.c2, cyan.c3, cyan.c4))
        val coral = THEME_PRESETS.first { it.name == "Coral" }
        assertEquals(listOf(0xFFFAF8F5, 0xFFF3E9D2, 0xFFFF6F61, 0xFFFF9F1C).map { it.toInt() }, listOf(coral.c1, coral.c2, coral.c3, coral.c4))
    }

    @Test fun aThemeSetsExactlyItsColours() {
        val before = Settings(showClock = false, autoLockMinutes = 30, clockStyle = 2, tileShade = 20, barTransparency = 75, iconTransparency = 100)
        for (p in THEME_PRESETS) {
            val s = before.withTheme(p)
            assertEquals(p.dark, s.darkTheme)
            assertEquals(p.c1, s.barColor); assertEquals(p.c1, s.clockBackColor)
            assertEquals(p.c2, s.tileColor); assertEquals(p.c3, s.highlightColor); assertEquals(p.c4, s.iconColor)
            assertEquals(if (p.dark) 0xFFFFFFFF.toInt() else 0xFF101820.toInt(), s.clockColor)
            assertTrue(s.clockBackground); assertEquals(50, s.clockBackTransparency)
            assertEquals(listOf(0, 0, 0, 0), listOf(s.tileTransparency, s.barTransparency, s.iconTransparency, s.clockTransparency))
            assertEquals(listOf(0, 0, 0, 0, 0, 0), listOf(s.tileShade, s.iconShade, s.barShade, s.clockShade, s.clockBackShade, s.highlightShade))
            assertEquals(false, s.showClock); assertEquals(30, s.autoLockMinutes); assertEquals(2, s.clockStyle)
            assertTrue(s.isTheme(p)); assertFalse(before.isTheme(p))
        }
    }

    @Test fun noTwoThemesAreTheSame() {
        for (a in THEME_PRESETS) for (b in THEME_PRESETS) if (a != b) assertFalse(Settings().withTheme(a).isTheme(b))
    }
}
