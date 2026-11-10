package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.shaded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockInkTest {
    @Test fun theStandardWhiteClockIsShadedToo() {
        // No colour of its own, no panel: white, then 30% darker.
        assertEquals(shaded(0xFFFFFFFF.toInt(), -30), clockInk(clockColor = 0, clockShade = -30, onPanel = false, panel = 0))
    }

    @Test fun theAutomaticInkOnAPanelIsShadedToo() {
        // A light panel gives black ink; lighter by 40% is a grey.
        assertEquals(shaded(0xFF000000.toInt(), 40), clockInk(clockColor = 0, clockShade = 40, onPanel = true, panel = 0xFFFAF8F5.toInt()))
    }

    @Test fun aChosenColourIsShaded() {
        assertEquals(shaded(0xFF336699.toInt(), 20), clockInk(clockColor = 0xFF336699.toInt(), clockShade = 20, onPanel = true, panel = 0xFF000000.toInt()))
    }

    @Test fun aRefusedWallpaperIsNotReportedAsChanged() {
        assertFalse(wallpaperWasSet(supported = false, allowed = true, id = 7))
        assertFalse(wallpaperWasSet(supported = true, allowed = false, id = 7))
        assertFalse(wallpaperWasSet(supported = true, allowed = true, id = 0))
        assertTrue(wallpaperWasSet(supported = true, allowed = true, id = 7))
    }
}
