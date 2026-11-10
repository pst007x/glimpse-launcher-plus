package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderOrderTest {
    private fun withFolder(vararg apps: String) =
        Layout(listOf(Page(listOf(Item.App("x"), Item.Folder("f", "Games", apps.toList())))), emptyList())

    private fun Layout.folderApps() = (pages[0].items[1] as Item.Folder).apps

    @Test fun anAppMovesToANewPlaceInItsFolder() {
        assertEquals(listOf("b", "c", "a"), withFolder("a", "b", "c").moveInFolder(0, 1, "a", 2).folderApps())
        assertEquals(listOf("c", "a", "b"), withFolder("a", "b", "c").moveInFolder(0, 1, "c", 0).folderApps())
    }

    @Test fun aPlaceOutsideTheFolderIsKeptInside() {
        assertEquals(listOf("b", "c", "a"), withFolder("a", "b", "c").moveInFolder(0, 1, "a", 9).folderApps())
        assertEquals(listOf("c", "a", "b"), withFolder("a", "b", "c").moveInFolder(0, 1, "c", -3).folderApps())
    }

    @Test fun anAppNotInTheFolderOrNotAFolderChangesNothing() {
        val l = withFolder("a", "b")
        assertEquals(l, l.moveInFolder(0, 1, "zz", 0))
        assertEquals(l, l.moveInFolder(0, 0, "a", 1))
    }
}
