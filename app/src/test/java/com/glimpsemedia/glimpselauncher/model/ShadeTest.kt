package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShadeTest {
    @Test fun zeroKeepsTheColour() { assertEquals(0xFF336699.toInt(), shaded(0xFF336699.toInt(), 0)) }

    @Test fun lighterAndDarkerGoHalfway() {
        assertEquals(0xFF808080.toInt(), shaded(0xFF000000.toInt(), 50))
        assertEquals(0xFF808080.toInt(), shaded(0xFFFFFFFF.toInt(), -50))
        assertEquals(0xFF991F1F.toInt(), shaded(0xFFFF3333.toInt(), -40))
    }

    @Test fun extremesAndClamping() {
        assertEquals(0xFF000000.toInt(), shaded(0xFF000000.toInt(), -50))
        assertEquals(0xFFFFFFFF.toInt(), shaded(0xFFFFFFFF.toInt(), 50))
        assertEquals(shaded(0xFF336699.toInt(), 50), shaded(0xFF336699.toInt(), 80))
        assertEquals(shaded(0xFF336699.toInt(), -50), shaded(0xFF336699.toInt(), -90))
        assertEquals(0x80, shaded(0x80336699.toInt(), 30) ushr 24)
    }

    @Test fun oldSettingsReadShadesAsZero() {
        val s = com.glimpsemedia.glimpselauncher.data.Settings()
        assertEquals(listOf(0, 0, 0, 0, 0, 0), listOf(s.tileShade, s.iconShade, s.barShade, s.clockShade, s.clockBackShade, s.highlightShade))
    }
}
