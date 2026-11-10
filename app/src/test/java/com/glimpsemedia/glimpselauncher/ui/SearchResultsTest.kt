package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.SearchEngine
import com.glimpsemedia.glimpselauncher.model.SearchHeading
import com.glimpsemedia.glimpselauncher.model.WalkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultsTest {
    private val ddg = SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=%s")
    private val wiki = SearchEngine("Wiki", "https://en.wikipedia.org/w/index.php?search=%s")
    private val files = WalkResult(
        listOf(Entry("Notes", true, "n", ""), Entry("notes.txt", false, "t", "Notes")), cut = false, failedRoots = emptySet(),
    )
    private val input = SearchInput(
        apps = listOf(AppName("a/Notes", "Notes"), AppName("b/Maps", "Maps")),
        files = files,
        help = listOf("Notes" to "Swipe right for your notes.", "Swipes" to "Swipe down."),
        engines = listOf(ddg, wiki),
    )

    @Test fun headingOrderFixed() {
        val headings = buildResults(input, "notes", emptyMap()).map { it.heading }
        assertEquals(listOf(SearchHeading.Apps, SearchHeading.Files, SearchHeading.Folders, SearchHeading.Menu, SearchHeading.Help, SearchHeading.Online), headings)
    }

    @Test fun filesHiddenWhenOff() {
        val headings = buildResults(input.copy(files = null), "notes", emptyMap()).map { it.heading }
        assertTrue(SearchHeading.Files !in headings && SearchHeading.Folders !in headings)
    }

    @Test fun onlineOnlyWithQueryAndEngines() {
        assertTrue(buildResults(input, "  ", emptyMap()).isEmpty())
        assertTrue(buildResults(input.copy(engines = emptyList()), "maps", emptyMap()).none { it.heading == SearchHeading.Online })
    }

    @Test fun onlineLinePerEngineInOrder() {
        val online = buildResults(input, "zzz", emptyMap()).single { it.heading == SearchHeading.Online }
        assertEquals(
            listOf(SearchHit.Online(ddg, "https://duckduckgo.com/?q=zzz"), SearchHit.Online(wiki, "https://en.wikipedia.org/w/index.php?search=zzz")),
            online.shown,
        )
    }

    @Test fun menuItemsFromSettings() {
        val menu = buildResults(input, "hold all apps", emptyMap()).single { it.heading == SearchHeading.Menu }
        assertEquals(SearchHit.Menu(com.glimpsemedia.glimpselauncher.ui.SettingsHit("Hold All apps to search", "search", "Hold All apps to search")), menu.shown.first())
    }

    @Test fun loadMoreAddsFiftyAtATime() {
        val many = input.copy(apps = (1..120).map { AppName("k$it", "Note $it") })
        fun shown(more: Int) = buildResults(many, "note", mapOf(SearchHeading.Apps to more)).first().shown.size
        assertEquals(listOf(8, 58, 108, 120), listOf(0, 1, 2, 3).map(::shown))
        assertEquals(120, buildResults(many, "note", emptyMap()).first().total)
        assertEquals(50, com.glimpsemedia.glimpselauncher.model.LOAD_MORE)
    }
}

class SearchWordingTest {
    @Test fun hintReadsAsOneList() {
        assertEquals("Type to search apps, settings and help", searchHint(folders = false, web = false))
        assertEquals("Type to search apps, settings, help and files", searchHint(folders = true, web = false))
        assertEquals("Type to search apps, settings, help and the web", searchHint(folders = false, web = true))
        assertEquals("Type to search apps, settings, help, files and the web", searchHint(folders = true, web = true))
    }

    @Test fun helpAndAboutOpenWithoutUnlock() {
        assertTrue(opensWithoutUnlock("help") && opensWithoutUnlock("about"))
        assertTrue(!opensWithoutUnlock("search") && !opensWithoutUnlock("lock"))
    }
}
