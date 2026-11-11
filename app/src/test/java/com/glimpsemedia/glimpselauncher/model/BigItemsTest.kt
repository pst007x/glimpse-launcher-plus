package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Backup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Big (2 × 2) items, on a 4 × 4 grid unless said: squares 0-3 are the first row, 4-7 the second, and so on. */
class BigItemsTest {
    private val C = Item.Covered
    private fun a(k: String) = Item.App(k)
    private fun w(id: Int) = Item.Widget(id, "p/W$id", 1, 1)
    private fun page(vararg at: Pair<Int, Item>, cols: Int = 4, rows: Int = 4): Page {
        val list = MutableList<Item?>(cols * rows) { null }
        for ((i, it) in at) list[i] = it
        return Page(list, cols, rows).packedEnd()
    }
    private fun layout(vararg pages: Page, big: Set<String> = emptySet()) =
        Layout(pages.toList(), emptyList(), columns = pages[0].columns, rows = pages[0].rows, bigItems = big)
    private fun Layout.at(p: Int, i: Int) = pages[p].items.getOrNull(i)
    private fun done(r: BigResult) = (r as BigResult.Done).layout

    // ---- growing ----

    @Test fun growsRightAndDown() {
        val l = done(layout(page(5 to a("a"))).makeBig(0, 5))
        assertEquals(a("a"), l.at(0, 5))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 6), l.at(0, 9), l.at(0, 10)))
        assertEquals(setOf("a"), l.bigItems)
    }

    @Test fun atTheRightEdgeItGrowsLeft() {
        val l = done(layout(page(7 to a("a"))).makeBig(0, 7))
        assertEquals(a("a"), l.at(0, 6))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 7), l.at(0, 10), l.at(0, 11)))
    }

    @Test fun onTheBottomRowItGrowsUp() {
        val l = done(layout(page(13 to a("a"))).makeBig(0, 13))
        assertEquals(a("a"), l.at(0, 9))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 10), l.at(0, 13), l.at(0, 14)))
    }

    @Test fun inTheBottomRightCornerItGrowsLeftAndUp() {
        assertEquals(a("a"), done(layout(page(15 to a("a"))).makeBig(0, 15)).at(0, 10))
    }

    @Test fun besideAWidgetItTakesTheNextBlock() {
        val l = done(layout(page(5 to a("a"), 6 to w(1))).makeBig(0, 5))
        assertEquals(a("a"), l.at(0, 4))
        assertEquals(w(1), l.at(0, 6))
    }

    @Test fun boxedInByWidgetsThereIsNoRoom() {
        assertEquals(BigResult.NoRoom, layout(page(5 to a("a"), 4 to w(1), 6 to w(2))).makeBig(0, 5))
    }

    @Test fun widgetsAndCoveredSquaresCannotBeMadeBig() {
        assertEquals(BigResult.NotAllowed, layout(page(0 to w(1))).makeBig(0, 0))
        assertEquals(BigResult.NotAllowed, layout(page(0 to a("a"))).makeBig(0, 3))
    }

    @Test fun builtInTilesAndFoldersCanBeBig() {
        assertEquals(setOf("emergency"), done(layout(page(0 to Item.Emergency)).makeBig(0, 0)).bigItems)
        assertEquals(setOf("f:x"), done(layout(page(0 to Item.Folder("x", "F", listOf("a", "b")))).makeBig(0, 0)).bigItems)
    }

    // ---- items in the way ----

    @Test fun itemsInTheWayMoveToTheFirstFreeSquares() {
        val l = done(layout(page(0 to a("a"), 1 to a("b"), 2 to a("e"), 4 to a("c"), 5 to a("d"))).makeBig(0, 0))
        assertEquals(a("a"), l.at(0, 0))
        assertEquals(a("e"), l.at(0, 2))
        assertEquals(listOf(a("b"), a("c"), a("d")), listOf(l.at(0, 3), l.at(0, 6), l.at(0, 7)))
    }

    @Test fun onAFullScreenTheyGoToTheNextScreen() {
        val full = page(*(0 until 16).map { it to a("p$it") }.toTypedArray())
        val l = done(layout(full, page(0 to a("z"))).makeBig(0, 0))
        assertEquals(listOf(a("z"), a("p1"), a("p4"), a("p5")), (0..3).map { l.at(1, it) })
    }

    @Test fun aNewScreenIsMadeWhenThereIsNone() {
        val full = page(*(0 until 16).map { it to a("p$it") }.toTypedArray())
        val l = done(layout(full).makeBig(0, 0))
        assertEquals(2, l.pages.size)
        assertEquals(listOf(a("p1"), a("p4"), a("p5")), (0..2).map { l.at(1, it) })
    }

    @Test fun aBigItemInTheWayBecomesNormalWhenNoBlockIsFree() {
        val others = listOf(0, 4, 5, 8, 9, 10, 11, 12, 13, 14, 15).map { it to a("o$it") }
        val start = layout(page(1 to a("a"), 2 to a("x"), 3 to C, 6 to C, 7 to C, *others.toTypedArray()), big = setOf("x"))
        val l = done(start.makeBig(0, 1))
        assertEquals(a("a"), l.at(0, 1))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 2), l.at(0, 5), l.at(0, 6)))
        assertEquals(a("x"), l.at(0, 3))
        assertEquals(a("o5"), l.at(0, 7))
        assertEquals(setOf("a"), l.bigItems)
    }

    @Test fun aBigItemInTheWayStaysBigWhenABlockIsFree() {
        val l = done(layout(page(0 to a("a"), 1 to a("x"), 2 to C, 5 to C, 6 to C), big = setOf("x")).makeBig(0, 0))
        assertEquals(a("a"), l.at(0, 0))
        assertEquals(setOf("a", "x"), l.bigItems)
        assertEquals(a("x"), l.at(0, 2))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 3), l.at(0, 6), l.at(0, 7)))
    }

    // ---- Make it normal ----

    @Test fun makeItNormalEmptiesTheThreeSquaresAndMovesNothing() {
        val big = done(layout(page(5 to a("a"), 0 to a("b"))).makeBig(0, 5))
        val l = big.makeNormal(0, 5)
        assertEquals(a("a"), l.at(0, 5))
        assertEquals(a("b"), l.at(0, 0))
        assertEquals(listOf(null, null, null), listOf(l.at(0, 6), l.at(0, 9), l.at(0, 10)))
        assertEquals(emptySet<String>(), l.bigItems)
    }

    // ---- dragging ----

    @Test fun aDraggedBigItemStaysOnTheGrid() {
        val big = done(layout(page(0 to a("a"))).makeBig(0, 0))
        val l = big.moveBig(0, 0, 0, 15)!!
        assertEquals(a("a"), l.at(0, 10))
        assertEquals(listOf(C, C, C), listOf(l.at(0, 11), l.at(0, 14), l.at(0, 15)))
        assertNull(l.at(0, 0))
        assertNull(l.at(0, 1))
    }

    @Test fun draggingMovesItemsInTheWay() {
        val big = done(layout(page(0 to a("a"), 10 to a("b"))).makeBig(0, 0))
        val l = big.moveBig(0, 0, 0, 10)!!
        assertEquals(a("a"), l.at(0, 10))
        assertEquals(a("b"), l.at(0, 0))
    }

    @Test fun draggingOntoAWidgetIsRefused() {
        val big = done(layout(page(0 to a("a"), 10 to w(1))).makeBig(0, 0))
        assertNull(big.moveBig(0, 0, 0, 10))
    }

    @Test fun draggingToAnotherScreenKeepsItBig() {
        val big = done(layout(page(0 to a("a")), page(0 to a("b"))).makeBig(0, 0))
        val l = big.moveBig(0, 0, 1, 0)!!
        assertEquals(a("a"), l.at(1, 0))
        assertEquals(a("b"), l.at(1, 2))
        assertTrue(l.pages[0].isEmpty)
    }

    @Test fun aCoveredSquareBelongsToTheBigItem() {
        val big = done(layout(page(5 to Item.Folder("x", "F", listOf("a")))).makeBig(0, 5))
        assertEquals(5, big.pages[0].ownerOf(10))
        assertEquals(5, big.pages[0].ownerOf(5))
        assertNull(big.pages[0].ownerOf(0))
    }

    @Test fun anAppDroppedOnABigFolderJoinsIt() {
        val big = done(layout(page(5 to Item.Folder("x", "F", listOf("a")), 0 to a("b"))).makeBig(0, 5))
        val l = big.dropOnto(0, 0, 0, big.pages[0].ownerOf(10)!!, "new", keepGaps = true)!!.withBigRepaired()
        assertEquals(Item.Folder("x", "F", listOf("a", "b")), l.at(0, 5))
        assertEquals(setOf("f:x"), l.bigItems)
    }

    @Test fun freeLayoutSwapsLeaveBigItemsAlone() {
        val big = done(layout(page(5 to a("a"), 0 to a("b"))).makeBig(0, 5))
        assertNull(big.placeItem(0, 0, 0, 5))
        assertNull(big.placeItem(0, 5, 0, 0))
    }

    @Test fun moveToTheNextScreenKeepsItBig() {
        val big = done(layout(page(0 to a("a")), page(0 to a("b"))).makeBig(0, 0))
        val l = big.moveBigToPage(0, 0, 1)!!
        assertEquals(setOf("a"), l.bigItems)
        assertEquals(a("a"), l.at(1, 1))   // the first free block
        assertEquals(a("b"), l.at(1, 0))
    }

    // ---- repair ----

    @Test fun aKeyWithNoItemIsDropped() {
        assertEquals(emptySet<String>(), layout(page(0 to a("a")), big = setOf("gone")).withBigRepaired().bigItems)
    }

    @Test fun aBigItemWithoutRoomBecomesNormal() {
        val l = layout(page(0 to a("a"), 1 to a("b"), 4 to C, 5 to C), big = setOf("a")).withBigRepaired()
        assertEquals(emptySet<String>(), l.bigItems)
        assertNull(l.at(0, 4))
        assertNull(l.at(0, 5))
        assertEquals(a("b"), l.at(0, 1))
    }

    @Test fun aStrayCoveredSquareIsEmptied() {
        val l = layout(page(0 to a("a"), 6 to C)).withBigRepaired()
        assertNull(l.at(0, 6))
    }

    @Test fun aBigItemOnTheEdgeBecomesNormal() {
        assertEquals(emptySet<String>(), layout(page(3 to a("a")), big = setOf("a")).withBigRepaired().bigItems)
    }

    @Test fun aRepairedLayoutIsLeftAlone() {
        val big = done(layout(page(5 to a("a"), 0 to a("b"))).makeBig(0, 5))
        assertEquals(big, big.withBigRepaired())
    }

    @Test fun removingABigItemDropsItsKeyAndSquares() {
        val big = done(layout(page(5 to a("a"), 0 to a("b"))).makeBig(0, 5))
        val l = big.removeFromPage(0, 5).withBigRepaired()
        assertEquals(emptySet<String>(), l.bigItems)
        assertTrue((5..15).all { l.at(0, it) == null })
        assertEquals(a("b"), l.at(0, 0))
    }

    @Test fun aBigItemsPageIsNeverPacked() {
        val big = done(layout(page(5 to a("a"))).makeBig(0, 5))
        assertTrue(big.pages[0].hasWidgets)
        assertEquals(big, big.compacted())
    }

    // ---- changing the grid ----

    @Test fun onANewGridABigItemStaysBigWhereItFits() {
        val big = done(layout(page(0 to a("a"), 2 to a("b"), 3 to a("c"))).makeBig(0, 0))
        val l = big.withGrid(2, 4).withBigRepaired()
        assertEquals(setOf("a"), l.bigItems)
        assertEquals(a("a"), l.at(0, 0))
        assertEquals(listOf(C, C, C), (1..3).map { l.at(0, it) })
        assertEquals(listOf(a("b"), a("c")), listOf(l.at(0, 4), l.at(0, 5)))
    }

    @Test fun onANewGridABigItemWithoutRoomBecomesNormal() {
        val firsts = (0 until 6).map { it to a("p$it") }
        val big = layout(page(*firsts.toTypedArray(), 6 to a("a"), 7 to C, 10 to C, 11 to C), big = setOf("a"))
        val l = big.withGrid(2, 4).withBigRepaired()
        assertEquals(emptySet<String>(), l.bigItems)
        assertEquals(a("a"), l.at(0, 6))
    }

    // ---- stored ----

    @Test fun jsonKeepsTheBigItems() {
        val big = done(layout(page(5 to a("a"), 0 to Item.Emergency)).makeBig(0, 5))
        val back = LayoutJson.decode(LayoutJson.encode(big))!!
        assertEquals(big.bigItems, back.bigItems)
        assertEquals(big.pages, back.pages)
    }

    @Test fun aLayoutWithoutBigReadsWithNothingBig() {
        val old = org.json.JSONObject(LayoutJson.encode(layout(page(0 to a("a"))))).apply { remove("big") }.toString()
        assertEquals(emptySet<String>(), LayoutJson.decode(old)!!.bigItems)
    }

    @Test fun aDamagedBigListIsRepairedOnLoad() {
        val text = LayoutJson.encode(layout(page(3 to a("a")))).replace("\"big\":[]", "\"big\":[\"a\",\"ghost\"]")
        assertEquals(emptySet<String>(), LayoutJson.decode(text)!!.bigItems)
    }

    @Test fun backupsWithABigItemAreFormatThree() {
        val big = done(layout(page(5 to a("a"))).makeBig(0, 5))
        assertEquals(3, Backup.formatFor(big))
        assertEquals(Backup.FORMAT_USUAL, Backup.formatFor(layout(page(0 to a("a")))))
        assertEquals(2, Backup.formatFor(layout(page(0 to a("a"), cols = 5, rows = 6))))
        assertEquals(Backup.FORMAT_USUAL, Backup.formatFor(null))
        assertEquals(3, Backup.FORMAT)
    }

    // ---- the big folder ----

    private val roomy = 240f   // a panel with room for 3 × 3 cells of 48 dp (the 2 × 4 grid)
    private val small = 150f   // too small for that (4 × 4, 5 × 6)

    @Test fun aBigFolderShowsUpToFourInTwoByTwo() {
        for (n in 0..4) assertEquals(BigFolderCells(2, n, 0), bigFolderCells(n, roomy))
    }

    @Test fun withRoomItShowsUpToNineInThreeByThree() {
        for (n in 5..9) assertEquals(BigFolderCells(3, n, 0), bigFolderCells(n, roomy))
    }

    @Test fun pastNineTheLastCellSaysHowManyMore() {
        assertEquals(BigFolderCells(3, 8, 2), bigFolderCells(10, roomy))
        assertEquals(BigFolderCells(3, 8, 4), bigFolderCells(12, roomy))
        assertFalse(bigFolderCells(9, roomy).more > 0)
    }

    @Test fun aSmallPanelKeepsTwoByTwoSoEveryIconIsEasyToTouch() {
        assertEquals(BigFolderCells(2, 4, 0), bigFolderCells(4, small))
        assertEquals(BigFolderCells(2, 3, 2), bigFolderCells(5, small))
        assertEquals(BigFolderCells(2, 3, 5), bigFolderCells(8, small))
    }

    @Test fun threeByThreeNeedsCellsOf48dp() {
        // 8 dp padding each side and two 6 dp gaps: 3 × 48 + 28 = 172.
        assertEquals(3, bigFolderCells(6, 172f).grid)
        assertEquals(2, bigFolderCells(6, 171f).grid)
    }
}
