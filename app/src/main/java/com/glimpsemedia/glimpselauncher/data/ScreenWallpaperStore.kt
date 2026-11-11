package com.glimpsemedia.glimpselauncher.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.glimpsemedia.glimpselauncher.model.NotesJson
import com.glimpsemedia.glimpselauncher.model.PicturePlace
import com.glimpsemedia.glimpselauncher.model.ScreenPicture
import com.glimpsemedia.glimpselauncher.model.ScreenPicturesJson
import com.glimpsemedia.glimpselauncher.model.cropRect
import com.glimpsemedia.glimpselauncher.model.exifTurn
import com.glimpsemedia.glimpselauncher.model.screenPictureFiles
import java.io.File
import java.util.UUID

/** Reading a chosen picture the right way up, and cutting the part that fills the screen. */
object PictureFiles {
    /** The picture at [uri], turned upright (photos taken sideways say so in their EXIF data), no side longer than [maxSide]. */
    fun decodeUpright(resolver: ContentResolver, uri: Uri, maxSide: Int): Bitmap? = safely {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@safely null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: return@safely null
        val orientation = try {
            resolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } ?: 1
        } catch (_: Exception) { 1 }
        val (turn, mirror) = exifTurn(orientation)
        val long = maxOf(raw.width, raw.height)
        val shrink = if (long > maxSide) maxSide.toFloat() / long else 1f
        if (turn == 0 && !mirror && shrink == 1f) return@safely raw
        val m = Matrix().apply { setScale(shrink, shrink); postRotate(turn.toFloat()); if (mirror) postScale(-1f, 1f) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it !== raw) raw.recycle() }
    }

    /** The part of [source] that fills a [sw]×[sh] screen at [place], at exactly that size. */
    fun cut(source: Bitmap, sw: Int, sh: Int, place: PicturePlace): Bitmap {
        val (l, t, w, h) = cropRect(source.width, source.height, sw, sh, place).let { listOf(it[0], it[1], it[2], it[3]) }
        val part = Bitmap.createBitmap(source, l, t, w, h)
        return Bitmap.createScaledBitmap(part, sw, sh, true).also { if (it !== part && part !== source) part.recycle() }
    }

    fun writeJpeg(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }
}

/**
 * Screen 2, 3, …'s own wallpapers: `screens.json` (which screen has which picture, and where it sits) and the
 * pictures in `screens/` (the upright original and the screen-sized part shown). All in the app's files folder [dir].
 */
class ScreenWallpaperStore(private val dir: File) {
    val pictureDir: File get() = File(dir, "screens")
    private val listFile get() = File(dir, "screens.json")

    /** The last load found screens.json unreadable: no picture may be cleared away. */
    @Volatile var setAside = false
        private set

    fun load(): List<ScreenPicture> {
        val f = listFile
        if (!f.isFile) return emptyList()
        val list = try { ScreenPicturesJson.decode(f.readText()) } catch (_: Exception) { null }
        if (list == null) { setAside = true; return emptyList() }
        return list
    }

    fun save(list: List<ScreenPicture>) {
        val tmp = File(dir, "screens.json.tmp")
        tmp.writeText(ScreenPicturesJson.encode(list))
        if (!tmp.renameTo(listFile)) { listFile.writeText(tmp.readText()); tmp.delete() }
        setAside = false
    }

    /** Keeps the chosen picture, upright, as a new original; its file name, or null if it could not be read. */
    fun importSource(resolver: ContentResolver, uri: Uri, maxSide: Int): String? {
        val bitmap = PictureFiles.decodeUpright(resolver, uri, maxSide) ?: return null
        return try {
            val name = "src-" + UUID.randomUUID() + ".jpg"
            PictureFiles.writeJpeg(bitmap, File(pictureDir, name))
            name
        } catch (_: Exception) { null } finally { bitmap.recycle() }
    }

    fun loadSource(name: String): Bitmap? = safely { File(pictureDir, name).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) } }

    /** Cuts the part shown from original [source] at [place] for a [sw]×[sh] screen; the new file's name, or null. */
    fun render(source: String, sw: Int, sh: Int, place: PicturePlace): String? {
        val src = loadSource(source) ?: return null
        return try {
            val shown = PictureFiles.cut(src, sw, sh, place)
            val name = "pic-" + UUID.randomUUID() + ".jpg"
            PictureFiles.writeJpeg(shown, File(pictureDir, name))
            if (shown !== src) shown.recycle()
            name
        } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null } finally { src.recycle() }
    }

    /** A shown picture for drawing (16-bit colour: a photo looks the same at half the memory). */
    fun loadShown(name: String): Bitmap? = safely {
        File(pictureDir, name).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }) }
    }

    /** Deletes picture files [list] does not use, unless the last load set the list aside; returns how many. */
    fun removeUnusedIfSafe(list: List<ScreenPicture>): Int {
        if (setAside) return 0
        val unused = (pictureDir.list()?.toSet() ?: emptySet()) - screenPictureFiles(list)
        unused.filter { NotesJson.isPlainFileName(it) }.forEach { File(pictureDir, it).delete() }
        return unused.size
    }
}
