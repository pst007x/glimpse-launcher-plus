package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.barShown
import com.glimpsemedia.glimpselauncher.data.showBarTextFromOld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderBarClockTest {
    private val folder = Item.Folder("f1", "Games", listOf("a/A", "b/B"))
    private val l = Layout(listOf(Page(listOf(Item.App("x/X"), folder))), emptyList())

    @Test fun anAppGoesStraightIntoAFolder() {
        val after = l.addToFolder(0, 1, "c/C")!!
        assertEquals(listOf("a/A", "b/B", "c/C"), (after.pages[0].items[1] as Item.Folder).apps)
        assertEquals(l.pages[0].items[0], after.pages[0].items[0])            // nothing else moves
        assertEquals(l, l.addToFolder(0, 1, "a/A"))                            // already there: unchanged
    }

    @Test fun aFullFolderOrNotAFolderTakesNoMore() {
        val full = Layout(listOf(Page(listOf(Item.Folder("f", "F", (1..FOLDER_MAX).map { "k$it/K" })))), emptyList())
        assertNull(full.addToFolder(0, 0, "new/N"))
        assertNull(l.addToFolder(0, 0, "c/C"))                                 // an app tile, not a folder
        assertNull(l.addToFolder(0, 5, "c/C"))                                 // nothing there
    }

    @Test fun theBarHasItsOwnSwitchInEachMode() {
        assertTrue(barShown(Settings(showBar = true, showBarText = false, textMode = false)))
        assertFalse(barShown(Settings(showBar = true, showBarText = false, textMode = true)))
        assertTrue(barShown(Settings(showBar = false, showBarText = true, textMode = true)))
        assertTrue(Settings().showBarText)
    }

    @Test fun theTextModeSwitchStartsAsTheOldOne() {
        assertFalse(showBarTextFromOld(stored = null, showBar = false))   // the bar was off everywhere: stays off in text mode
        assertTrue(showBarTextFromOld(stored = null, showBar = true))
        assertFalse(showBarTextFromOld(stored = false, showBar = true))   // once chosen, its own value
    }

}
