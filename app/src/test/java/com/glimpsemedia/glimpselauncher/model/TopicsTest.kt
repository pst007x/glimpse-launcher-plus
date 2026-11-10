package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicsTest {
    private data class App(val name: String, val topics: Set<Topic>)

    private val apps = listOf(
        App("Samsung Internet", setOf(Topic.INTERNET)),
        App("Chrome", setOf(Topic.INTERNET)),
        App("Brave", setOf(Topic.INTERNET)),
        App("Facebook", setOf(Topic.SOCIAL)),
        App("WhatsApp", setOf(Topic.SOCIAL, Topic.MESSAGES)),
        App("Messages", setOf(Topic.MESSAGES)),
        App("Maps", setOf(Topic.MAPS)),
        App("Calendar", emptySet()),
    )

    private fun related(q: String, already: List<App> = searchApps(apps, q) { it.name }) =
        relatedApps(apps, q, already, { it.name }, { it.topics }).map { (t, list) -> t to list.map { it.name } }

    @Test fun internetFindsTheBrowsersAfterTheOneNamedInternet() {
        // "Samsung Internet" has the word internet, so it narrows the search; the others follow as related apps.
        assertEquals(listOf("Samsung Internet"), searchApps(apps, "internet") { it.name }.map { it.name })
        assertEquals(listOf(Topic.INTERNET to listOf("Brave", "Chrome")), related("internet"))
    }

    @Test fun socialListsTheSocialAppsEvenThoughNoNameMatches() {
        assertEquals(emptyList<String>(), searchApps(apps, "social") { it.name }.map { it.name })
        assertEquals(listOf(Topic.SOCIAL to listOf("Facebook", "WhatsApp")), related("social"))
    }

    @Test fun halfAWordIsEnoughButOneLetterIsNot() {
        assertTrue(topicsFor("soc").contains(Topic.SOCIAL))
        assertTrue(topicsFor("inte").contains(Topic.INTERNET))
        assertEquals(emptyList<Topic>(), topicsFor("s"))
        assertEquals(emptyList<Topic>(), topicsFor(" "))
    }

    @Test fun anAppInTwoTopicsIsListedUnderBoth() {
        val r = related("chat").toMap()
        assertEquals(listOf("Facebook", "WhatsApp"), r[Topic.SOCIAL])
        assertEquals(listOf("Messages", "WhatsApp"), r[Topic.MESSAGES])
    }

    @Test fun appsAlreadyShownAreNotRepeated() {
        assertEquals(listOf(Topic.MAPS to emptyList<String>()).filter { it.second.isNotEmpty() }, related("map"))
        assertEquals(emptyList<Pair<Topic, List<String>>>(), related("zzz"))
    }

    @Test fun capitalsDoNotMatter() {
        assertEquals(related("web"), related("WEB"))
    }
}
