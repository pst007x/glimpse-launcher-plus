package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {
    private val empty = Layout(listOf(Page()), emptyList())

    @Test fun aNewTagIsAddedOnceWithItsNameTidied() {
        val l = empty.withNewTag("  Games ").withNewTag("games")
        assertEquals(listOf(AppTag("Games", emptySet())), l.tags)
    }

    @Test fun aBlankTagNameAddsNothing() {
        assertEquals(emptyList<AppTag>(), empty.withNewTag("   ").tags)
    }

    @Test fun anAppCanHaveSeveralTagsAndLoseOne() {
        val l = empty.withNewTag("Games").withNewTag("Family")
            .withAppTagged("chess", "Games", true).withAppTagged("chess", "Family", true)
            .withAppTagged("chess", "Games", false)
        assertEquals(setOf("Family"), l.tagsOf("chess"))
        assertEquals(setOf("chess"), l.appsTagged("Family"))
        assertEquals(emptySet<String>(), l.appsTagged("Games"))
    }

    @Test fun taggingWithAnUnknownTagMakesIt() {
        val l = empty.withAppTagged("chess", "Games", true)
        assertEquals(listOf(AppTag("Games", setOf("chess"))), l.tags)
    }

    @Test fun renamingKeepsTheAppsAndRefusesADuplicate() {
        val l = empty.withAppTagged("chess", "Games", true).withNewTag("Family")
        assertEquals(setOf("chess"), l.withTagRenamed("Games", "Play").appsTagged("Play"))
        assertEquals(l, l.withTagRenamed("Games", "family"))
        assertEquals(l, l.withTagRenamed("Games", "  "))
    }

    @Test fun deletingATagLeavesTheAppsAlone() {
        val l = empty.withAppTagged("chess", "Games", true).add(Item.App("chess")).withoutTag("Games")
        assertEquals(emptyList<AppTag>(), l.tags)
        assertTrue("chess" in l.placedKeys())
    }

    @Test fun uninstalledAppsLeaveTheirTagsButTheTagStays() {
        val l = empty.withAppTagged("chess", "Games", true).withAppTagged("cards", "Games", true)
            .withoutMissing(setOf("cards"))
        assertEquals(listOf(AppTag("Games", setOf("cards"))), l.tags)
    }

    @Test fun uninstalledAppsKeepTheOtherTilesColours() {
        val l = Layout(listOf(Page(listOf(Item.App("a"), Item.App("b")))), emptyList())
            .withTileColor(Item.App("a"), 0xFF112233.toInt()).withTileTransparency(Item.App("a"), 50)
            .withIconColor(Item.App("a"), 0xFF445566.toInt()).withIconTransparency(Item.App("a"), 25)
            .withoutMissing(setOf("a"))
        assertEquals(0xFF112233.toInt(), l.tileColors["a"])
        assertEquals(50, l.tileTransparency["a"])
        assertEquals(0xFF445566.toInt(), l.iconColors["a"])
        assertEquals(25, l.iconTransparency["a"])
    }

    @Test fun tagsAndIconSquaresAreSavedAndReadBack() {
        val l = Layout(listOf(Page(listOf(Item.App("a")))), emptyList())
            .withAppTagged("a", "Games", true).withNewTag("Empty")
            .withIconColor(Item.App("a"), 0xFF445566.toInt()).withIconTransparency(Item.App("a"), 75)
        assertEquals(l, LayoutJson.decode(LayoutJson.encode(l)))
    }

    @Test fun anOldSaveWithoutTagsStillReads() {
        val old = """{"v":1,"pages":[[{"t":"app","k":"a"}]],"dock":[],"colors":{},"see":{}}"""
        val l = LayoutJson.decode(old)!!
        assertEquals(emptyList<AppTag>(), l.tags)
        assertEquals(emptyMap<String, Int>(), l.iconColors)
    }

    @Test fun anIconSquareColourOfZeroGoesBackToTheSetting() {
        val l = empty.withIconColor(Item.App("a"), 0xFF445566.toInt()).withIconColor(Item.App("a"), 0)
            .withIconTransparency(Item.App("a"), 25).withIconTransparency(Item.App("a"), null)
        assertEquals(emptyMap<String, Int>(), l.iconColors)
        assertEquals(emptyMap<String, Int>(), l.iconTransparency)
    }
}
