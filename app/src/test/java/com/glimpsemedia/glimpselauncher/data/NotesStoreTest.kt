package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.NOTE_LINE_COLORS
import com.glimpsemedia.glimpselauncher.model.Note
import com.glimpsemedia.glimpselauncher.model.NoteLine
import com.glimpsemedia.glimpselauncher.model.Notes
import com.glimpsemedia.glimpselauncher.model.NotesJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NotesStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun savedAndReadBack() {
        val s = NotesStore(tmp.root)
        val n = Notes(listOf(Note("a", "Shop", 0xFF90CAF9.toInt(), folded = true,
            lines = listOf(NoteLine("milk ☑ \"quoted\"", true, NOTE_LINE_COLORS[2]), NoteLine("")), pictures = listOf("x.jpg"))))
        s.save(n)
        assertEquals(n, NotesStore(tmp.root).load())
        assertFalse(File(tmp.root, "notes.json.tmp").exists())
    }

    @Test fun missingFileIsNoNotes() { assertEquals(Notes(), NotesStore(tmp.root).load()) }

    @Test fun unreadableFileIsKeptAside() {
        File(tmp.root, "notes.json").writeText("{ not json")
        assertEquals(Notes(), NotesStore(tmp.root).load())
        assertEquals("{ not json", File(tmp.root, "notes.json.bad").readText())
    }

    @Test fun orphanPicturesAreFound() {
        val n = Notes(listOf(Note("a", pictures = listOf("keep.jpg"))))
        assertEquals(setOf("old.jpg"), orphanPictures(listOf("keep.jpg", "old.jpg"), n))
    }

    @Test fun removingOrphansDeletesOnlyThem() {
        val s = NotesStore(tmp.root)
        val keep = s.newPictureFile().apply { writeText("k") }
        val old = s.newPictureFile().apply { writeText("o") }
        assertEquals(1, s.removeOrphans(Notes(listOf(Note("a", pictures = listOf(keep.name))))))
        assertTrue(keep.exists()); assertFalse(old.exists())
    }

    @Test fun aNewerVersionIsNotRead() { assertNull(NotesJson.decode("""{"v":2,"notes":[]}""")) }

    @Test fun pictureNamesThatLeaveTheFolderAreDropped() {
        val text = """{"v":1,"notes":[{"id":"a","t":"","c":0,"f":false,"lines":[],"p":["ok.jpg","../x.jpg","a/b.jpg",".."]}]}"""
        assertEquals(listOf("ok.jpg"), NotesJson.decode(text)!!.list[0].pictures)
    }

    @Test fun anUnreadableFileKeepsEveryPicture() {
        val s = NotesStore(tmp.root)
        val pic = s.newPictureFile().apply { writeText("photo") }
        File(tmp.root, "notes.json").writeText("""{"v":1,"notes":[{"id":"a","p":["${pic.name}"]""")  // cut short
        val loaded = s.load()
        assertTrue(s.setAside)
        if (!s.setAside) s.removeOrphans(loaded)
        assertTrue(pic.exists())
        assertEquals(0, s.removeOrphansIfSafe(loaded))
        assertTrue(pic.exists())
    }

    @Test fun duplicateIdsAreMadeUnique() {
        val text = """{"v":1,"notes":[{"id":"a","t":"one"},{"id":"a","t":"two"}]}"""
        val ids = NotesJson.decode(text)!!.list.map { it.id }
        assertEquals(2, ids.toSet().size)
    }
}
