package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTileTest {
    private val empty = Layout(listOf(Page()), emptyList())

    @Test fun searchTileRoundTripAndOldReaders() {
        val layout = empty.add(Item.App("a/b")).add(Item.Search)
        val back = LayoutJson.decode(LayoutJson.encode(layout))!!
        assertEquals(listOf(Item.App("a/b"), Item.Search), back.pages[0].items)
        // A kind this version does not know (from a newer one) is dropped; the rest still loads.
        val future = LayoutJson.encode(layout).replace("\"t\":\"search\"", "\"t\":\"future\"")
        assertEquals(listOf<Item?>(Item.App("a/b")), LayoutJson.decode(future)!!.pages[0].items)
    }

    @Test fun atMostOneSearchTile() {
        assertFalse(empty.hasSearchIcon)
        val once = empty.add(Item.Search)
        assertTrue(once.hasSearchIcon)
        assertEquals(once, once.add(Item.Search))
        assertEquals(once, once.addToPage(0, Item.Search))
        assertEquals(once, once.addToPageAt(0, 3, Item.Search))
    }

    @Test fun searchTileKeys() {
        assertEquals("search", Item.Search.colorKey)
        assertEquals("search", Item.Search.holdKey)
    }
}
