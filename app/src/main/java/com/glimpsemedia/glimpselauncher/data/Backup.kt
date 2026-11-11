package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Item
import com.glimpsemedia.glimpselauncher.model.LayoutJson
import com.glimpsemedia.glimpselauncher.model.NotesJson
import com.glimpsemedia.glimpselauncher.model.TextModeJson
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** The Glimpse Launcher builds, Play and Plus with their test copies; a backup from any restores into any. */
const val RELEASE_APP = "com.glimpsemedia.glimpselauncher"
const val PREVIEW_APP = "com.glimpsemedia.glimpselauncher.preview"
const val PLUS_APP = "com.glimpsemedia.glimpselauncherplus"
const val PLUS_PREVIEW_APP = "com.glimpsemedia.glimpselauncherplus.preview"
private val OUR_APPS = setOf(RELEASE_APP, PREVIEW_APP, PLUS_APP, PLUS_PREVIEW_APP)

/** One saved setting, with its type kept so it reads back exactly. */
sealed interface PrefValue {
    data class B(val v: Boolean) : PrefValue
    data class I(val v: Int) : PrefValue
    data class L(val v: Long) : PrefValue
    data class F(val v: Float) : PrefValue
    data class S(val v: String) : PrefValue
}

/** What a backup says about itself: which app, which format, which version, when, and what it holds. */
data class BackupManifest(val app: String, val format: Int, val version: String, val created: Long, val notes: Int, val themes: Int, val pin: Boolean)

/**
 * A whole backup: the manifest, the settings, and the app's files by their entry names ("layout.json",
 * "notes/x.jpg"...). The files stay on disk (the app's own when backing up, a staging folder when restoring), so a
 * backup with many large pictures never has to fit in memory.
 */
class BackupContents(val manifest: BackupManifest, val settings: Map<String, PrefValue>, val files: Map<String, File>)

sealed interface BackupRead {
    data class Ok(val contents: BackupContents) : BackupRead
    data object NotABackup : BackupRead
    data object TooNew : BackupRead
}

/** A backup that would be bigger than [Backup] reads back: refused when saving, so every saved backup can be restored. */
class BackupTooBig : Exception("The backup is too big")

/**
 * The backup file: a zip with fixed entry names. Writing and reading are kept apart from Android so they can be
 * tested; reading never trusts the file (only known names, plain picture names, size limits, counts that match).
 */
object Backup {
    /** The newest backup format this app reads. 2: the layout may use a smaller grid (4×5, 5×5, 5×6). 3: big items. */
    const val FORMAT = 3
    /** The format older apps read; a backup is written in it unless its layout needs format 2. */
    const val FORMAT_USUAL = 1
    const val MAX_ENTRY = 50L shl 20
    const val MAX_TOTAL = 500L shl 20
    private const val MAX_SMALL = 1L shl 20       // manifest.json and settings.json
    private val FILES = setOf("layout.json", "textmode.json", "notes.json", "themes.json", "screens.json")
    // icons/picked/: pictures chosen as icons and saved copies of per-app pack icons (icons/pack/ is made again).
    // screens/: screen 2, 3, …'s own wallpapers.
    private val FOLDERS = listOf("notes/", "wallpapers/", "icons/picked/", "screens/")
    // Folder locations are Android grants for this phone and this install: they cannot be restored.
    private val LEFT_OUT = setOf("welcomePage", "lastBackupMillis", "searchFolderList")

    /** Only these names are ever written or read: the four files, and plain file names inside notes/ and wallpapers/. */
    /**
     * The format a backup of [layout] is written in: the oldest that can read it, so older apps refuse only what they
     * would show wrongly (3 with a big item, 2 with a smaller grid, else 1).
     */
    fun formatFor(layout: com.glimpsemedia.glimpselauncher.model.Layout?): Int = when {
        layout == null -> FORMAT_USUAL
        layout.bigItems.isNotEmpty() -> 3
        (layout.columns to layout.rows) in com.glimpsemedia.glimpselauncher.model.TIGHT_GRIDS -> 2
        else -> FORMAT_USUAL
    }

