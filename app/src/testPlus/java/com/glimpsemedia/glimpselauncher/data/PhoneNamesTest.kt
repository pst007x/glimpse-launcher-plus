package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.walk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhoneNamesTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Packed down, the walk's names give back exactly the same entries: names, folders, full paths, types, places. */
    @Test fun packedNamesGiveBackTheSameEntries() {
        listOf("Download/Recipes/cake.txt", "Download/Recipes/pie.jpg", "DCIM/Camera/a.jpg", "notes.txt", "Documents/My Café #1/x.pdf")
            .forEach { File(tmp.root, it).apply { parentFile!!.mkdirs(); writeText("x") } }
        val lookup: (String) -> String? = { mapOf("txt" to "text/plain", "jpg" to "image/jpeg", "pdf" to "application/pdf")[it] }
        val found = walk(listOf(phoneRoot(tmp.root)), FileTree(tmp.root, lookup)).entries
        val packed = PhoneNames.of(found, tmp.root.absolutePath, lookup)
        assertEquals(found, packed.toList())
        assertEquals(found.size, packed.size)
    }
}
