package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeLayoutTest {
    private fun app(k: String) = Item.App(k)
    private fun layout(vararg pages: List<Item?>) = Layout(pages.map { Page(it) }, emptyList())

    @Test fun anIconCanSitInAnySlotWithGapsBefore() {
        val l = layout(listOf(app("a"), app("b"))).placeItem(0, 0, 0, 7)!!
        assertEquals(listOf(null, app("b"), null, null, null, null, null, app("a")), l.pages[0].items)
    }

    @Test fun anOccupiedSlotSwaps() {
        val l = layout(listOf(app("a"), app("b"), app("c"))).placeItem(0, 0, 0, 2)!!
        assertEquals(listOf(app("c"), app("b"), app("a")), l.pages[0].items)
    }

    @Test fun placingOnAnotherPageLeavesAGapBehind() {
        val l = layout(listOf(app("a"), app("b")), listOf(app("c"))).placeItem(0, 0, 1, 5)!!
        assertEquals(listOf(null, app("b")), l.pages[0].items)
        assertEquals(listOf(app("c"), null, null, null, null, app("a")), l.pages[1].items)
    }

    @Test fun slotsOutsideThePageAreRefused() {
        assertNull(layout(listOf(app("a"))).placeItem(0, 0, 0, 8))
        assertNull(layout(listOf(app("a"))).placeItem(0, 0, 1, 0))
        assertNull(layout(listOf(app("a"))).placeItem(0, 3, 0, 0))
    }

    @Test fun removingCanKeepOrCloseTheGap() {
        val l = layout(listOf(app("a"), app("b"), app("c")))
        assertEquals(listOf(app("a"), null, app("c")), l.removeFromPage(0, 1, keepGaps = true).pages[0].items)
        assertEquals(listOf(app("a"), app("c")), l.removeFromPage(0, 1).pages[0].items)
    }

    @Test fun newIconsFillTheFirstGap() {
        val l = layout(listOf(app("a"), null, app("c"))).add(app("z"))
        assertEquals(listOf(app("a"), app("z"), app("c")), l.pages[0].items)
    }

    @Test fun aPageWithGapsIsNotFullUntilEightIcons() {
        val sparse = Page(listOf(null, null, null, null, null, null, null, app("x")))
        assertTrue(!sparse.isFull)
        assertEquals(1, sparse.count)
        assertTrue(Page((1..8).map { app("k$it") }).isFull)
    }

    @Test fun packingClosesAllGaps() {
        val l = layout(listOf(null, app("a"), null, app("b")), listOf(null)).compacted()
        assertEquals(listOf(app("a"), app("b")), l.pages[0].items)
        assertEquals(emptyList<Item?>(), l.pages[1].items)
    }

    @Test fun aGapSurvivesSavingAndLoading() {
        val l = layout(listOf(null, app("a"), null, Item.AllApps), listOf(app("z")))
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
    }

    @Test fun aMissingAppLeavesAGapForTheFreeLayout() {
        val l = layout(listOf(app("a"), app("b"), app("c"))).withoutMissing(setOf("a", "c"))
        assertEquals(listOf(app("a"), null, app("c")), l.pages[0].items)
    }

    @Test fun droppingOntoAnIconKeepsTheGapItLeft() {
        val l = layout(listOf(app("a"), null, app("c"))).dropOnto(0, 0, 0, 2, "f1", keepGaps = true)!!
        assertEquals(listOf(null, null, Item.Folder("f1", "Folder", listOf("c", "a"))), l.pages[0].items)
    }

    @Test fun anEmptyingFolderLeavesAGapInTheFreeLayout() {
        val l = layout(listOf(app("x"), Item.Folder("f", "F", listOf("a", "b")), app("y")))
        val out = l.takeOutOfFolder(0, 1, "a", keepGaps = true)
        assertEquals(listOf(app("x"), app("b"), app("y"), app("a")), out.pages[0].items)
    }
}
