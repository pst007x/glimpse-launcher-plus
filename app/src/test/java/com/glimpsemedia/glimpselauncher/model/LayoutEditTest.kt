package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutEditTest {
    private fun apps(vararg k: String) = k.map { Item.App(it) }
    private fun layout(vararg pages: List<Item>, dock: List<String> = emptyList()) = Layout(pages.map { Page(it) }, dock)
    private fun keysOf(l: Layout, page: Int) = l.pages[page].items.map { (it as Item.App).key }

    @Test fun addGoesToTheFirstPageWithRoomThenToANewPage() {
        var l = layout(apps("a", "b", "c", "d", "e", "f", "g", "h"), apps("i"))
        l = l.add(Item.App("j"))
        assertEquals(listOf("i", "j"), keysOf(l, 1))
        val full = layout(apps("a", "b", "c", "d", "e", "f", "g", "h")).add(Item.App("z"))
        assertEquals(2, full.pages.size)
        assertEquals(listOf("z"), keysOf(full, 1))
    }

    @Test fun anAppIsPlacedOnlyOnce() {
        val l = layout(apps("a"), dock = listOf("b"))
        assertEquals(l, l.add(Item.App("a")))
        assertEquals(2, l.add(Item.App("b")).screenKeys().size)  // one in the bar may be on a screen too
    }

    @Test fun onlyOneAllAppsIcon() {
        val l = layout(apps("a")).add(Item.AllApps)
        assertTrue(l.hasAllAppsIcon)
        assertEquals(l, l.add(Item.AllApps))
    }

    @Test fun removeKeepsTheOthersInOrder() {
        val l = layout(apps("a", "b", "c")).removeFromPage(0, 1)
        assertEquals(listOf("a", "c"), keysOf(l, 0))
    }

    @Test fun moveWithinAPageShiftsTheOthers() {
        val l = layout(apps("a", "b", "c", "d")).moveItem(0, 3, 0, 1)!!
        assertEquals(listOf("a", "d", "b", "c"), keysOf(l, 0))
        val back = l.moveItem(0, 1, 0, 99)!!
        assertEquals(listOf("a", "b", "c", "d"), keysOf(back, 0))
    }

    @Test fun moveToAnotherPage() {
        val l = layout(apps("a", "b"), apps("c")).moveItem(0, 0, 1, 0)!!
        assertEquals(listOf("b"), keysOf(l, 0))
        assertEquals(listOf("a", "c"), keysOf(l, 1))
    }

    @Test fun moveToAFullPageIsRefused() {
        val l = layout(apps("a"), apps("1", "2", "3", "4", "5", "6", "7", "8"))
        assertNull(l.moveItem(0, 0, 1, 0))
        assertNotNull(l.moveItem(1, 0, 1, 7))
    }

    @Test fun theLastScreenGoesWhenEmptyButOthersStay() {
        val l = layout(apps("a"), emptyList(), apps("b"), emptyList(), emptyList())
        assertEquals(3, l.withoutEmptyExtraPages().pages.size)
        assertEquals(listOf("b"), keysOf(l.withoutEmptyExtraPages(), 2))
        assertEquals(3, layout(apps("a"), emptyList(), apps("b")).withoutEmptyExtraPages().pages.size)
        assertEquals(1, layout(emptyList(), emptyList()).withoutEmptyExtraPages().pages.size)
        assertEquals(l.withoutEmptyExtraPages(), l.withoutEmptyExtraPages().withoutEmptyExtraPages())
    }

    @Test fun onlyEmptyPagesGoAndNeverTheLast() {
        val l = layout(apps("a"), emptyList(), emptyList())
        assertEquals(2, l.removePageIfEmpty(1).pages.size)
        assertEquals(l, l.removePageIfEmpty(0))
        assertEquals(1, layout(emptyList()).removePageIfEmpty(0).pages.size)
    }

    @Test fun bottomBarIsCappedAndRespectsPlacedApps() {
        var l = layout(apps("a"), dock = listOf("1", "2", "3", "4"))
        assertEquals(4, l.addToDock("5", 4).dock.size)
        l = l.removeFromDock("2")
        assertEquals(listOf("1", "3", "4"), l.dock)
        assertEquals(listOf("a", "1", "3", "4"), l.addToDock("a", 4).dock)  // also on a screen is fine; the free place is on the left
        assertEquals(listOf("9", "1", "3", "4"), l.addToDock("9", 4).dock)
    }

    @Test fun jsonRoundTrips() {
        val l = Layout(
            listOf(Page(listOf(Item.App("a/B"), Item.AllApps, Item.Folder("f1", "Games", listOf("c/D")))), Page()),
            listOf("p/Q"),
        )
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
    }

    @Test fun badJsonGivesNull() {
        assertNull(LayoutJson.decode("not json"))
        assertNull(LayoutJson.decode("{\"v\":99,\"pages\":[],\"dock\":[]}"))
    }
}

class FreeLayoutAddTest {
    private fun app(k: String) = Item.App(k)

    @Test fun anAppAddedToASquareLandsThere() {
        val l = Layout(pages = listOf(Page()), dock = emptyList())
        val r = l.addToPageAt(0, 5, app("a"))!!
        assertEquals(app("a"), r.pages[0].items[5])
        assertEquals(null, r.pages[0].items[0])
    }

    @Test fun aTakenSquareFallsBackToTheFirstGap() {
        val l = Layout(pages = listOf(Page(listOf(app("a")))), dock = emptyList())
        val r = l.addToPageAt(0, 0, app("b"))!!
        assertEquals(app("b"), r.pages[0].items[1])
    }

    @Test fun aWidgetAddedToASquareStartsThere() {
        val l = Layout(pages = listOf(Page()), dock = emptyList())
        val w = Item.Widget(1, "p/c", 2, 1)
        val r = l.addWidgetAt(w, 0, 4)
        assertEquals(w, r.pages[0].items[4])
        assertEquals(Item.Covered, r.pages[0].items[5])
    }

    @Test fun aWidgetThatDoesNotFitThereGoesWhereItFits() {
        val l = Layout(pages = listOf(Page()), dock = emptyList())
        val w = Item.Widget(1, "p/c", 2, 1)
        val r = l.addWidgetAt(w, 0, 5)  // right column: 2 wide cannot start here
        assertEquals(w, r.pages[0].items[0])
    }
}

class BarAndScreenTest {
    @Test fun anAppInTheBarCanAlsoGoOnAScreen() {
        val l = Layout(pages = listOf(Page()), dock = listOf("p/Main"))
        val r = l.addToPage(0, Item.App("p/Main"))!!
        assertEquals(Item.App("p/Main"), r.pages[0].items[0])
    }

    @Test fun anAppOnAScreenCanAlsoGoInTheBar() {
        val l = Layout(pages = listOf(Page(listOf(Item.App("a/Main")))), dock = emptyList())
        assertEquals(listOf("a/Main"), l.addToDock("a/Main", 4).dock)
    }
}
