package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.TextLayout
import com.glimpsemedia.glimpselauncher.model.TextModeJson
import java.io.File

/** The text layout in `textmode.json` in [dir], written to a temporary file, then renamed. */
class TextModeStore(private val dir: File) {
    private val file = File(dir, "textmode.json")

    /** Null when it was never saved. A file that can't be read is kept as textmode.json.bad, and text mode starts empty. */
    fun load(): TextLayout? {
        if (!file.exists()) return null
        val read = try { TextModeJson.decode(file.readText()) } catch (_: Exception) { null }
        if (read != null) return read
        val bad = File(dir, "textmode.json.bad")
        bad.delete()
        if (!file.renameTo(bad)) file.copyTo(bad, overwrite = true)
        return TextLayout()
    }

    fun save(t: TextLayout) {
        val temp = File(dir, "textmode.json.tmp")
        temp.writeText(TextModeJson.encode(t))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }
}
