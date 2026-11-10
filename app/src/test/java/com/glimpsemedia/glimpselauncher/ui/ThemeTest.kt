package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test fun theChartHasEighteenHuesOfSevenShades() {
        assertEquals(18, ColourGrid.size)
        assertTrue(ColourGrid.all { it.size == 7 })
    }

    @Test fun standardColoursFollowTheTheme() {
        val dark = Palette.of(Palette.STANDARD, Palette.STANDARD, dark = true)
        val light = Palette.of(Palette.STANDARD, Palette.STANDARD, dark = false)
        assertEquals(Color.White, dark.tileText)
        assertEquals(Color.Black, light.tileText)
        assertEquals(Color.White, dark.barText)
        assertEquals(Color.Black, light.barText)
    }

    @Test fun aChosenColourKeepsItselfAndGetsReadableText() {
        val yellow = 0xFFFFD54F.toInt()
        val navy = 0xFF2D2A79.toInt()
        assertEquals(Color(yellow), Palette.of(yellow, Palette.STANDARD, true).tile)
        assertEquals(Color.Black, Palette.of(yellow, Palette.STANDARD, true).tileText)
        assertEquals(Color.White, Palette.of(navy, navy, false).tileText)
        assertEquals(Color.White, Palette.of(navy, navy, false).barText)
    }

    @Test fun everyChartColourGetsBlackOrWhiteText() {
        for (hue in ColourGrid) for (c in hue) {
            val text = Palette.of(c, c, true).tileText
            assertTrue(text == Color.Black || text == Color.White)
        }
    }
}

class TransparencyTest {
    @Test fun zeroPercentIsSolidAndOneHundredIsClear() {
        assertEquals(1f, tileOpacity(0), 0f)
        assertEquals(0.75f, tileOpacity(25), 0f)
        assertEquals(0.5f, tileOpacity(50), 0f)
        assertEquals(0.25f, tileOpacity(75), 0f)
        assertEquals(0f, tileOpacity(100), 0f)
    }

    @Test fun theTileColourItselfIsSolid() {
        assertEquals(1f, Palette.of(Palette.STANDARD, Palette.STANDARD, true).tile.alpha, 0f)
        assertEquals(1f, Palette.of(0x80FF0000.toInt(), Palette.STANDARD, true).tile.alpha, 0f)
        assertEquals(50, Palette.of(Palette.STANDARD, Palette.STANDARD, true, 50).tileTransparency)
    }
}

class BarTransparencyTest {
    @Test fun theBarCanBeSeeThroughAndKeepsReadableText() {
        val p = Palette.of(Palette.STANDARD, Palette.STANDARD, dark = true, tileTransparency = 0, barTransparency = 75)
        assertEquals(0.25f, p.bar.alpha, 0.01f)
        assertEquals(Color.White, p.barText)
        assertEquals(1f, Palette.of(Palette.STANDARD, Palette.STANDARD, dark = false).bar.alpha, 0f)
    }
}