    fun isAllowedName(name: String): Boolean =
        name in FILES || FOLDERS.any { f -> name.startsWith(f) && NotesJson.isPlainFileName(name.removePrefix(f)) }

    /** Kept from this phone when a backup is restored: when the last backup was made, and Search's folders (their grants stay). */
    private val KEPT_ON_RESTORE = setOf("lastBackupMillis", "searchFolderList")

    /** The settings after restoring [backup] over [current]: the backup's, plus this phone's [KEPT_ON_RESTORE]. */
    fun restoredPrefs(backup: Map<String, PrefValue>, current: Map<String, PrefValue>): Map<String, PrefValue> =
        backup - KEPT_ON_RESTORE + current.filterKeys { it in KEPT_ON_RESTORE }

    /** The settings to back up: all of them, except where the welcome was, when the last backup was made, and Search's folders. */
    fun settingsForBackup(all: Map<String, PrefValue>): Map<String, PrefValue> = all.filterKeys { it !in LEFT_OUT }

    /**
     * The backed-up settings this phone can take: a setting this app already stores with another type is left out
     * (a crafted or future file could otherwise stop the launcher from starting).
     */
    fun matchTypes(backup: Map<String, PrefValue>, current: Map<String, PrefValue>): Map<String, PrefValue> =
        backup.filter { (k, v) -> current[k]?.let { it::class == v::class } ?: true }

    fun backupFileName(millis: Long, zone: ZoneId): String =
        "glimpse-backup-${Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()}.zip"

