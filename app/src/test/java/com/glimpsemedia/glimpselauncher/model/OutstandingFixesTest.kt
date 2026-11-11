package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixes for the issues left over from earlier reviews. */
class OutstandingFixesTest {
    @Test fun startAgainKeepsTheGrid() {
        val l = DefaultLayout.build(listOf("a/A"), emptyList(), columns = 5, rows = 6)
        assertEquals(5, l.columns); assertEquals(6, l.rows)
        val f = DefaultLayout.buildFilled((1..40).map { "p$it/A" }, emptyList(), columns = 5, rows = 6)
        assertEquals(5 to 6, f.columns to f.rows)
        assertEquals(listOf(30, 10), f.pages.map { it.items.size })
    }

    @Test fun theWelcomeHighlightsOnlyTheGridThatIsSet() {
        assertTrue(welcomeGridPicked(4, 4, 4))
        assertFalse(welcomeGridPicked(4, 5, 4))
        assertFalse(welcomeGridPicked(5, 6, 4))
        assertTrue(welcomeGridPicked(2, 4, 2))
    }

    @Test fun webAddressesWithAPortOrInternationalNamesAreKept() {
        assertEquals("https://example.com:8080", webAddress("example.com:8080"))
        assertEquals("https://bücher.de", webAddress("bücher.de"))
        assertEquals("https://my_host.example.com", webAddress("my_host.example.com"))
        assertEquals("https://example.com", webAddress("example.com"))
        assertNull(webAddress("tel:123"))
        assertNull(webAddress("javascript:alert(1)"))
        assertNull(webAddress("nodot"))
        assertNull(webAddress("example.com:notaport"))
    }
}
