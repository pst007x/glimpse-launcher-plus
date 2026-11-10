package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTest {
    private fun keys(n: Int, from: Int = 1) = (from until from + n).map { "app$it/Main" }
    private fun apps(keys: List<String>) = keys.map { Item.App(it) }
    private fun twoCol(vararg pages: List<Item?>) = Layout(pages.map { Page(it) }, emptyList())
    private fun appKeys(l: Layout) = l.pages.flatMap { p -> p.items.filterIsInstance<Item.App>().map { it.key } }
    private fun widgets(l: Layout) = l.pages.flatMap { it.items.filterIsInstance<Item.Widget>() }

    @Test fun twentyAppsOnFourColumnsFillSixteenThenFour() {
        val l = DefaultLayout.buildFilled(keys(20), emptyList(), columns = 4)
        assertEquals(4, l.columns)
        assertEquals(listOf(16, 4), l.pages.map { it.count })
    }

    @Test fun switchingToFourColumnsKeepsOrderAndMergesPages() {
        val l = twoCol(apps(keys(8)), apps(keys(8, 9)), apps(keys(8, 17)))
        val four = l.withColumns(4)
        assertEquals(4, four.columns)
        assertEquals(16, four.slotsPerPage)
        assertEquals(listOf(16, 8), four.pages.map { it.count })
        assertEquals(keys(24), appKeys(four))
    }

    @Test fun switchingToTwoColumnsAddsPages() {
        val four = Layout(listOf(Page(apps(keys(16)), columns = 4)), emptyList(), columns = 4)
        val two = four.withColumns(2)
        assertEquals(2, two.columns)
        assertEquals(listOf(8, 8), two.pages.map { it.count })
        assertEquals(keys(16), appKeys(two))
    }

    @Test fun freeLayoutGapsCloseWhenSwitching() {
        val l = twoCol(listOf(null, Item.App("a/M"), null, null, Item.App("b/M")))
        val four = l.withColumns(4)
        assertFalse(four.pages.any { p -> p.items.any { it == null } })
        assertEquals(listOf("a/M", "b/M"), appKeys(four))
    }

    @Test fun widgetWidthDoublesAndHalves() {
        val narrow = Item.Widget(1, "p/W1", 1, 2)
        val wide = Item.Widget(2, "p/W2", 2, 1)
        val l = Layout(listOf(Page().withWidget(0, narrow), Page().withWidget(0, wide)), emptyList())
        val four = l.withColumns(4)
        assertEquals(setOf(2 to 2, 4 to 1), widgets(four).map { it.cols to it.rows }.toSet())
        val back = four.withColumns(2)
        assertEquals(setOf(1 to 2, 2 to 1), widgets(back).map { it.cols to it.rows }.toSet())
    }

    @Test fun roundTripKeepsEverything() {
        val folder = Item.Folder("f1", "Games", listOf("g1/M", "g2/M"))
        val l = Layout(
            listOf(
                Page(apps(keys(6)) + listOf(folder, Item.AllApps)),
                Page(listOf(Item.Emergency)).let { it.withWidget(2, Item.Widget(7, "p/W7", 1, 2)) },
                Page().withWidget(0, Item.Widget(8, "p/W8", 2, 2)),
            ),
            emptyList(),
        )
        val back = l.withColumns(4).withColumns(2)
        fun things(x: Layout) = x.pages.flatMap { p ->
            p.items.filterNotNull().filter { it != Item.Covered }.map { if (it is Item.Widget) "w${it.id}:${it.cols}x${it.rows}" else it.toString() }
        }.sorted()
        assertEquals(things(l), things(back))
        // nothing twice
        assertEquals(things(back).size, things(back).toSet().size)
    }

    @Test fun folderMovesAsOneItem() {
        val folder = Item.Folder("f1", "Games", listOf("g1/M", "g2/M", "g3/M"))
        val four = twoCol(apps(keys(3)) + listOf(folder)).withColumns(4)
        assertEquals(1, four.pages.flatMap { it.items }.count { it is Item.Folder })
        assertEquals(folder, four.pages.flatMap { it.items }.first { it is Item.Folder })
    }

    @Test fun oldSaveReadsAsTwoColumns() {
        val old = """{"v":1,"pages":[[{"t":"app","k":"a/M"}]],"dock":[]}"""
        assertEquals(2, LayoutJson.decode(old)!!.columns)
        val four = Layout(listOf(Page(apps(keys(10)), columns = 4)), emptyList(), columns = 4)
        val read = LayoutJson.decode(LayoutJson.encode(four))!!
        assertEquals(4, read.columns)
        assertEquals(four, read)
    }

    @Test fun fourColumnPageHoldsSixteen() {
        var page: Page? = Page(columns = 4)
        repeat(16) { i -> page = page!!.withItem(Item.App("k$i/M")) }
        assertNotNull(page)
        assertTrue(page!!.isFull)
        assertEquals(null, page!!.withItem(Item.App("extra/M")))
    }

    @Test fun addingToAFourColumnLayoutMakesFourColumnPages() {
        val l = Layout(listOf(Page(apps(keys(16)), columns = 4)), emptyList(), columns = 4).add(Item.App("new/M"))
        assertEquals(listOf(4, 4), l.pages.map { it.columns })
        assertEquals(listOf(16, 1), l.pages.map { it.count })
    }

    @Test fun fourColumnDenseWidgetsSurviveSwitchingToTwo() {
        val wide = Item.Widget(11, "p/Cal", 3, 4)
        val thin = Item.Widget(12, "p/Thin", 1, 4)
        val page = Page(columns = 4).withWidget(0, wide).withWidget(3, thin)
        val four = Layout(listOf(page), emptyList(), columns = 4)
        val two = four.withColumns(2)
        assertEquals(setOf(11, 12), widgets(two).map { it.id }.toSet())
        assertEquals(setOf(11, 12), widgets(two.withColumns(4).withColumns(2)).map { it.id }.toSet())
    }

    // ---- the tighter grids (4×5, 5×5, 5×6) -------------------------------------------------------------------

    private fun flat(l: Layout) = l.pages.flatMap { p -> p.items.filterNotNull().filter { it !is Item.Widget && it != Item.Covered } }

    @Test fun gridsAreTheFive() {
        assertEquals(listOf(2 to 4, 4 to 4, 4 to 5, 5 to 5, 5 to 6), GRIDS)
        assertEquals(listOf(4 to 5, 5 to 5, 5 to 6), TIGHT_GRIDS)
        var refused = false
        try { Page(columns = 5, rows = 4) } catch (_: IllegalArgumentException) { refused = true }
        assertTrue(refused)
    }

    @Test fun pageHasColumnsTimesRowsSlots() {
        assertEquals(30, Page(columns = 5, rows = 6).slots)
    }

    @Test fun orderSurvivesEveryGrid() {
        val folders = listOf(Item.Folder("f1", "One", listOf("x/A", "y/B")), Item.Folder("f2", "Two", listOf("z/C")))
        val items = apps(keys(20)) + folders[0] + apps(keys(10, 21)) + folders[1] + apps(keys(10, 31))
        val start = Layout(items.chunked(8).map { Page(it) }, emptyList())
        val before = flat(start)
        for ((c, r) in listOf(5 to 6, 4 to 4, 2 to 4)) {
            val l = start.withGrid(5, 6).let { if ((c to r) == (5 to 6)) it else it.withGrid(4, 4) }.let { if ((c to r) == (2 to 4)) it.withGrid(2, 4) else it }
            assertEquals(before, flat(l))
            assertTrue(l.pages.all { it.columns == c && it.rows == r })
            assertEquals(c to r, l.columns to l.rows)
        }
    }

    @Test fun widgetSurvivesTightGridAndBack() {
        val page = Page(columns = 4).withWidget(0, Item.Widget(7, "w/W", 4, 2)).let { p ->
            var q = p; for (a in apps(keys(3))) q = q.withItem(a)!!; q
        }
        val l = Layout(listOf(page), emptyList(), columns = 4).withGrid(5, 6).withGrid(4, 4)
        val ws = widgets(l)
        assertEquals(1, ws.size)
        assertEquals(7, ws[0].id)
        assertTrue(ws[0].cols <= 4 && ws[0].rows <= 4)
        assertEquals(keys(3), appKeys(l))
    }

    @Test fun freeLayoutKeepsEveryItem() {
        val p = Page(columns = 4).set(0, Item.App("a/M")).set(5, Item.App("b/M")).set(15, Item.App("c/M"))
        val l = Layout(listOf(p), emptyList(), columns = 4).withGrid(5, 5)
        assertEquals(listOf("a/M", "b/M", "c/M"), appKeys(l))
    }

    @Test fun widgetSizesForTightGrids() {
        val s = widgetSizes(5, 6)
        assertEquals(1 to 1, s.first())
        assertEquals(5 to 6, s.last())
        assertEquals(30, s.size)
        assertEquals(listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 2 to 3, 2 to 4), widgetSizes(2))
    }

    @Test fun tilesOnMovesTightGridsTo4x4() {
        for (g in TIGHT_GRIDS) assertEquals(4 to 4, gridAfterTilesOn(g.first, g.second))
        assertEquals(2 to 4, gridAfterTilesOn(2, 4))
        assertEquals(4 to 4, gridAfterTilesOn(4, 4))
    }

    @Test fun jsonRows() {
        val old = LayoutJson.decode(LayoutJson.encode(Layout(listOf(Page(apps(keys(3)), columns = 4)), emptyList(), columns = 4)).replace(",\"rows\":4", ""))!!
        assertEquals(4, old.rows)
        val tight = Layout(listOf(Page(apps(keys(30)), columns = 5, rows = 6)), emptyList(), columns = 5, rows = 6)
        assertEquals(tight, LayoutJson.decode(LayoutJson.encode(tight)))
        val bad = LayoutJson.encode(Layout(listOf(Page(apps(keys(3)), columns = 4)), emptyList(), columns = 4)).replace("\"cols\":4", "\"cols\":5")
        val read = LayoutJson.decode(bad)!!
        assertEquals(2 to 4, read.columns to read.rows)
    }

    @Test fun tightLayoutReadsBackWhateverTheSettings() {
        val tight = Layout(listOf(Page(apps(keys(4)), columns = 5, rows = 6)), emptyList(), columns = 5, rows = 6)
        val back = LayoutJson.decode(LayoutJson.encode(tight))!!
        assertEquals(5 to 6, back.columns to back.rows)
    }

    @Test fun droppingUninstalledAppsKeepsTheTightGrid() {
        // On every start, apps no longer on the phone are dropped: a 5×6 screen must stay 5×6.
        val tight = Layout(listOf(Page(apps(keys(5)), columns = 5, rows = 6)), emptyList(), columns = 5, rows = 6)
        val after = tight.withoutMissing(keys(3).toSet())
        assertEquals(5 to 6, after.pages[0].columns to after.pages[0].rows)
        assertEquals(keys(3), appKeys(after))
    }

    @Test fun everyGridFitsTheMostSquaresAScreenCanHave() {
        // The home screen keeps each screen's squares MAX_SLOTS apart: no grid may have more, or screens overlap.
        assertEquals(30, MAX_SLOTS)
        assertTrue(GRIDS.all { (c, r) -> c * r <= MAX_SLOTS })
    }
}
