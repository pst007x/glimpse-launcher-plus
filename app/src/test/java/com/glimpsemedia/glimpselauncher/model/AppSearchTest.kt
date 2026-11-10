package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private val names = listOf("Google", "Gmail", "Maps", "Google Maps", "Calendar", "Chrome", "Camera", "1Password", "Zoom", "Contacts")
    private fun search(q: String) = searchApps(names, q) { it }

    @Test fun nothingTypedListsEveryAppAToZWithNumbersLast() {
        assertEquals(
            listOf("Calendar", "Camera", "Chrome", "Contacts", "Gmail", "Google", "Google Maps", "Maps", "Zoom", "1Password"),
            search(""),
        )
    }

    @Test fun eachLetterNarrowsTheListToNamesThatBeginWithIt() {
        assertEquals(listOf("Gmail", "Google", "Google Maps"), search("g"))
        assertEquals(listOf("Google", "Google Maps"), search("go"))
        assertEquals(listOf("Google", "Google Maps"), search("goo"))
        assertEquals(listOf("Google Maps"), search("google m"))
    }

    @Test fun capitalsAndSpacesAroundTheWordDoNotMatter() {
        assertEquals(listOf("Calendar", "Camera"), search("  CAL".trim()).take(1) + search("cam"))
        assertEquals(search("go"), search(" GO "))
    }

    @Test fun ifNoNameBeginsWithItAWordInTheNameDoes() {
        assertEquals(listOf("Maps"), search("map")) // a name that begins with it comes first, alone
        assertEquals(listOf("Google Maps"), searchApps(listOf("Google Maps", "Camera"), "map") { it })
    }

    @Test fun ifNoWordBeginsWithItAnythingContainingItIsShown() {
        assertEquals(listOf("Calendar"), search("lend"))
    }

    @Test fun nothingMatchingGivesAnEmptyList() {
        assertEquals(emptyList<String>(), search("qqq"))
    }

    @Test fun headingsAreLettersWithHashForNumbers() {
        assertEquals("G", initialOf("google"))
        assertEquals("#", initialOf("1Password"))
        assertEquals("#", initialOf(" "))
        assertEquals(
            listOf("C" to listOf("Calendar", "Camera"), "G" to listOf("Gmail")),
            groupedByInitial(listOf("Calendar", "Camera", "Gmail")) { it },
        )
    }
}
