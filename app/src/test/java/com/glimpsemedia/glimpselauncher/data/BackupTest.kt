package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Item
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.model.LayoutJson
import com.glimpsemedia.glimpselauncher.model.Page
import com.glimpsemedia.glimpselauncher.model.TextLayout
import com.glimpsemedia.glimpselauncher.model.TextModeJson
import com.glimpsemedia.glimpselauncher.model.TextWidget
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupTest {
    @get:Rule val tmp = TemporaryFolder()

    private val settings = mapOf(
        "darkTheme" to PrefValue.B(true), "textSize5" to PrefValue.I(3), "lastBig" to PrefValue.L(1L shl 40),
        "ratio" to PrefValue.F(0.5f), "pinHash" to PrefValue.S("abc=="),
    )
    private val notesJson = """{"v":1,"notes":[{"id":"n1","t":"A","lines":[],"p":["p1.jpg"]},{"id":"n2","t":"B","lines":[],"p":[]}]}"""

    /** A phone folder with the four kinds of things a backup holds. */
    private fun source(): Map<String, File> {
        val d = tmp.newFolder()
        fun f(name: String, bytes: ByteArray) = File(d, name).also { it.parentFile?.mkdirs(); it.writeBytes(bytes) }
        return linkedMapOf(
            "layout.json" to f("layout.json", "{}".toByteArray()),
            "notes.json" to f("notes.json", notesJson.toByteArray()),
            "notes/p1.jpg" to f("notes/p1.jpg", byteArrayOf(1, 2, 3)),
            "wallpapers/w1.img" to f("wallpapers/w1.img", byteArrayOf(9, 8)),
        )
    }
    private val manifest = BackupManifest(RELEASE_APP, Backup.FORMAT, "1.0.3", 1_794_168_000_000L, notes = 2, themes = 0, pin = true)

    private fun bytes(c: BackupContents) = ByteArrayOutputStream().also { Backup.write(it, c) }.toByteArray()
    private fun staging() = File(tmp.root, "staging")
    private fun zip(vararg entries: Pair<String, ByteArray>) = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
    }.toByteArray()
    private fun manifestJson(app: String = RELEASE_APP, format: Int = 1, notes: Int = 0) =
        """{"app":"$app","format":$format,"version":"1","created":1,"notes":$notes,"themes":0,"pin":false}""".toByteArray()
    private val emptySettings = "{}".toByteArray()

    @Test fun writeThenReadGivesEverythingBack() {
        val src = source()
        val r = Backup.read(ByteArrayInputStream(bytes(BackupContents(manifest, settings, src))), staging()) as BackupRead.Ok
        assertEquals(manifest, r.contents.manifest)
        assertEquals(src.keys, r.contents.files.keys)
        src.forEach { (k, f) -> assertArrayEquals(k, f.readBytes(), r.contents.files.getValue(k).readBytes()) }
        assertTrue(r.contents.files.values.all { it.path.startsWith(staging().path) })   // kept on disk, not in memory
    }

    @Test fun settingsKeepTheirTypes() {
        val r = Backup.read(ByteArrayInputStream(bytes(BackupContents(manifest, settings, source()))), staging()) as BackupRead.Ok
        assertEquals(settings, r.contents.settings)
    }

    @Test fun foreignOrMissingManifestIsNotABackup() {
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(zip("layout.json" to "{}".toByteArray())), staging()))
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson(app = "com.other.app"), "settings.json" to emptySettings)), staging()))
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream("not a zip at all".toByteArray()), staging()))
        assertTrue(Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson(app = PREVIEW_APP), "settings.json" to emptySettings)), staging()) is BackupRead.Ok)
    }

    @Test fun plusBackupsRestoreAnywhere() {
        for (app in listOf(PLUS_APP, PLUS_PREVIEW_APP))
            assertTrue(app, Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson(app = app), "settings.json" to emptySettings)), staging()) is BackupRead.Ok)
    }

    @Test fun aBackupWithoutItsSettingsIsNotABackup() {
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson())), staging()))
        assertFalse(staging().exists())   // nothing left behind
    }

    @Test fun countsMustMatchWhatIsThere() {
        // The manifest promises two notes but the file has none: cut short, or not genuine.
        val z = zip("manifest.json" to manifestJson(notes = 2), "settings.json" to emptySettings)
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(z), staging()))
    }

    @Test fun newerFormatIsTooNew() {
        assertEquals(BackupRead.TooNew, Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson(format = 3), "settings.json" to emptySettings)), staging()))
        // Format 2 (a backup with a smaller grid) is read by this app.
        assertTrue(Backup.read(ByteArrayInputStream(zip("manifest.json" to manifestJson(format = 2), "settings.json" to emptySettings)), staging()) is BackupRead.Ok)
    }

    @Test fun unsafeNamesAreIgnored() {
        val z = zip(
            "manifest.json" to manifestJson(), "settings.json" to emptySettings, "notes/../x" to byteArrayOf(1), "/etc/x" to byteArrayOf(1),
            "notes/" to byteArrayOf(), "other.txt" to byteArrayOf(1), "notes/ok.jpg" to byteArrayOf(7),
        )
        val r = Backup.read(ByteArrayInputStream(z), staging())
        // Either refused outright (Android's own zip-path check) or read with only the safe entry; never written outside.
        if (r is BackupRead.Ok) assertEquals(setOf("notes/ok.jpg"), r.contents.files.keys)
        assertFalse(File(tmp.root, "x").exists()); assertFalse(File(staging(), "x").exists()); assertFalse(File(staging(), "other.txt").exists())
        assertFalse(Backup.isAllowedName("notes/../x")); assertFalse(Backup.isAllowedName("wallpapers/a/b"))
        assertTrue(Backup.isAllowedName("themes.json")); assertTrue(Backup.isAllowedName("wallpapers/w.img"))
    }

    @Test fun sizeLimitsWhenReading() {
        val z = zip("manifest.json" to manifestJson(), "settings.json" to emptySettings, "notes/big.jpg" to ByteArray(11))
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(z), staging(), maxEntry = 10, maxTotal = 1000))
        assertEquals(BackupRead.NotABackup, Backup.read(ByteArrayInputStream(z), staging(), maxEntry = 1000, maxTotal = 20))
    }

    @Test(expected = BackupTooBig::class) fun aBackupTooBigToRestoreIsNotSaved() {
        Backup.write(ByteArrayOutputStream(), BackupContents(manifest, settings, source()), maxEntry = 2, maxTotal = 1000)
    }

    @Test fun forThisPhoneRemovesWidgetsAndTheOldPhonesWallpaperRecord() {
        val d = tmp.newFolder()
        val withWidget = Page().withWidget(0, Item.Widget(41, "w/W", 2, 1))
        val layout = File(d, "layout.json").also { it.writeText(LayoutJson.encode(Layout(listOf(withWidget.set(2, Item.App("a/A"))), emptyList()))) }
        val text = File(d, "textmode.json").also { it.writeText(TextModeJson.encode(TextLayout(listOf("a/A"), widgets = listOf(TextWidget(7, "p/P", 1))))) }
        val themes = File(d, "themes.json").also { it.writeText("""{"v":1,"themes":[],"lastHome":{"id":5},"lastLock":{"id":6}}""") }
        Backup.forThisPhone(BackupContents(manifest, emptyMap(), mapOf("layout.json" to layout, "textmode.json" to text, "themes.json" to themes)))
        val l = LayoutJson.decode(layout.readText())!!
        assertTrue(l.pages[0].items.none { it is Item.Widget || it == Item.Covered })
        assertEquals(Item.App("a/A"), l.pages[0].items[2])
        assertEquals(emptyList<TextWidget>(), TextModeJson.decode(text.readText())!!.widgets)
        val t = JSONObject(themes.readText())
        assertFalse(t.has("lastHome")); assertFalse(t.has("lastLock"))
    }

    @Test fun settingsOfAnotherTypeAreLeftOut() {
        val backup = mapOf("showClock" to PrefValue.I(1), "darkTheme" to PrefValue.B(false), "newKey" to PrefValue.S("x"))
        val current = mapOf("showClock" to PrefValue.B(true), "darkTheme" to PrefValue.B(true))
        assertEquals(mapOf("darkTheme" to PrefValue.B(false), "newKey" to PrefValue.S("x")), Backup.matchTypes(backup, current))
    }

    @Test fun welcomePageAndLastBackupAreLeftOut() {
        val all = settings + ("welcomePage" to PrefValue.I(3)) + ("lastBackupMillis" to PrefValue.L(5))
        assertEquals(settings, Backup.settingsForBackup(all))
    }

    @Test fun fileNameHasTheDate() {
        assertEquals("glimpse-backup-2026-11-08.zip", Backup.backupFileName(1_794_168_000_000L, ZoneId.of("Europe/London")))
    }
}
