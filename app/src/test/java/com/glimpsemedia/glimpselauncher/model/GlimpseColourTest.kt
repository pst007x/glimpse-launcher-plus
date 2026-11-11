package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlimpseColourTest {
    @Test fun kindToDrawing() {
        assertEquals("phone", gcDrawableFor(setOf(GcRole.PHONE)))
        assertEquals("message_circle", gcDrawableFor(setOf(GcRole.MESSAGES)))
        assertEquals("alarm_clock", gcDrawableFor(setOf(GcRole.CLOCK)))
        assertNull(gcDrawableFor(emptySet()))
    }

    @Test fun firstKindWins() = assertEquals("phone", gcDrawableFor(setOf(GcRole.CONTACTS, GcRole.PHONE)))   // a dialer that is also contacts

    @Test fun categoryToColour() {
        assertEquals(GC_PINK, gcColourFor(category = 4, topics = emptySet(), key = "a"))        // social
        assertEquals(GC_CYAN, gcColourFor(category = 1, topics = emptySet(), key = "a"))        // audio
        assertEquals(GC_YELLOW, gcColourFor(category = 0, topics = emptySet(), key = "a"))      // game
        assertEquals(GC_GREEN, gcColourFor(category = 6, topics = emptySet(), key = "a"))       // maps
        assertEquals(GC_PINK, gcColourFor(category = -1, topics = setOf(Topic.MESSAGES), key = "a"))
    }

    @Test fun keyColourStable() {
        val c = gcColourFor(-1, emptySet(), "com.some/.App")
        assertEquals(c, gcColourFor(-1, emptySet(), "com.some/.App"))
        assertTrue(c in listOf(GC_CYAN, GC_PINK, GC_YELLOW, GC_GREEN))
    }

    @Test fun noBrandLogos() {
        assertTrue(GC_ALL.none { it == "apple" || it == "bitcoin" })
    }

    @Test fun everyDrawingUsedIsBuiltIn() {
        val all = GC_ALL.toSet()
        assertTrue((GcRole.entries.map { it.drawable } + GC_BUILT_IN.values + GC_BADGES.values).all { it in all })
    }

    @Test fun everyMonoBadgeHasAColourOne() {
        assertEquals(TextIcon.entries.map { it.name }.toSet(), GC_BADGES.keys)
    }
}
