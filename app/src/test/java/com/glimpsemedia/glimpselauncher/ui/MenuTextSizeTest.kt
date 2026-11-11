package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.data.MENU_TEXT_STEPS
import com.glimpsemedia.glimpselauncher.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuTextSizeTest {
    @Test fun stepsAndScales() {
        assertEquals(-1..4, MENU_TEXT_STEPS)
        assertEquals(0, Settings().menuTextStep)
        assertEquals(listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.25f, 1.4f), MENU_TEXT_STEPS.map { menuTextScale(it) })
        assertEquals(0.8f, menuTextScale(-5)); assertEquals(1.4f, menuTextScale(9))
    }

    @Test fun buttonsPerRowFitTheWidth() {
        assertEquals(2, buttonsPerRow(360f, 3, 120f, 8f))
        assertEquals(3, buttonsPerRow(400f, 3, 120f, 8f))
        assertEquals(1, buttonsPerRow(100f, 3, 120f, 8f))   // never fewer than one
        assertEquals(2, buttonsPerRow(1000f, 2, 120f, 8f))  // never more than there are
    }

    @Test fun rowsAreBalanced() {
        assertEquals(listOf(3, 2), balancedRows(5, 3))
        assertEquals(listOf(2, 1), balancedRows(3, 2))
        assertEquals(listOf(2, 2), balancedRows(4, 3))
        assertEquals(listOf(3), balancedRows(3, 3))
        assertEquals(emptyList<Int>(), balancedRows(0, 2))
    }

    @Test fun buttonWidthFollowsTheLongestLabel() {
        val short = minButtonWidth(listOf("Dark", "Light"), 20f)
        val long = minButtonWidth(listOf("Dark", "Choose a picture"), 20f)
        assertTrue(long > short)
        assertTrue(short >= 48f)
        assertTrue(minButtonWidth(listOf("Light"), 16f) < minButtonWidth(listOf("Light"), 20f))
    }
}

class ButtonBelowTest {
    // A name with a button beside it: the button goes under the name when the name would be squeezed.
    @Test fun buttonGoesBelowWhenNarrow() {
        assertEquals(false, buttonBelow(385f, 0.9f))   // this phone, usual menu text
        assertEquals(true, buttonBelow(385f, 1.4f))    // this phone, +4
        assertEquals(false, buttonBelow(321f, 0.9f))   // a small phone, usual text
        assertEquals(true, buttonBelow(321f, 1.1f))    // a small phone, +2
    }
}
