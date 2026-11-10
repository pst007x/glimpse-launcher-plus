package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {
    private fun keys(n: Int) = (1..n).map { "pkg$it/Main" }

    @Test fun pagesHoldAtMostEightItems() {
        assertThrows(IllegalArgumentException::class.java) {
            Page((1..9).map { Item.App("k$it") })
        }
        assertTrue(Page((1..8).map { Item.App("k$it") }).isFull)
    }

    @Test fun bottomBarHoldsAtMostSixThreeEachSide() {
        assertThrows(IllegalArgumentException::class.java) { Layout(listOf(Page()), keys(7), dockLeft = 3) }
        assertThrows(IllegalArgumentException::class.java) { Layout(listOf(Page()), keys(5), dockLeft = 1) }
        Layout(listOf(Page()), keys(6), dockLeft = 3)
    }

    @Test fun thereIsAlwaysOnePage() {
        assertThrows(IllegalArgumentException::class.java) { Layout(emptyList(), emptyList()) }
        assertEquals(1, DefaultLayout.build(emptyList(), emptyList()).pages.size)
    }

    @Test fun theFirstLayoutIsCleanWithOnlyTheBottomBarFilled() {
        val layout = DefaultLayout.build(keys(20), listOf("pkg3/Main", "pkg1/Main"))
        assertEquals(1, layout.pages.size)
        assertTrue(layout.pages[0].isEmpty)
        assertEquals(listOf("pkg3/Main", "pkg1/Main"), layout.dock)
    }

    @Test fun twentyAppsMakeThreePagesOfEightEightFourWhenFilled() {
        val layout = DefaultLayout.buildFilled(keys(20), emptyList())
        assertEquals(listOf(8, 8, 4), layout.pages.map { it.items.size })
    }

    @Test fun bottomBarAppsAreNotRepeatedOnPages() {
        val layout = DefaultLayout.buildFilled(keys(10), listOf("pkg3/Main", "pkg1/Main", "missing/Main"))
        assertEquals(listOf("pkg3/Main", "pkg1/Main"), layout.dock)
        val onPages = layout.pages.flatMap { it.items }.filterIsInstance<Item.App>().map { it.key }
        assertEquals(8, onPages.size)
        assertTrue("pkg3/Main" !in onPages && "pkg1/Main" !in onPages)
    }

    @Test fun dockDefaultsAreCappedAtFour() {
        val layout = DefaultLayout.build(keys(10), keys(6))
        assertEquals(4, layout.dock.size)
    }

    @Test fun uninstalledAppsAreDroppedEverywhere() {
        val layout = Layout(
            listOf(Page(listOf(Item.App("a/Main"), Item.Folder("f1", "Games", listOf("b/Main", "c/Main")),
                Item.Folder("f2", "Gone", listOf("d/Main"))))),
            listOf("a/Main", "d/Main"),
        )
        val kept = layout.withoutMissing(setOf("a/Main", "c/Main"))
        assertEquals(
            listOf(Item.App("a/Main"), Item.Folder("f1", "Games", listOf("c/Main"))),
            kept.pages[0].items,
        )
        assertEquals(listOf("a/Main"), kept.dock)
        assertEquals(setOf("a/Main", "c/Main"), kept.placedKeys())
    }
}
