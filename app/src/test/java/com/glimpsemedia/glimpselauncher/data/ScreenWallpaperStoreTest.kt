package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.PicturePlace
import com.glimpsemedia.glimpselauncher.model.ScreenPicture
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenWallpaperStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val base = BackupManifest(RELEASE_APP, Backup.FORMAT, "1.0.17", 18L, 0, 0, false)

    @Test fun backupsTakeScreensJsonAndItsPictures() {
        assertTrue(Backup.isAllowedName("screens.json"))
        assertTrue(Backup.isAllowedName("screens/pic-1.jpg"))
        assertFalse(Backup.isAllowedName("screens/../x.jpg"))
    }

    @Test fun backupRoundTripsTheScreenPictures() {
        val from = tmp.newFolder("from")
        val store = ScreenWallpaperStore(from)
        store.pictureDir.mkdirs()
        File(store.pictureDir, "s.jpg").writeBytes(byteArrayOf(1, 2))
        File(store.pictureDir, "w.jpg").writeBytes(byteArrayOf(3))
        store.save(listOf(ScreenPicture(2, "s.jpg", "w.jpg", PicturePlace(1.5f, 0.3f, 0.6f))))
        val c = BackupStore(from).collect(base, emptyMap())
        val to = tmp.newFolder("to")
        assertEquals(RestoreResult.Done, BackupStore(to).restore(c, writePrefs = {}, currentPrefs = emptyMap()))
        for (f in listOf("screens.json", "screens/s.jpg", "screens/w.jpg")) assertArrayEquals(f, File(from, f).readBytes(), File(to, f).readBytes())
        assertEquals(store.load(), ScreenWallpaperStore(to).load())
    }

    @Test fun unusedPicturesAreClearedAway() {
        val d = tmp.newFolder("d")
        val store = ScreenWallpaperStore(d)
        store.pictureDir.mkdirs()
        for (n in listOf("s.jpg", "w.jpg", "old.jpg")) File(store.pictureDir, n).writeBytes(byteArrayOf(1))
        val list = listOf(ScreenPicture(3, "s.jpg", "w.jpg", PicturePlace()))
        store.save(list)
        assertEquals(1, store.removeUnusedIfSafe(store.load()))
        assertEquals(setOf("s.jpg", "w.jpg"), store.pictureDir.list()!!.toSet())
    }

    @Test fun anUnreadableListKeepsEveryPicture() {
        val d = tmp.newFolder("d")
        File(d, "screens.json").writeText("{ broken")
        val store = ScreenWallpaperStore(d)
        store.pictureDir.mkdirs()
        File(store.pictureDir, "s.jpg").writeBytes(byteArrayOf(1))
        assertEquals(emptyList<ScreenPicture>(), store.load())
        assertEquals(0, store.removeUnusedIfSafe(emptyList()))
        assertTrue(File(store.pictureDir, "s.jpg").exists())
    }

    @Test fun noListMeansNoPictures() {
        assertEquals(emptyList<ScreenPicture>(), ScreenWallpaperStore(tmp.newFolder("d")).load())
    }

    @Test fun theSwitchIsOffUntilTurnedOn() {
        assertFalse(Settings().screenWallpapers)
    }
}