    /** Writes the zip, copying each file straight from disk. Throws [BackupTooBig] past the limits [read] uses. */
    fun write(out: OutputStream, contents: BackupContents, maxEntry: Long = MAX_ENTRY, maxTotal: Long = MAX_TOTAL) {
        val files = contents.files.filterKeys { isAllowedName(it) }
        var total = 0L
        for (f in files.values) {
            val n = f.length()
            total += n
            if (n > maxEntry || total > maxTotal) throw BackupTooBig()
        }
        ZipOutputStream(out).use { z ->
            fun put(name: String, bytes: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() }
            put("manifest.json", encodeManifest(contents.manifest).toByteArray())
            put("settings.json", encodeSettings(contents.settings).toByteArray())
            files.forEach { (name, file) ->
                z.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }

    /**
     * Reads a backup, saving its files into [staging] (emptied first) without holding them in memory. Anything that
     * is not a whole, genuine Glimpse Launcher backup gives [BackupRead.NotABackup], and [staging] is emptied again.
     */
    fun read(input: InputStream, staging: File, maxEntry: Long = MAX_ENTRY, maxTotal: Long = MAX_TOTAL): BackupRead {
        staging.deleteRecursively()
        val result = try {
            readOrThrow(input, staging, maxEntry, maxTotal)
        } catch (_: Exception) {
            BackupRead.NotABackup
        } catch (_: OutOfMemoryError) {
            BackupRead.NotABackup
        }
        if (result !is BackupRead.Ok) staging.deleteRecursively()
        return result
    }

    private fun readOrThrow(input: InputStream, staging: File, maxEntry: Long, maxTotal: Long): BackupRead {
        var manifestText: String? = null
        var settingsText: String? = null
        val files = LinkedHashMap<String, File>()
        var total = 0L
        ZipInputStream(input).use { z ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name
                val small = name == "manifest.json" || name == "settings.json"
                val keep = !e.isDirectory && (small || isAllowedName(name))
                val target = if (keep && !small) File(staging, name).also { it.parentFile?.mkdirs() } else null
                val memory = if (small) ByteArrayOutputStream() else null
                val out = target?.outputStream()
                try {
                    var size = 0L
                    while (true) {
                        val n = z.read(buf)
                        if (n < 0) break
                        size += n; total += n
                        if (size > (if (small) MAX_SMALL else maxEntry) || total > maxTotal) return BackupRead.NotABackup
                        memory?.write(buf, 0, n)
                        out?.write(buf, 0, n)
                    }
                } finally {
                    out?.close()
                }
                when (name) {
                    "manifest.json" -> manifestText = memory!!.toString()
                    "settings.json" -> settingsText = memory!!.toString()
                    else -> if (target != null) files[name] = target   // a repeated name: the last one wins
                }
            }
        }
        val manifest = manifestText?.let { decodeManifest(it) } ?: return BackupRead.NotABackup
        if (manifest.app !in OUR_APPS) return BackupRead.NotABackup
        if (manifest.format > FORMAT) return BackupRead.TooNew
        // A genuine backup always has its settings; without them a restore would wipe every setting.
        val settings = settingsText?.let { decodeSettings(it) } ?: return BackupRead.NotABackup
        // The counts must match what is really there (a cut-short file reads as a shorter one otherwise).
        if (countIn(files["notes.json"], "notes") != manifest.notes || countIn(files["themes.json"], "themes") != manifest.themes) return BackupRead.NotABackup
        return BackupRead.Ok(BackupContents(manifest, settings, files))
    }

    /** How many items [file]'s JSON [array] holds (0 if there is no file). */
    fun countIn(file: File?, array: String): Int =
        if (file == null || !file.isFile) 0 else runCatching { JSONObject(file.readText()).optJSONArray(array)?.length() ?: 0 }.getOrDefault(0)

    /**
     * Makes a read backup right for this phone, rewriting its staged files: widgets go (a widget's number belongs to
     * one install; another phone could show a different widget for it), and the saved themes forget which wallpaper
     * Glimpse last set (that was on the old phone).
     */
    fun forThisPhone(contents: BackupContents) {
        contents.files["layout.json"]?.let { f ->
            LayoutJson.decode(f.readText())?.let { l ->
                val pages = l.pages.map { page ->
                    page.items.withIndex().filter { it.value is Item.Widget }.map { it.index }.fold(page) { p, at -> p.withoutWidget(at) }
                }
                f.writeText(LayoutJson.encode(l.copy(pages = pages)))
            }
        }
        contents.files["textmode.json"]?.let { f ->
            TextModeJson.decode(f.readText())?.let { t -> f.writeText(TextModeJson.encode(t.copy(widgets = emptyList()))) }
        }
        contents.files["themes.json"]?.let { f ->
            runCatching {
                val o = JSONObject(f.readText())
                o.remove("lastHome"); o.remove("lastLock")
                f.writeText(o.toString())
            }
        }
    }

    private fun encodeManifest(m: BackupManifest) = JSONObject().put("app", m.app).put("format", m.format).put("version", m.version)
        .put("created", m.created).put("notes", m.notes).put("themes", m.themes).put("pin", m.pin).toString()

    private fun decodeManifest(text: String): BackupManifest? = try {
        val o = JSONObject(text)
        BackupManifest(o.getString("app"), o.getInt("format"), o.optString("version"), o.optLong("created"), o.optInt("notes"), o.optInt("themes"), o.optBoolean("pin"))
    } catch (_: Exception) {
        null
    }

    fun encodeSettings(s: Map<String, PrefValue>): String {
        val o = JSONObject()
        s.forEach { (k, v) ->
            o.put(k, when (v) {
                is PrefValue.B -> JSONObject().put("t", "b").put("v", v.v)
                is PrefValue.I -> JSONObject().put("t", "i").put("v", v.v)
                is PrefValue.L -> JSONObject().put("t", "l").put("v", v.v)
                is PrefValue.F -> JSONObject().put("t", "f").put("v", v.v.toDouble())
                is PrefValue.S -> JSONObject().put("t", "s").put("v", v.v)
            })
        }
        return o.toString()
    }

    /** The settings from their saved form; a broken file gives null. */
    fun decodeSettings(text: String): Map<String, PrefValue>? = try {
        val o = JSONObject(text)
        o.keys().asSequence().mapNotNull { k ->
            val e = o.optJSONObject(k) ?: return@mapNotNull null
            val v: PrefValue? = when (e.optString("t")) {
                "b" -> PrefValue.B(e.getBoolean("v"))
                "i" -> PrefValue.I(e.getInt("v"))
                "l" -> PrefValue.L(e.getLong("v"))
                "f" -> PrefValue.F(e.getDouble("v").toFloat())
                "s" -> PrefValue.S(e.getString("v"))
                else -> null
            }
            v?.let { k to it }
        }.toMap()
    } catch (_: Exception) {
        null
    }
}
