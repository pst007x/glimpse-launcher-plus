package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {
    private fun w(id: Int, c: Int, r: Int) = Item.Widget(id, "pkg/Provider$id", c, r)
    private fun empty() = Layout(listOf(Page()), emptyList())
    private fun apps(vararg k: String) = k.map { Item.App(it) }

    @Test fun sizesSnapToTheGrid() {
        assertEquals(listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 2 to 3, 2 to 4), widgetSizes(2))
        assertEquals(16, widgetSizes(4).size)
        assertEquals(4 to 4, widgetSizes(4).last())
        assertEquals(4 to 1, snapWidgetSize(250, 100, 80, 150, columns = 4))
        assertEquals(1 to 1, snapWidgetSize(40, 40, 170, 150))
        assertEquals(2 to 1, snapWidgetSize(250, 100, 170, 150))
        assertEquals(1 to 2, snapWidgetSize(100, 200, 170, 150))
        assertEquals(2 to 2, snapWidgetSize(250, 250, 170, 150))
        assertEquals(2 to 3, snapWidgetSize(250, 400, 170, 150))
        assertEquals(2 to 4, snapWidgetSize(900, 900, 170, 150))
        // 1 wide and 3 tall is not a size: it becomes 2 by 3
        assertEquals(2 to 3, snapWidgetSize(100, 400, 170, 150))
    }

    @Test fun aWidgetCoversItsSquaresAndLeavesTheRest() {
        val l = empty().addWidgetToPage(0, w(1, 2, 2))!!
        val items = l.pages[0].items
        assertEquals(w(1, 2, 2), items[0])
        assertEquals(listOf(Item.Covered, Item.Covered, Item.Covered), items.subList(1, 4))
        assertEquals(4, l.pages[0].count)
        assertTrue(l.pages[0].hasWidgets)
        // Icons then go in the squares after it
        val withApp = l.add(Item.App("a"))
        assertEquals(Item.App("a"), withApp.pages[0].items[4])
    }

    @Test fun aWidgetGoesWhereItFitsOrOnANewScreen() {
        val full = Layout(listOf(Page(apps("1", "2", "3", "4", "5", "6", "7", "8"))), emptyList())
        val l = full.addWidget(w(1, 2, 1))
        assertEquals(2, l.pages.size)
        assertEquals(w(1, 2, 1), l.pages[1].items[0])
        assertNull(full.addWidgetToPage(0, w(2, 1, 1)))
        // A tall one skips the squares that are taken
        val partly = Layout(listOf(Page(apps("a", "b"))), emptyList())
        assertEquals(w(3, 2, 2), partly.addWidgetToPage(0, w(3, 2, 2))!!.pages[0].items[2])
    }

    @Test fun aWidgetOnTheEmptyScreenAfterTheLastStartsANewScreen() {
        val l = Layout(listOf(Page(apps("a"))), emptyList()).addWidget(w(5, 2, 2), preferred = 1)
        assertEquals(2, l.pages.size)
        assertEquals(w(5, 2, 2), l.pages[1].items[0])
    }

    @Test fun aWidgetMovesToWhereAllItsSquaresAreFree() {
        val l = empty().addWidgetToPage(0, w(1, 2, 1))!!.add(Item.App("a"))   // widget on row 0, app in square 2
        // Down to rows 2-3? Square 4 is free for a 2 x 1
        val moved = l.moveWidget(0, 0, 0, 4)!!
        assertEquals(w(1, 2, 1), moved.pages[0].items[4])
        assertEquals(Item.Covered, moved.pages[0].items[5])
        assertNull(moved.pages[0].items.getOrNull(0))
        assertEquals(Item.App("a"), moved.pages[0].items[2])
        // Onto the app's square: no room
        assertNull(l.moveWidget(0, 0, 0, 2))
        // A 2-wide widget cannot start in the right-hand column
        assertNull(l.moveWidget(0, 0, 0, 5))
        // To another screen
        val two = Layout(listOf(Page(listOf(w(1, 1, 2), Item.Covered.let { null }.let { null })), Page()), emptyList())
        assertNotNull(two.moveWidget(0, 0, 1, 2))
    }

    @Test fun aMovedWidgetFitsWhatIsThere() {
        val tall = w(1, 2, 3)
        val l = Layout(listOf(Page(), Page().withWidget(0, tall)), emptyList())
        // Dropped low down on an empty screen: kept on the grid, same size
        val a = l.moveWidgetFitting(1, 0, 0, 6)!!
        assertEquals(tall, a.pages[0].items[2])  // row 1, the lowest a 3-tall one can start
        // Dropped where an icon is: moves to the nearest free place of the same size
        val b = Layout(listOf(Page(listOf(null, Item.App("x"))), Page().withWidget(0, w(1, 2, 2))), emptyList())
            .moveWidgetFitting(1, 0, 0, 0)!!
        assertEquals(w(1, 2, 2), b.pages[0].items[2])
        // Not enough room for its size: it shrinks
        val crowded = Layout(listOf(Page(listOf(Item.App("1"), null, Item.App("3"), null, Item.App("5"), null, Item.App("7"), null)),
            Page().withWidget(0, w(1, 2, 2))), emptyList())
        val c = crowded.moveWidgetFitting(1, 0, 0, 0)!!
        assertEquals(w(1, 1, 2), c.pages[0].items[1])
        // No room at all
        val full = Layout(listOf(Page(apps("1", "2", "3", "4", "5", "6", "7")), Page().withWidget(0, w(1, 2, 1))), emptyList())
        assertEquals(w(1, 1, 1), full.moveWidgetFitting(1, 0, 0, 0)!!.pages[0].items[7])
        val none = Layout(listOf(Page(apps("1", "2", "3", "4", "5", "6", "7", "8")), Page().withWidget(0, w(1, 1, 1))), emptyList())
        assertNull(none.moveWidgetFitting(1, 0, 0, 0))
    }

    @Test fun removingAWidgetFreesAllItsSquares() {
        val l = empty().addWidgetToPage(0, w(1, 2, 3))!!.removeWidget(0, 0)
        assertTrue(l.pages[0].isEmpty)
    }

    @Test fun resizingKeepsThePlaceIfItFitsElseMoves() {
        val l = empty().addWidgetToPage(0, w(1, 1, 1))!!
        val bigger = l.resizeWidget(0, 0, 2, 2)!!
        assertEquals(w(1, 2, 2), bigger.pages[0].items[0])
        assertEquals(4, bigger.pages[0].count)
        // A page whose room is taken refuses
        val crowded = Layout(listOf(Page(listOf(w(1, 1, 1)) + apps("b", "c", "d", "e", "f", "g", "h"))), emptyList())
        assertNull(crowded.resizeWidget(0, 0, 2, 2))
    }

    @Test fun pagesWithWidgetsKeepTheirShapeWhenIconsAreRemovedOrCompacted() {
        var l = empty().addWidgetToPage(0, w(1, 2, 1))!!.add(Item.App("a")).add(Item.App("b"))
        assertEquals(Item.App("a"), l.pages[0].items[2])
        l = l.removeFromPage(0, 2)
        assertEquals(w(1, 2, 1), l.pages[0].items[0])
        assertEquals(Item.Covered, l.pages[0].items[1])
        assertEquals(Item.App("b"), l.pages[0].items[3])
        assertEquals(l.pages[0].items, l.compacted().pages[0].items)
    }

    @Test fun widgetsAreNotDraggedAndIconsDoNotLandOnThem() {
        val l = empty().addWidgetToPage(0, w(1, 2, 1))!!.add(Item.App("a"))
        assertNull(l.placeItem(0, 0, 0, 5))
        assertNull(l.placeItem(0, 2, 0, 0))
        assertNull(l.placeItem(0, 2, 0, 1))
        assertNull(l.moveItem(0, 0, 0, 3))
        assertNull(l.dropOnto(0, 2, 0, 0, "f"))
        assertNotNull(l.placeItem(0, 2, 0, 5))
    }

    @Test fun savedAndReadBackWithWidgets() {
        val l = empty().addWidgetToPage(0, w(7, 2, 2))!!.add(Item.App("a"))
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
        assertEquals(listOf(w(7, 2, 2)), l.widgets())
    }
}
