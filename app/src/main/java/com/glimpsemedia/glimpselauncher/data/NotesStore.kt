package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Notes
import com.glimpsemedia.glimpselauncher.model.NotesJson
import java.io.File
import java.util.UUID

/**
 * The notes on the phone only: `notes.json` in [dir] (written to a temporary file, then renamed) and the pictures,
 * copied into [dir]/notes so they survive when the originals are deleted.
 */
class NotesStore(private val dir: File) {
    private val file = File(dir, "notes.json")

    val pictureDir: File get() = File(dir, "notes").also { it.mkdirs() }

    /** The saved notes; none if there is no file. A file that can't be read is kept as notes.json.bad, never wiped. */
    fun load(): Notes {
        if (!file.exists()) return Notes()
        val read = try { NotesJson.decode(file.readText()) } catch (_: Exception) { null }
        if (read != null) return read
        setAside = true
        val bad = File(dir, "notes.json.bad")
        bad.delete()
        if (!file.renameTo(bad)) file.copyTo(bad, overwrite = true)
        return Notes()
    }

    fun save(notes: Notes) {
        val temp = File(dir, "notes.json.tmp")
        temp.writeText(NotesJson.encode(notes))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    /** A new, not yet created, picture file. */
    fun newPictureFile(): File = File(pictureDir, UUID.randomUUID().toString() + ".jpg")

    fun deletePictures(names: Collection<String>) {
        names.filter { NotesJson.isPlainFileName(it) }.forEach { File(pictureDir, it).delete() }
    }

    /** The last load found notes.json unreadable and kept it as notes.json.bad: its pictures must stay. */
    var setAside = false
        private set

    /** Orphan cleanup, except after a load that set the file aside (every picture would look like an orphan). */
    fun removeOrphansIfSafe(notes: Notes): Int = if (setAside) 0 else removeOrphans(notes)

    /** Deletes picture files no note mentions (a cancelled photo, a crash before saving); returns how many. */
    fun removeOrphans(notes: Notes): Int {
        val names = pictureDir.list()?.toList().orEmpty()
        val orphans = orphanPictures(names, notes)
        deletePictures(orphans)
        return orphans.size
    }
}

/** The file names no note mentions. */
fun orphanPictures(files: Collection<String>, notes: Notes): Set<String> =
    files.toSet() - notes.list.flatMap { it.pictures }.toSet()
