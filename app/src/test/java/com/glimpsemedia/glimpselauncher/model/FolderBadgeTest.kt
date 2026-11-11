package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderBadgeTest {
    private val folder = Item.Folder("f1", "Games", listOf("a/A", "b/B"))
    private val other = Item.Folder("f2", "Media", listOf("c/C"))
    private val layout = Layout(listOf(Page(listOf(folder, other, Item.App("d/D")))), emptyList())

    @Test fun aFolderCanHaveABadge() {
        val l = layout.withFolderBadge(folder, TextIcon.Music)
        assertEquals(TextIcon.Music, l.badgeOf(folder))
        assertEquals(TextIcon.Games, l.withFolderBadge(folder, TextIcon.Games).badgeOf(folder))
        assertNull(l.withFolderBadge(folder, null).badgeOf(folder))
        assertNull(l.badgeOf(other))
    }

    @Test fun badgesAreSavedAndReadBack() {
        val l = layout.withFolderBadge(folder, TextIcon.Music).withFolderBadge(other, TextIcon.Work)
        assertEquals(l.folderBadges, LayoutJson.decode(LayoutJson.encode(l))!!.folderBadges)
    }

    @Test fun unknownOrMissingBadgesReadAsNone() {
        val plain = LayoutJson.encode(layout).replace(",\"folderBadges\":{}", "")
        assertEquals(emptyMap<String, String>(), LayoutJson.decode(plain)!!.folderBadges)
        val odd = LayoutJson.encode(layout.withFolderBadge(folder, TextIcon.Music)).replace("\"Music\"", "\"Banjo\"")
        assertNull(LayoutJson.decode(odd)!!.badgeOf(folder))
        assertEquals(emptyMap<String, String>(), LayoutJson.decode(odd)!!.folderBadges)
    }

    @Test fun badgeIsDroppedWhenItsFolderGoes() {
        val l = layout.withFolderBadge(folder, TextIcon.Music)
        val without = l.copy(pages = listOf(Page(listOf(other, Item.App("d/D"))))).withoutStrayColors()
        assertEquals(emptyMap<String, String>(), without.folderBadges)
    }

    @Test fun gridSwitchKeepsTheBadge() {
        val l = layout.withFolderBadge(folder, TextIcon.Music)
        assertEquals(TextIcon.Music, l.withColumns(4).withColumns(2).badgeOf(folder))
    }

    @Test fun menuLineRule() {
        assertEquals("Add a badge", badgeMenuLine(folder, folderStyle = 1, badge = null))
        assertEquals("Change the badge", badgeMenuLine(folder, folderStyle = 1, badge = TextIcon.Music))
        assertNull(badgeMenuLine(folder, folderStyle = 0, badge = null))
        assertNull(badgeMenuLine(Item.App("d/D"), folderStyle = 1, badge = null))
    }

    @Test fun colourBadgeSavedAndRead() {
        val l = layout.withColourBadge(folder, TextIcon.Music)
        assertEquals(TextIcon.Music, l.colourBadgeOf(folder))
        assertNull(l.badgeOf(folder))                                   // not a mono badge
        assertEquals(TextIcon.Music, LayoutJson.decode(LayoutJson.encode(l))!!.colourBadgeOf(folder))
        assertEquals(TextIcon.Games, l.withFolderBadge(folder, TextIcon.Games).badgeOf(folder))   // a mono one replaces it
        assertNull(l.withFolderBadge(folder, TextIcon.Games).colourBadgeOf(folder))
        assertEquals("Change the badge", badgeMenuLine(folder, 1, l.anyBadgeOf(folder)))
    }

    @Test fun unknownColourBadgeIsNone() {
        val odd = LayoutJson.encode(layout.withColourBadge(folder, TextIcon.Music)).replace("color:Music", "color:Banjo")
        assertNull(LayoutJson.decode(odd)!!.colourBadgeOf(folder))
        assertEquals(emptyMap<String, String>(), LayoutJson.decode(odd)!!.folderBadges)
    }
}
