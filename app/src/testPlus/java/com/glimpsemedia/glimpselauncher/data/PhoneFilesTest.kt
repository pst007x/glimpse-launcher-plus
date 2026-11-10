package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.walk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class PhoneFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(rel: String) = File(tmp.root, rel).apply { parentFile!!.mkdirs(); writeText("x") }
    private fun dir(rel: String) = File(tmp.root, rel).apply { mkdirs() }
    private fun names(tree: FileTree = FileTree(tmp.root)) = walk(listOf(phoneRoot(tmp.root)), tree).entries.map { it.name }

    @Test fun namesAndPathsUnderTheRoot() {
        val cake = file("Download/Recipes/cake.txt")
        val found = walk(listOf(phoneRoot(tmp.root)), FileTree(tmp.root)).entries
        val e = found.single { it.name == "cake.txt" }
        assertEquals(cake.absolutePath, e.id)
        assertEquals("Download/Recipes", e.path)
        assertFalse(e.isFolder)
        assertEquals("", found.single { it.name == "Download" }.path)
        assertTrue(found.single { it.name == "Recipes" }.isFolder)
        assertEquals("Phone", found.first().name)
    }

    @Test fun hiddenNamesAreSkipped() {
        dir("DCIM/.thumbnails"); file("DCIM/.nomedia"); file("DCIM/photo.jpg")
        val n = names()
        assertTrue("photo.jpg" in n); assertFalse(".thumbnails" in n); assertFalse(".nomedia" in n)
    }

    @Test fun androidDataAndObbAreSkipped() {
        file("Android/data/x.txt"); file("Android/obb/y.obb"); file("Android/media/z.txt")
        val n = names()
        assertFalse("data" in n); assertFalse("obb" in n); assertFalse("x.txt" in n); assertTrue("z.txt" in n)
    }

    @Test fun symbolicLinksAreNotFollowed() {
        file("Music/song.mp3")
        Files.createSymbolicLink(File(tmp.root, "Music/loop").toPath(), tmp.root.toPath())
        val n = names()
        assertFalse("loop" in n)
        assertEquals(1, n.count { it == "song.mp3" })
    }

    @Test fun unreadableFolderIsSkipped() {
        file("Locked/secret.txt"); file("Open/fine.txt")
        val tree = object : FileTree(tmp.root) {
            override fun list(dir: File): Array<File>? = if (dir.name == "Locked") null else super.list(dir)
        }
        val n = names(tree)
        assertTrue("fine.txt" in n); assertTrue("Locked" in n); assertFalse("secret.txt" in n)
    }

    @Test fun limitsMarkTheWalkCut() {
        (1..5).forEach { file("Documents/f$it.txt") }
        assertTrue(walk(listOf(phoneRoot(tmp.root)), FileTree(tmp.root), maxEntries = 3).cut)
        assertEquals(200_000, PHONE_MAX_ENTRIES); assertEquals(20, PHONE_MAX_DEPTH)
    }

    @Test fun mimeFromTheExtension() {
        assertEquals("image/jpeg", mimeOf("a.JPG") { if (it == "jpg") "image/jpeg" else null })
        assertEquals("*/*", mimeOf("a.zzz") { null })
        assertEquals("*/*", mimeOf("README") { null })
    }

    @Test fun externalDocIdKeepsTheNameAsIs() = assertEquals("primary:Download/My Café #1", externalDocId("Download/My Café #1"))
}
