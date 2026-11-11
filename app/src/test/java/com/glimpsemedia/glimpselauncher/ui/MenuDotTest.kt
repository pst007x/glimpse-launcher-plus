package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuDotTest {
    @Test fun everyGridGetsAFullSizeTouch() {
        for (c in 2..5) assertTrue(menuDotFor(c).touch >= 48.dp)
    }

    @Test fun compactGridsDrawASmallerDot() {
        assertEquals(30.dp, menuDotFor(4).circle)
        assertEquals(30.dp, menuDotFor(5).circle)
        assertEquals(48.dp, menuDotFor(2).circle)
    }

    @Test fun theDotFitsInsideItsTouch() {
        for (c in 2..5) assertTrue(menuDotFor(c).circle <= menuDotFor(c).touch)
    }

    @Test fun showPageDotsOnlyWithMoreThanOneScreen() {
        assertEquals(false, showPageDots(1))
        assertEquals(false, showPageDots(0))
        assertEquals(true, showPageDots(2))
    }
}
