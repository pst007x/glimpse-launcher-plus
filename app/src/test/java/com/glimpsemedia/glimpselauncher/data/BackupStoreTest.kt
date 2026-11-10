package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class BackupStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val base = BackupManifest(RELEASE_APP, Backup.FORMAT, "1.0.3", 5L, 0, 0, false)
    private val notesJson = """{"v":1,"notes":[{"id":"n1","t":"A","c":0,"f":false,"lines":[],"p":["p1.jpg"]},{"id":"n2","t":"B","c":0,"f":false,"lines":[],"p":[]}]}"""
    private val themesJson = """{"v":1,"themes":[{"id":"t1","n":"Mine","look":{"dark":true}}]}"""

    private fun phone(name: String): File = tmp.newFolder(name).also { d ->
        File(d, "layout.json").writeText("""{"v":1,"pages":[[]],"dock":[],"cols":2}""")
        File(d, "notes.json").writeText(notesJson)
        File(d, "themes.json").writeText(themesJson)
        File(d, "notes").mkdirs(); File(d, "notes/p1.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(d, "wallpapers").mkdirs(); File(d, "wallpapers/w1.img").writeBytes(byteArrayOf(4, 5))
    }

    /** "My" phone before a restore: its own files, picture and settings. */
    private fun mine(): File = tmp.newFolder("to").also { to ->
        File(to, "layout.json").writeText("MINE"); File(to, "notes.json").writeText("MY NOTES")
        File(to, "notes").mkdirs(); File(to, "notes/mine.jpg").writeBytes(byteArrayOf(7))
    }
    private fun assertMine(to: File) {
        assertEquals("MINE", File(to, "layout.json").readText())
        assertEquals("MY NOTES", File(to, "notes.json").readText())
        assertArrayEquals(byteArrayOf(7), File(to, "notes/mine.jpg").readBytes())
        assertFalse(File(to, "themes.json").exists())
        assertFalse(File(to, "notes/p1.jpg").exists())
    }
    private val myPrefs = mapOf("darkTheme" to PrefValue.B(true), "pinHash" to PrefValue.S("mine"))

    @Test fun collectThenRestoreRoundTrips() {
        val from = phone("from")
        val prefs = mapOf("darkTheme" to PrefValue.B(false), "pinHash" to PrefValue.S("h"))
        val c = BackupStore(from).collect(base, prefs)
        val to = tmp.newFolder("to")
        File(to, "notes").mkdirs(); File(to, "notes/old.jpg").writeBytes(byteArrayOf(9))
        var written: Map<String, PrefValue>? = null
        assertEquals(RestoreResult.Done, BackupStore(to).restore(c, writePrefs = { written = it }, currentPrefs = emptyMap()))
        for (f in listOf("layout.json", "notes.json", "themes.json", "notes/p1.jpg", "wallpapers/w1.img")) {
            assertArrayEquals(f, File(from, f).readBytes(), File(to, f).readBytes())
        }
        assertFalse(File(to, "notes/old.jpg").exists())     // the backup's pictures replace the old ones
        assertFalse(File(to, "textmode.json").exists())     // none in the backup: none after
        assertEquals(prefs, written)
        assertFalse(File(to, "before-restore").exists())    // the safety copy goes once the restore is done
    }

    @Test fun failedRestorePutsEverythingBack() {
        val c = BackupStore(phone("from")).collect(base, mapOf("darkTheme" to PrefValue.B(false)))
        val to = mine()
        var prefsNow: Map<String, PrefValue> = myPrefs
        var count = 0
        val result = BackupStore(to).restore(c, writePrefs = { prefsNow = it }, currentPrefs = myPrefs,
            copy = { from, dest -> if (++count == 2) throw IOException("disk full") else from.copyTo(dest, overwrite = true) })
        assertEquals(RestoreResult.Failed, result)
        assertMine(to)
        assertEquals(myPrefs, prefsNow)
        assertFalse(File(to, "before-restore").exists())
    }

    @Test fun noRoomForTheSafetyCopyChangesNothing() {
        val c = BackupStore(phone("from")).collect(base, emptyMap())
        val to = mine()
        to.setWritable(false)   // like a full disk: the safety copy cannot be made
        var prefsWritten = false
        try {
            assertEquals(RestoreResult.NoRoom, BackupStore(to).restore(c, writePrefs = { prefsWritten = true }, currentPrefs = myPrefs))
        } finally {
            to.setWritable(true)
        }
        assertMine(to)
        assertFalse(prefsWritten)
    }

    @Test fun aRestoreStoppedPartWayIsUndoneOnTheNextStart() {
        val to = mine()
        // A restore that got as far as the safety copy and the mark, then the phone closed the launcher mid-copy.
        val safe = File(to, "before-restore").also { it.mkdirs() }
        File(to, "layout.json").copyTo(File(safe, "layout.json")); File(to, "notes.json").copyTo(File(safe, "notes.json"))
        File(to, "notes/mine.jpg").copyTo(File(safe, "notes/mine.jpg"))
        File(safe, "settings.json").writeText(Backup.encodeSettings(myPrefs))
        File(safe, "in-progress").writeText("restoring")
        File(to, "layout.json").writeText("HALF RESTORED"); File(to, "notes.json").delete(); File(to, "themes.json").writeText("{}")
        var prefsNow: Map<String, PrefValue> = emptyMap()
        assertTrue(BackupStore(to).recoverIfInterrupted { prefsNow = it })
        assertMine(to)
        assertEquals(myPrefs, prefsNow)
        assertFalse(safe.exists())
        assertFalse(BackupStore(to).recoverIfInterrupted { prefsNow = emptyMap() })   // nothing more to do
        assertEquals(myPrefs, prefsNow)
    }

    @Test fun aLeftoverSafetyCopyWithoutTheMarkIsRemoved() {
        val to = mine()
        File(to, "before-restore/notes").mkdirs(); File(to, "before-restore/notes/old.jpg").writeBytes(byteArrayOf(1))
        assertFalse(BackupStore(to).recoverIfInterrupted { })
        assertFalse(File(to, "before-restore").exists())
        assertMine(to)
    }

    @Test fun missingPictureIsSkipped() {
        val from = phone("from")
        File(from, "notes/p1.jpg").delete()
        val c = BackupStore(from).collect(base, emptyMap())
        assertFalse(c.files.containsKey("notes/p1.jpg"))
        assertTrue(c.files.containsKey("notes.json"))
    }

    @Test fun counts() {
        val c = BackupStore(phone("from")).collect(base, mapOf("pinHash" to PrefValue.S("h")))
        assertEquals(2, c.manifest.notes); assertEquals(1, c.manifest.themes); assertTrue(c.manifest.pin)
    }

    @Test fun aBackupOfASmallerGridSaysItNeedsANewerApp() {
        // An older Glimpse Launcher cannot read 4×5, 5×5 or 5×6 and would lose the screens: such a backup is format 2,
        // which older apps refuse with "from a newer Glimpse Launcher". Other backups stay format 1 for them.
        val tight = phone("tight").also { d ->
            File(d, "layout.json").writeText(com.glimpsemedia.glimpselauncher.model.LayoutJson.encode(
                com.glimpsemedia.glimpselauncher.model.Layout(listOf(com.glimpsemedia.glimpselauncher.model.Page(columns = 5, rows = 6)), emptyList(), columns = 5, rows = 6)))
        }
        assertEquals(2, BackupStore(tight).collect(base, emptyMap()).manifest.format)
        assertEquals(1, BackupStore(phone("usual")).collect(base, emptyMap()).manifest.format)
    }
}
