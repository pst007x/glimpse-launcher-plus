package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetHeightTest {
    private fun sizeAfter(w: Item.Widget, from: Pair<Int, Int>, to: Pair<Int, Int>): Pair<Int, Int> {
        val page = Page(columns = from.first, rows = from.second).withWidget(0, w)!!
        val l = Layout(listOf(page), emptyList(), columns = from.first, rows = from.second).withGrid(to.first, to.second)
        val got = l.pages.flatMap { it.items }.filterIsInstance<Item.Widget>().single()
        return got.cols to got.rows
    }

    @Test fun aFullHeightWidgetStaysFullHeightOnMoreRows() =
        assertEquals(2 to 5, sizeAfter(Item.Widget(1, "w/W", 2, 4), 4 to 4, 4 to 5))

    @Test fun aWidgetGrowsInHeightWithTheRowsRoundedDown() =
        assertEquals(5 to 3, sizeAfter(Item.Widget(1, "w/W", 4, 2), 4 to 4, 5 to 6))

    @Test fun aWidgetShrinksInHeightWithTheRowsRoundedUp() =
        assertEquals(3 to 5, sizeAfter(Item.Widget(1, "w/W", 3, 6), 5 to 6, 5 to 5))

    @Test fun theWholeScreenStaysTheWholeScreen() =
        assertEquals(4 to 4, sizeAfter(Item.Widget(1, "w/W", 5, 6), 5 to 6, 4 to 4))

    @Test fun thereAndBackGivesTheSameSize() {
        val there = sizeAfter(Item.Widget(1, "w/W", 4, 2), 4 to 4, 5 to 6)
        assertEquals(4 to 2, sizeAfter(Item.Widget(1, "w/W", there.first, there.second), 5 to 6, 4 to 4))
    }

    @Test fun twoWidgetsInEachOthersWayBothSurviveWithoutOverlapping() {
        // Side by side on 5 columns; on 2 columns each wants a whole row's width at the same place.
        val page = Page(columns = 5, rows = 6).withWidget(0, Item.Widget(1, "a/A", 3, 3))!!.withWidget(3, Item.Widget(2, "b/B", 2, 3))!!
        val l = Layout(listOf(page), emptyList(), columns = 5, rows = 6).withGrid(2, 4)
        val ws = l.pages.flatMap { p -> p.items.filterIsInstance<Item.Widget>() }
        assertEquals(setOf(1, 2), ws.map { it.id }.toSet())
        for (p in l.pages) {
            val taken = p.items.withIndex().filter { (_, it) -> it is Item.Widget }.sumOf { (_, it) -> (it as Item.Widget).cols * it.rows }
            assertEquals(taken, p.items.count { it is Item.Widget || it == Item.Covered })   // every square has one owner
        }
    }
}
