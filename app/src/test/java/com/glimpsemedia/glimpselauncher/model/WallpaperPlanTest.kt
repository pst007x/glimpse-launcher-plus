package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.THEME_PRESETS
import com.glimpsemedia.glimpselauncher.data.currentThemeColours
import com.glimpsemedia.glimpselauncher.data.withTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPlanTest {
    @Test fun blendEndsWhereTheMixIsWhole() {
        val p = wallpaperPlan(1080, 2340, 1, 2, 3, 4, dark = true)
        assertEquals(1f, 0.35f * p.endX / 1080 + 0.65f * p.endY / 2340, 0.001f)
        assertEquals(1, p.from); assertEquals(2, p.to)
    }

    @Test fun glowsWhereThePreviewsHaveThem() {
        val p = wallpaperPlan(1080, 2340, 1, 2, 3, 4, dark = true)
        assertEquals(listOf(3, 4), p.glows.map { it.color })
        assertEquals(270f, p.glows[0].cx, 1f); assertEquals(1872f, p.glows[0].cy, 1f)
        assertEquals(972f, p.glows[1].cx, 1f); assertEquals(327.6f, p.glows[1].cy, 1f)
        assertEquals(626.4f, p.glows[0].radius, 1f)
        assertEquals(0.47f, p.glows[0].alpha, 0.001f)
        assertEquals(0.37f, wallpaperPlan(1080, 2340, 1, 2, 3, 4, dark = false).glows[1].alpha, 0.001f)
    }

    @Test fun sizeIsPortraitAndNeverZero() {
        val wide = wallpaperPlan(2340, 1080, 1, 2, 3, 4, true)
        assertEquals(1080, wide.width); assertEquals(2340, wide.height)
        val none = wallpaperPlan(0, 0, 1, 2, 3, 4, true)
        assertEquals(1080, none.width); assertEquals(2340, none.height)
    }

    @Test fun myColoursNowAreTheShadedSettings() {
        val s = Settings().withTheme(THEME_PRESETS.first { it.name == "Cyan" }).copy(tileShade = 20)
        assertEquals(listOf(0xFF0D0F12.toInt(), shaded(0xFF24282E.toInt(), 20), 0xFF00E5FF.toInt(), 0xFF2962FF.toInt()), s.currentThemeColours())
    }
}
