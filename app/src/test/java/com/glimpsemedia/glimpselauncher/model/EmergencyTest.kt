package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyTest {
    @Test fun aNumberNeedsAtLeastThreeDigits() {
        assertTrue(Contact("Anna", "07700 900111").isUsable)
        assertTrue(Contact("Short", "112").isUsable)
        assertFalse(Contact("Nobody", "  ").isUsable)
        assertFalse(Contact("Bad", "12").isUsable)
    }

    @Test fun theEmergencyIconIsKeptOnceAndSaved() {
        val l = Layout(listOf(Page(listOf(Item.App("a")))), emptyList()).add(Item.Emergency)
        assertTrue(l.hasEmergencyIcon)
        assertEquals(l, l.add(Item.Emergency))
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
        assertEquals("emergency", Item.Emergency.colorKey)
    }
}
