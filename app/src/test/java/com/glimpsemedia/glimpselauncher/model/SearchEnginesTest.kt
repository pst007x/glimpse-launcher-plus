package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEnginesTest {
    private val ddg = SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=%s")

    @Test fun noPresetEngines() {
        assertEquals(emptyList<SearchEngine>(), SearchListsJson.engines(null))
        assertEquals(emptyList<FolderLocation>(), SearchListsJson.folders(null))
    }

    @Test fun nameRules() {
        assertNotNull((checkEngine("  ", "https://a.com/?q=%s") as EngineCheck.Bad).nameError)
        assertNotNull((checkEngine("x".repeat(31), "https://a.com/?q=%s") as EngineCheck.Bad).nameError)
        assertEquals("x".repeat(30), (checkEngine(" " + "x".repeat(30) + " ", "https://a.com/?q=%s") as EngineCheck.Ok).engine.name)
    }

    @Test fun addressNeedsPercentS() {
        val bad = checkEngine("A", "https://a.com/?q=") as EngineCheck.Bad
        assertEquals("The address needs %s where your search words go", bad.addressError)
        assertNull(bad.nameError)
    }

    @Test fun bareAddressGetsHttps() {
        assertEquals("https://duckduckgo.com/?q=%s", (checkEngine("D", "duckduckgo.com/?q=%s") as EngineCheck.Ok).engine.url)
    }

    @Test fun notAWebAddress() {
        for (a in listOf("ftp://x.com/%s", "hello %s", "https://nohost/%s")) {
            assertEquals(a, "That is not a web address", (checkEngine("A", a) as EngineCheck.Bad).addressError)
        }
    }

    @Test fun engineUrlEncodesSpecials() {
        assertEquals("https://duckduckgo.com/?q=c%2B%2B%20a%26b%2050%25%20%C3%BC", engineUrl(ddg, " c++ a&b 50% ü "))
    }

    @Test fun engineHostForTheDimLine() {
        assertEquals("duckduckgo.com", engineHost(ddg))
    }

    @Test fun jsonRoundTrip() {
        val engines = listOf(ddg, SearchEngine("Wiki", "https://en.wikipedia.org/w/index.php?search=%s"))
        assertEquals(engines, SearchListsJson.engines(SearchListsJson.encodeEngines(engines)))
        val folders = listOf(FolderLocation("content://x/tree/primary%3ADownload", "Download"))
        assertEquals(folders, SearchListsJson.folders(SearchListsJson.encodeFolders(folders)))
    }

    @Test fun badJsonGivesEmpty() {
        assertTrue(SearchListsJson.engines("{").isEmpty())
        assertEquals(listOf(ddg), SearchListsJson.engines("[{\"name\":1},{\"name\":\"DuckDuckGo\",\"url\":\"https://duckduckgo.com/?q=%s\"}]"))
        assertTrue(SearchListsJson.folders("[{\"uri\":\"x\"}]").isEmpty())
    }
}

class EngineOrderTest {
    @Test fun movedUpAndDown() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(1, -1))
        assertEquals(listOf("a", "c", "b"), listOf("a", "b", "c").moved(1, 1))
    }

    @Test fun movedOutOfRangeUnchanged() {
        assertEquals(listOf("a", "b"), listOf("a", "b").moved(0, -1))
        assertEquals(listOf("a", "b"), listOf("a", "b").moved(1, 1))
        assertEquals(listOf("a", "b"), listOf("a", "b").moved(-1, 1))
    }
}

class PickedFolderTest {
    @Test fun withPickedAddsOnceAtTheEnd() {
        val a = FolderLocation("content://t/a", "A"); val b = FolderLocation("content://t/b", "B")
        assertEquals(listOf(a, b), listOf(a).withPicked(b))
        assertEquals(listOf(a, b), listOf(a, b).withPicked(FolderLocation("content://t/a", "A again")))
    }
}
