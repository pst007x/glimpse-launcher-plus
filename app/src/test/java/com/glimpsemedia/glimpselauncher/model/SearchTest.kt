package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchTest {
    @Test fun startsWithBeforeContains() {
        val names = listOf("Maps", "Google Maps", "Mail", "Cinema", "Camera")
        assertEquals(listOf("Mail", "Maps", "Cinema", "Google Maps"), rankByName(names, " ma ") { it })
    }

    @Test fun matchingIgnoresCapitals() {
        assertEquals(listOf("MAPS"), rankByName(listOf("MAPS", "Zoom"), "maps") { it })
    }

    @Test fun emptyQueryGivesNothing() {
        assertEquals(emptyList<String>(), rankByName(listOf("Maps"), "  ") { it })
    }

    @Test fun specialCharactersAreLiteral() {
        assertEquals(listOf("C++ notes"), rankByName(listOf("C++ notes", "Cat"), "c++") { it })
        assertEquals(listOf("50% off"), rankByName(listOf("50% off", "Cat"), "50%") { it })
    }

    @Test fun helpTitleBeforeText() {
        val topics = listOf("Locking" to "Set a PIN to keep it safe.", "PIN pad" to "Type the PIN.", "Swipes" to "Swipe down.")
        val ranked = rankHelp(topics, "pin", { it.first }, { it.second }).map { it.first }
        assertEquals(listOf("PIN pad", "Locking"), ranked)
    }

    @Test fun sectionLimitsAndExpands() {
        val ranked = (1..12).map { "App $it" }
        val s = section(SearchHeading.Apps, ranked, more = 0)!!
        assertEquals(8, s.shown.size)
        assertEquals(12, s.total)
        assertEquals(12, section(SearchHeading.Apps, ranked, more = 1)!!.shown.size)
        assertNull(section(SearchHeading.Apps, emptyList<String>(), more = 0))
    }

    @Test fun headingTitlesAndLimits() {
        assertEquals(listOf("Apps", "Files", "Folders", "Menu items", "Help items", "Online"), SearchHeading.entries.map { it.title })
        assertEquals(listOf(8, 10, 10, 8, 6), SearchHeading.entries.dropLast(1).map { it.limit })
    }
}
