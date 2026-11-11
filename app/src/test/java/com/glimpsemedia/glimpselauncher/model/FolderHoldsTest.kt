package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderHoldsTest {
    private val a = Item.App("a/A")
    private val b = Item.App("b/B")
    private val start = Layout(listOf(Page(listOf(a, b))), emptyList())
        .withHoldAction("a/A", HoldAction.Web("https://example.com"))
        .withHoldAction("b/B", HoldAction.Call("123"))

    @Test fun makingAFolderDropsBothAppsHoldActions() {
        val l = start.dropOnto(0, 1, 0, 0, "f1")!!.withoutStrayHolds()
        assertTrue(l.pages[0].items.first() is Item.Folder)
        assertEquals(emptyMap<String, HoldAction>(), l.holdActions)   // they belonged to the tiles, which are gone
    }

    @Test fun theFolderCanHaveItsOwnHoldActionAndLosesItWhenTakenApart() {
        val folder = start.dropOnto(0, 1, 0, 0, "f1")!!.withoutStrayHolds()
        val f = folder.pages[0].items.first() as Item.Folder
        val held = folder.withHoldAction(f.holdKey!!, HoldAction.Web("https://example.org"))
        assertEquals(1, held.holdActions.size)
        val apart = held.takeOutOfFolder(0, 0, "a/A").withoutStrayHolds()
        assertEquals(setOf("a/A", "b/B"), apart.pages[0].items.filterIsInstance<Item.App>().map { it.key }.toSet())
        assertEquals(emptyMap<String, HoldAction>(), apart.holdActions)   // the folder is gone, and the apps come back plain
    }

    @Test fun anAppTakenOutOfABiggerFolderComesBackWithNoAction() {
        val three = Layout(listOf(Page(listOf(Item.Folder("f1", "F", listOf("a/A", "b/B", "c/C"))))), emptyList())
            .withHoldAction("f:f1", HoldAction.Call("1"))
        val out = three.takeOutOfFolder(0, 0, "c/C").withoutStrayHolds()
        assertEquals(setOf("f:f1"), out.holdActions.keys)   // the folder keeps its own
    }
}
