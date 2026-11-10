package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NamesTest {
    @Test fun contentDescriptionKeepsTheNameWhenHidden() {
        assertEquals("Phone", tileContentDescription("Phone", 0))
        assertEquals("Messages, 3 new", tileContentDescription("Messages", 3))
    }

    @Test fun contentDescriptionSaysThereIsAHoldAction() {
        assertEquals("Torch, has a hold action", tileContentDescription("Torch", 0, hasHold = true))
        assertEquals("Messages, 3 new, has a hold action", tileContentDescription("Messages", 3, hasHold = true))
    }

    @Test fun holdTimeKeepsTheLongerAccessibilitySetting() {
        assertEquals(500L, holdTimeoutMs(400L))
        assertEquals(1500L, holdTimeoutMs(1500L))
    }
}
