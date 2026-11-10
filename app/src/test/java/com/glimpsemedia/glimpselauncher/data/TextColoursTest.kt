package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TextColoursTest {
    private val blue = 0xFF2962FF.toInt()
    private val green = 0xFF00D68F.toInt()
    private val coloured = Settings(tileTextColor = blue, tileTextShade = 10, textModeColor = green, textModeShade = -5)

    @Test fun bothStartAutomatic() {
        val s = Settings()
        assertEquals(listOf(0, 0, 0, 0), listOf(s.tileTextColor, s.tileTextShade, s.textModeColor, s.textModeShade))
    }

    @Test fun aThemePutsBothBackToAutomatic() {
        val s = coloured.withTheme(THEME_PRESETS.first())
        assertEquals(listOf(0, 0, 0, 0), listOf(s.tileTextColor, s.tileTextShade, s.textModeColor, s.textModeShade))
    }

    @Test fun aSavedThemeKeepsBothColours() {
        val back = Settings().withLook(coloured.look())
        assertEquals(listOf(blue, 10, green, -5), listOf(back.tileTextColor, back.tileTextShade, back.textModeColor, back.textModeShade))
        val t = SavedThemes(listOf(SavedTheme("t1", "Mine", coloured.look(), null, null)))
        assertEquals(t, SavedThemesJson.decode(SavedThemesJson.encode(t)))
    }

    @Test fun olderSavedThemesReadAutomatic() {
        val old = """{"v":1,"themes":[{"id":"t1","n":"Old","look":{"dark":true,"tile":0,"bar":0,"clock":0,"highlight":0}}]}"""
        val l = SavedThemesJson.decode(old)!!.list[0].look
        assertEquals(listOf(0, 0, 0, 0), listOf(l.tileTextColor, l.tileTextShade, l.textModeColor, l.textModeShade))
    }
}
