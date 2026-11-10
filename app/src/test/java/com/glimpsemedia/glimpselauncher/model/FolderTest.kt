package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTest {
    private fun apps(vararg k: String) = k.map { Item.App(it) }
    private fun layout(vararg pages: List<Item>) = Layout(pages.map { Page(it) }, emptyList())

    @Test fun anAppDroppedOnAnotherMakesAFolderWhereTheTargetWas() {
        val l = layout(apps("a", "b", "c")).dropOnto(0, 0, 0, 2, "f1")!!
        assertEquals(listOf(Item.App("b"), Item.Folder("f1", "Folder", listOf("c", "a"))), l.pages[0].items)
    }

    @Test fun droppingAnEarlierOrLaterAppGivesTheSameShape() {
        val l = layout(apps("a", "b", "c")).dropOnto(0, 2, 0, 0, "f1")!!
        assertEquals(listOf(Item.Folder("f1", "Folder", listOf("a", "c")), Item.App("b")), l.pages[0].items)
    }

    @Test fun dropAcrossPagesTakesTheAppFromItsPage() {
        val l = layout(apps("a"), apps("b")).dropOnto(0, 0, 1, 0, "f1")!!
        assertEquals(emptyList<Item>(), l.pages[0].items)
        assertEquals(listOf(Item.Folder("f1", "Folder", listOf("b", "a"))), l.pages[1].items)
    }

    @Test fun anAppJoinsAFolderUntilItIsFull() {
        val full = Item.Folder("f", "Games", (1..8).map { "g$it" })
        assertNull(layout(listOf(Item.App("x"), full)).dropOnto(0, 0, 0, 1, "n"))
        val room = Item.Folder("f", "Games", listOf("g1", "g2"))
        val l = layout(listOf(Item.App("x"), room)).dropOnto(0, 0, 0, 1, "n")!!
        assertEquals(listOf(Item.Folder("f", "Games", listOf("g1", "g2", "x"))), l.pages[0].items)
    }

    @Test fun onlyAppsGoIntoFoldersAndNotOntoAllApps() {
        assertNull(layout(listOf(Item.AllApps, Item.App("a"))).dropOnto(0, 0, 0, 1, "n"))
        assertNull(layout(listOf(Item.App("a"), Item.AllApps)).dropOnto(0, 0, 0, 1, "n"))
        assertNull(layout(listOf(Item.Folder("f", "F", listOf("x", "y")), Item.Folder("g", "G", listOf("p", "q")))).dropOnto(0, 0, 0, 1, "n"))
        assertNull(layout(apps("a")).dropOnto(0, 0, 0, 0, "n"))
    }

    @Test fun takingAnAppOutKeepsTheFolderUntilOneIsLeft() {
        val l = layout(listOf(Item.Folder("f", "F", listOf("a", "b", "c"))))
        val one = l.takeOutOfFolder(0, 0, "b")
        assertEquals(listOf(Item.Folder("f", "F", listOf("a", "c")), Item.App("b")), one.pages[0].items)
        val two = one.takeOutOfFolder(0, 0, "a")
        assertEquals(listOf(Item.App("c"), Item.App("b"), Item.App("a")), two.pages[0].items)
    }

    @Test fun anAppTakenOutOfAFullPageGoesToAnotherPage() {
        val items = listOf<Item>(Item.Folder("f", "F", listOf("a", "b"))) + apps("1", "2", "3", "4", "5", "6", "7")
        val l = layout(items).takeOutOfFolder(0, 0, "a")
        assertEquals(8, l.pages[0].items.size)
        assertEquals(listOf(Item.App("a")), l.pages[1].items)
    }

    @Test fun renamingTrimsAndKeepsAName() {
        val l = layout(listOf(Item.Folder("f", "F", listOf("a", "b"))))
        assertEquals("Family", (l.renameFolder(0, 0, "  Family ").pages[0].items[0] as Item.Folder).name)
        assertEquals("Folder", (l.renameFolder(0, 0, "   ").pages[0].items[0] as Item.Folder).name)
    }
}

class TileColorTest {
    private fun layout() = Layout(
        listOf(Page(listOf(Item.App("a"), Item.Folder("f1", "Games", listOf("b", "c")), Item.AllApps))),
        emptyList(),
    )

    @Test fun aTileCanHaveItsOwnColourAndGoBackToStandard() {
        val colored = layout().withTileColor(Item.App("a"), 0xFFFF0000.toInt()).withTileColor(Item.AllApps, 0xFF00FF00.toInt())
        assertEquals(mapOf("a" to 0xFFFF0000.toInt(), "all" to 0xFF00FF00.toInt()), colored.tileColors)
        assertEquals(mapOf("all" to 0xFF00FF00.toInt()), colored.withTileColor(Item.App("a"), 0).tileColors)
    }

    @Test fun aFolderIsKeptByItsId() {
        val l = layout().withTileColor(Item.Folder("f1", "Games", emptyList()), 5)
        assertEquals(mapOf("f:f1" to 5), l.tileColors)
        val renamed = l.renameFolder(0, 1, "Family")
        assertEquals(mapOf("f:f1" to 5), renamed.tileColors)
    }

    @Test fun coloursOfRemovedTilesGoAway() {
        val l = layout().withTileColor(Item.App("a"), 1).withTileColor(Item.App("gone"), 2)
        assertEquals(mapOf("a" to 1), l.withoutStrayColors().tileColors)
        assertEquals(emptyMap<String, Int>(), l.removeFromPage(0, 0).withoutStrayColors().tileColors - "gone")
    }

    @Test fun coloursSurviveSavingAndMovingTiles() {
        val l = layout().withTileColor(Item.App("a"), 0xFF123456.toInt())
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
        assertEquals(l.tileColors, l.moveItem(0, 0, 0, 2)!!.tileColors)
    }
}

class TileTransparencyTest {
    private fun layout() = Layout(listOf(Page(listOf(Item.App("a"), Item.App("b")))), emptyList())

    @Test fun aTileCanHaveItsOwnTransparencyAndGoBackToTheSetting() {
        val l = layout().withTileTransparency(Item.App("a"), 50).withTileTransparency(Item.App("b"), 100)
        assertEquals(mapOf("a" to 50, "b" to 100), l.tileTransparency)
        assertEquals(mapOf("b" to 100), l.withTileTransparency(Item.App("a"), null).tileTransparency)
    }

    @Test fun transparencyIsSavedAndPrunedWithTheTile() {
        val l = layout().withTileTransparency(Item.App("a"), 25).withTileTransparency(Item.App("gone"), 75)
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
        assertEquals(mapOf("a" to 25), l.withoutStrayColors().tileTransparency)
    }
}
