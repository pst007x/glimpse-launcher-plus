package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.FOLDER_PICTURES
import com.glimpsemedia.glimpselauncher.data.folderIconFromStored
import com.glimpsemedia.glimpselauncher.data.folderStyleFromStored
import com.glimpsemedia.glimpselauncher.ui.SettingsMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderLookTest {
    private val folder = Item.Folder("f1", "Games", listOf("a/A", "b/B"))
    private val other = Item.Folder("f2", "Media", listOf("c/C"))
    private val layout = Layout(listOf(Page(listOf(folder, other, Item.App("d/D")))), emptyList())

    @Test fun aFolderCanHaveItsOwnColour() {
        val red = 0xFFCC2222.toInt()
        val l = layout.withFolderColor(folder, red)
        assertEquals(mapOf("f:f1" to red), l.folderColors)
        assertEquals(emptyMap<String, Int>(), l.withFolderColor(folder, 0).folderColors)   // back to the shared colour
    }

    @Test fun folderColoursAreSavedAndReadBack() {
        val l = layout.withFolderColor(folder, 0xFF2266CC.toInt()).withFolderColor(other, 0xFF22AA44.toInt())
        assertEquals(l.folderColors, LayoutJson.decode(LayoutJson.encode(l))!!.folderColors)
    }

    @Test fun aRemovedFolderLosesItsColour() {
        val l = layout.withFolderColor(folder, 0xFF2266CC.toInt())
        val without = l.copy(pages = listOf(Page(listOf(other, Item.App("d/D"))))).withoutStrayColors()
        assertEquals(emptyMap<String, Int>(), without.folderColors)
    }

    @Test fun oldLayoutsHaveNoFolderColours() {
        val text = LayoutJson.encode(layout).replace(",\"folderColors\":{}", "")
        assertEquals(emptyMap<String, Int>(), LayoutJson.decode(text)!!.folderColors)
    }

    @Test fun foldersLookAsTheyAlwaysDidUntilChosen() {
        assertEquals(0, Settings().folderStyle)
        assertEquals(0, Settings().folderColor)
        assertEquals(listOf(0, 1, 0, 0), listOf(0, 1, 2, -1).map { folderStyleFromStored(it) })
    }

    @Test fun folderSettingsAreSearchable() {
        assertEquals(listOf("Folder icon", "Folder picture", "Folder colour"), SettingsMap.sections.getValue("tiles").blocks.first { it.heading == "Folders" }.rows)
        assertTrue(SettingsMap.search("folder colour").any { it.label == "Folder colour" })
        assertTrue(SettingsMap.search("folder picture").any { it.label == "Folder picture" })
    }

    @Test fun thereAreSixFolderPicturesAndThirdIsTheDefault() {
        assertEquals(6, FOLDER_PICTURES)
        assertEquals(3, Settings().folderIcon)
    }

    @Test fun aStoredFolderPictureIsOneToSix() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 3, 3, 3), listOf(1, 2, 3, 4, 5, 6, 0, 7, -1).map { folderIconFromStored(it) })
    }
}
