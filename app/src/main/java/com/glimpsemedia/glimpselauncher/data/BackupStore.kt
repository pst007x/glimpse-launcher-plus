package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.NotesJson
import java.io.File

/** What a restore came to: done, refused before anything changed, or failed and put back. */
enum class RestoreResult { Done, NoRoom, Failed }

/**
 * The phone side of a backup, on the app's files folder [dir]: lists the four files and the note and wallpaper
 * pictures (left on disk), and puts a backup back.
 *
 * A restore first copies the current files and settings to before-restore/ and leaves an in-progress mark there.
 * If any step fails they are put back; if the launcher is stopped part-way (power off, the phone closing it),
 * [recoverIfInterrupted] puts them back on the next start. A successful restore removes the safety copy.
 */
class BackupStore(private val dir: File) {
    private val files = listOf("layout.json", "textmode.json", "notes.json", "themes.json")
    private val folders = listOf("notes", "wallpapers", "icons/picked")
    private val safe get() = File(dir, "before-restore")
    private val mark get() = File(safe, "in-progress")

    fun collect(manifestBase: BackupManifest, prefs: Map<String, PrefValue>): BackupContents {
        val out = LinkedHashMap<String, File>()
        for (name in files) File(dir, name).takeIf { it.isFile }?.let { out[name] = it }
        for (folder in folders) {
            File(dir, folder).listFiles()?.sortedBy { it.name }?.forEach { f ->
                if (f.isFile && NotesJson.isPlainFileName(f.name)) out["$folder/${f.name}"] = f
            }
        }
        val pin = (prefs["pinHash"] as? PrefValue.S)?.v?.isNotEmpty() == true
        // A smaller grid needs this app to read it: older apps would lose the screens, so they are told it is too new.
        val tight = out["layout.json"]?.let { f -> runCatching { com.glimpsemedia.glimpselauncher.model.LayoutJson.decode(f.readText()) }.getOrNull() }
            ?.let { (it.columns to it.rows) in com.glimpsemedia.glimpselauncher.model.TIGHT_GRIDS } == true
        val manifest = manifestBase.copy(
            format = if (tight) Backup.FORMAT else Backup.FORMAT_USUAL,
            notes = Backup.countIn(out["notes.json"], "notes"), themes = Backup.countIn(out["themes.json"], "themes"), pin = pin,
        )
        return BackupContents(manifest, Backup.settingsForBackup(prefs), out)
    }

    fun restore(
        c: BackupContents,
        writePrefs: (Map<String, PrefValue>) -> Unit,
        currentPrefs: Map<String, PrefValue>,
        copy: (from: File, to: File) -> Unit = { from, to -> from.copyTo(to, overwrite = true) },
    ): RestoreResult {
        // 1. The safety copy (files, settings, then the mark). If even this cannot be made, nothing has changed.
        try {
            safe.deleteRecursively()
            if (!safe.mkdirs()) throw java.io.IOException("no safety folder")
            copyOurs(dir, safe)
            File(safe, "settings.json").writeText(Backup.encodeSettings(currentPrefs))
            mark.writeText("restoring")
        } catch (_: Exception) {
            runCatching { safe.deleteRecursively() }
            return RestoreResult.NoRoom
        }
        // 2. Replace everything; on any failure put the safety copy back.
        return try {
            clearOurs(dir)
            for ((name, from) in c.files) {
                if (!Backup.isAllowedName(name)) continue
                val to = File(dir, name)
                to.parentFile?.mkdirs()
                copy(from, to)
            }
            writePrefs(c.settings)
            mark.delete()
            safe.deleteRecursively()
            RestoreResult.Done
        } catch (_: Exception) {
            putBack(writePrefs)   // if even this fails, the mark stays and the next start tries again
            RestoreResult.Failed
        }
    }

    /** On start: a restore that was stopped part-way is undone. True if anything was put back. */
    fun recoverIfInterrupted(writePrefs: (Map<String, PrefValue>) -> Unit): Boolean {
        if (!mark.isFile) {
            // A safety copy without the mark is left over from a finished restore: it is not needed.
            if (safe.exists()) runCatching { safe.deleteRecursively() }
            return false
        }
        return putBack(writePrefs)
    }

    /** Puts the safety copy back (files and settings) and removes it; false if that failed (the mark then stays). */
    private fun putBack(writePrefs: (Map<String, PrefValue>) -> Unit): Boolean = try {
        clearOurs(dir)
        copyOurs(safe, dir)
        Backup.decodeSettings(File(safe, "settings.json").readText())?.let(writePrefs)
        mark.delete()
        safe.deleteRecursively()
        true
    } catch (_: Exception) {
        false
    }

    private fun clearOurs(d: File) {
        for (name in files) File(d, name).delete()
        for (folder in folders) File(d, folder).listFiles()?.forEach { it.delete() }
    }

    private fun copyOurs(from: File, to: File) {
        for (name in files) File(from, name).takeIf { it.isFile }?.copyTo(File(to, name), overwrite = true)
        for (folder in folders) {
            File(from, folder).listFiles()?.filter { it.isFile }?.forEach { it.copyTo(File(to, "$folder/${it.name}").also { t -> t.parentFile?.mkdirs() }, overwrite = true) }
        }
    }
}
