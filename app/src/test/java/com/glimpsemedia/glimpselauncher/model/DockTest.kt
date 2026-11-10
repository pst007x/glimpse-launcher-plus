package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockTest {
    private fun bar(dock: List<String>, left: Int = minOf(2, dock.size)) = Layout(listOf(Page()), dock, dockLeft = left)
    private val six = bar(listOf("a", "b", "c", "d", "e", "f"), 3)

    @Test fun sixPlacesShowEverything() {
        assertEquals(listOf("a", "b", "c") to listOf("d", "e", "f"), six.dockSides(6))
        assertEquals(emptyList<String>(), six.dockHidden(6))
    }

    @Test fun fourPlacesShowTheTwoNearestAllAppsAndKeepTheOuterOnes() {
        assertEquals(listOf("b", "c") to listOf("d", "e"), six.dockSides(4))
        assertEquals(listOf("a", "f"), six.dockHidden(4))
        assertEquals(six, six.copy())   // nothing is thrown away
    }

    @Test fun oldLayoutsPutTheFirstTwoOnTheLeft() {
        val old = """{"v":1,"pages":[[]],"dock":["a/A","b/B","c/C"],"cols":2}"""
        val l = LayoutJson.decode(old)!!
        assertEquals(2, l.dockLeft)
        assertEquals(listOf("a/A", "b/B") to listOf("c/C"), l.dockSides(4))
        assertEquals(six, LayoutJson.decode(LayoutJson.encode(six)))
    }

    @Test fun movingWithinASideSwapsNeighbours() {
        val l = bar(listOf("a", "b", "c", "d"))
        assertEquals(listOf("b", "a") to listOf("c", "d"), l.dockMoved("b", -1, 4).dockSides(4))
        assertEquals(listOf("a", "b") to listOf("d", "c"), l.dockMoved("c", +1, 4).dockSides(4))
    }

    @Test fun movingAcrossAllAppsUsesAFreePlaceOrSwaps() {
        // Room on the right: b crosses over and the left side gets shorter.
        val roomy = bar(listOf("a", "b", "c"))
        assertEquals(listOf("a") to listOf("b", "c"), roomy.dockMoved("b", +1, 4).dockSides(4))
        // No room: b and c change places.
        val full = bar(listOf("a", "b", "c", "d"))
        assertEquals(listOf("a", "c") to listOf("b", "d"), full.dockMoved("b", +1, 4).dockSides(4))
        assertEquals(listOf("a", "c") to listOf("b", "d"), full.dockMoved("c", -1, 4).dockSides(4))
    }

    @Test fun theEndsDoNotMove() {
        val l = bar(listOf("a", "b", "c", "d"))
        assertFalse(l.canMoveInDock("a", -1, 4)); assertFalse(l.canMoveInDock("d", +1, 4))
        assertTrue(l.canMoveInDock("b", +1, 4))
        // Hidden apps stay where they are: moving the outermost shown app never swaps with a hidden one.
        assertFalse(six.canMoveInDock("b", -1, 4)); assertFalse(six.canMoveInDock("e", +1, 4))
    }

    @Test fun addingFillsTheLeftThenTheRightUpToTheBarSize() {
        var l = bar(emptyList())
        for (k in listOf("a", "b", "c", "d", "e")) l = l.addToDock(k, 4)
        assertEquals(listOf("b", "a") to listOf("c", "d"), l.dockSides(4))
        assertEquals(4, l.dock.size)                        // the fifth does not fit in 4 places
        assertEquals(5, l.addToDock("e", 6).dock.size)       // it does in 6
        assertFalse(l.dockHasRoom(4)); assertTrue(l.dockHasRoom(6))
    }

    @Test fun removingAndUninstallingKeepEachAppOnItsSide() {
        assertEquals(listOf("a", "c") to listOf("d", "e", "f"), six.removeFromDock("b").dockSides(6))
        assertEquals(listOf("a") to listOf("e"), six.withoutMissing(setOf("a", "e")).dockSides(6))
    }

    @Test fun newSettingsDefaults() {
        val s = Settings()
        assertEquals(4, s.barSlots); assertEquals(1, s.textClockAlign)
    }

    @Test fun crossingNeverBringsAHiddenAppIntoView() {
        // 6 places hold [_ a | b c d]; with 4 shown, d is hidden. Moving b left must not uncover d: b swaps with a.
        val l = bar(listOf("a", "b", "c", "d"), 1)
        assertEquals(listOf("a") to listOf("b", "c"), l.dockSides(4)); assertEquals(listOf("d"), l.dockHidden(4))
        val moved = l.dockMoved("b", -1, 4)
        assertEquals(listOf("b") to listOf("a", "c"), moved.dockSides(4))
        assertEquals(listOf("d"), moved.dockHidden(4))
        // The mirror case: [a b c | _] with a hidden; moving c right swaps nothing in (the right side is empty), so it stays.
        val m = bar(listOf("a", "b", "c"), 3)
        assertEquals(listOf("a"), m.dockHidden(4))
        assertEquals(listOf("a"), m.dockMoved("c", +1, 4).dockHidden(4))
    }

    @Test fun aBarOfSixWithoutASplitNeverThrows() {
        assertEquals(3, Layout(listOf(Page()), listOf("a", "b", "c", "d", "e", "f")).dockLeft)
    }
}
