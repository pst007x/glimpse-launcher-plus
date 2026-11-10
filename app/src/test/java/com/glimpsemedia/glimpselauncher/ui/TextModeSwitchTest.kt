package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.glimpsemedia.glimpselauncher.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class TextModeSwitchTest {
    @Test fun theLookFollowsTheModeAndTheme() {
        val off = switchLook(Settings(textMode = false))
        val on = switchLook(Settings(textMode = true, highlightColor = 0xFF00E5FF.toInt()))
        assertEquals("Text mode: Off", off.label); assertEquals("Text mode: On", on.label)
        assertEquals(0xFF, off.back ushr 24)
        assertEquals(0xFF00E5FF.toInt(), on.ink)
        assertEquals(readableOn(Color(off.back)).toArgb(), off.ink)
    }

    @Test fun standardHighlightIsTheYellow() {
        assertEquals(GlimpseColors.Accent.toArgb(), switchLook(Settings(textMode = true)).ink)
    }

    @Test fun anUnreadableHighlightFallsBackToReadableInk() {
        // Dark red highlight on the dark standard bar: too little contrast, so On uses the bar's readable ink.
        val look = switchLook(Settings(textMode = true, highlightColor = 0xFF8B0000.toInt()))
        assertEquals(readableOn(Color(look.back)).toArgb(), look.ink)
    }

    @Test fun textModeHasTheTextScreenThenTheWidgetsScreen() {
        assertEquals(2, homePageCount(textMode = true, tilePages = 5, ghostPage = true))
        assertEquals(6, homePageCount(textMode = false, tilePages = 5, ghostPage = true))
        assertEquals(5, homePageCount(textMode = false, tilePages = 5, ghostPage = false))
    }
}
