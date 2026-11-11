package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.shaded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorRowTest {
    private val red = 0xFFCC3333.toInt()

    @Test fun theSwatchShowsTheColourWithItsShade() {
        assertEquals(shaded(red, -20), swatchArgb(red, null, -20))
        assertEquals(red, swatchArgb(red, null, 0))
    }

    @Test fun aStandardColourWithAKnownLookIsShadedToo() {
        val yellow = 0xFFFFC107.toInt()
        assertEquals(shaded(yellow, 15), swatchArgb(0, yellow, 15))
    }

    @Test fun aStandardWithNoKnownLookIsLeftToTheMenu() = assertNull(swatchArgb(0, null, 20))

    @Test fun theNoteSaysHowMuchDarkerOrLighter() {
        assertEquals("20% darker", shadeNote(-20))
        assertEquals("15% lighter", shadeNote(15))
        assertNull(shadeNote(0))
    }
}

class PickerSliderTest {
    @Test fun theFirstOwnColourBringsTheSliderIntoView() = assertEquals(true, showSliderAfterPick(0, 0xFF112233.toInt(), hasExtra = true))

    @Test fun changingOneOwnColourForAnotherLeavesThePlace() = assertEquals(false, showSliderAfterPick(0xFF445566.toInt(), 0xFF112233.toInt(), hasExtra = true))

    @Test fun pickersWithoutASliderNeverMove() = assertEquals(false, showSliderAfterPick(0, 0xFF112233.toInt(), hasExtra = false))

    @Test fun goingBackToStandardLeavesThePlace() = assertEquals(false, showSliderAfterPick(0xFF445566.toInt(), 0, hasExtra = true))
}
