package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.NotesJson
import com.glimpsemedia.glimpselauncher.model.SHADE_MAX
import com.glimpsemedia.glimpselauncher.model.SHADE_MIN
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Saved themes: at most this many. */
const val SAVED_THEMES_MAX = 20

/** Everything about the look that a theme carries: menus, colours, transparencies and shades. Nothing else. */
data class ThemeLook(
    val darkTheme: Boolean,
    val tileColor: Int, val barColor: Int, val clockColor: Int, val highlightColor: Int,
    val clockBackground: Boolean, val clockBackColor: Int, val iconColor: Int,
    val tileTransparency: Int, val barTransparency: Int, val clockTransparency: Int,
    val clockBackTransparency: Int, val iconTransparency: Int,
    val tileShade: Int, val iconShade: Int, val barShade: Int, val clockShade: Int, val clockBackShade: Int, val highlightShade: Int,
    val tileTextColor: Int = 0, val tileTextShade: Int = 0, val textModeColor: Int = 0, val textModeShade: Int = 0,
)

fun Settings.look() = ThemeLook(
    darkTheme, tileColor, barColor, clockColor, highlightColor, clockBackground, clockBackColor, iconColor,
    tileTransparency, barTransparency, clockTransparency, clockBackTransparency, iconTransparency,
    tileShade, iconShade, barShade, clockShade, clockBackShade, highlightShade,
    tileTextColor, tileTextShade, textModeColor, textModeShade,
)

/** These settings with [l]'s look; the clock style, names, grid, locking and the rest are kept. */
fun Settings.withLook(l: ThemeLook) = copy(
    darkTheme = l.darkTheme, tileColor = l.tileColor, barColor = l.barColor, clockColor = l.clockColor,
    highlightColor = l.highlightColor, clockBackground = l.clockBackground, clockBackColor = l.clockBackColor,
    iconColor = l.iconColor, tileTransparency = l.tileTransparency, barTransparency = l.barTransparency,
    clockTransparency = l.clockTransparency, clockBackTransparency = l.clockBackTransparency,
    iconTransparency = l.iconTransparency, tileShade = l.tileShade, iconShade = l.iconShade, barShade = l.barShade,
    clockShade = l.clockShade, clockBackShade = l.clockBackShade, highlightShade = l.highlightShade,
    tileTextColor = l.tileTextColor, tileTextShade = l.tileTextShade, textModeColor = l.textModeColor, textModeShade = l.textModeShade,
)

/** A wallpaper Glimpse Launcher can put back: one it drew from four colours, or its own copy of a picture. */
sealed interface SavedWallpaper {
    data class Drawn(val c1: Int, val c2: Int, val c3: Int, val c4: Int, val dark: Boolean) : SavedWallpaper
    /** [file] is a plain file name in the launcher's own `files/wallpapers/`. */
    data class Picture(val file: String) : SavedWallpaper
}

/** The wallpaper Glimpse last set on a screen, with the id the phone gave it (to tell if it was changed elsewhere since). */
data class GlimpseWallpaper(val wallpaper: SavedWallpaper, val id: Int)

/** A theme the person saved: the look, and the home and lock screen wallpapers if Glimpse had set them. */
data class SavedTheme(val id: String, val name: String, val look: ThemeLook, val home: SavedWallpaper?, val lock: SavedWallpaper?) {
    /** The four dots on its card: the bar, the tiles, the highlight, the icon square. */
    fun cardColours(): List<Int> = Settings().withLook(look).currentThemeColours()
}

/** The saved themes, and the wallpapers Glimpse last set on each screen. */
data class SavedThemes(
    val list: List<SavedTheme> = emptyList(),
    val lastHome: GlimpseWallpaper? = null,
    val lastLock: GlimpseWallpaper? = null,
) {
    /** The wallpapers still showing that Glimpse set: a screen whose wallpaper id has changed was set elsewhere. */
    fun wallpapersNow(homeId: Int, lockId: Int): Pair<SavedWallpaper?, SavedWallpaper?> =
        lastHome?.takeIf { it.id == homeId }?.wallpaper to lastLock?.takeIf { it.id == lockId }?.wallpaper

    /** Every picture file in use: by a saved theme or as a last wallpaper. */
    fun pictureFiles(): Set<String> = buildSet {
        fun add(w: SavedWallpaper?) { if (w is SavedWallpaper.Picture) add(w.file) }
        list.forEach { add(it.home); add(it.lock) }
        add(lastHome?.wallpaper); add(lastLock?.wallpaper)
    }
}

/** "My theme N" with the lowest N not taken. */
fun nextThemeName(taken: List<String>): String = generateSequence(1) { it + 1 }.map { "My theme $it" }.first { it !in taken }

/** A theme name as typed: trimmed, at most 30 characters; null if empty. */
fun cleanThemeName(name: String): String? = name.trim().take(30).ifEmpty { null }

/** Saved themes as text for `themes.json`, and back; anything unreadable or from a newer version gives null. */
object SavedThemesJson {
    private const val VERSION = 1

    private fun encodeLook(l: ThemeLook) = JSONObject()
        .put("dark", l.darkTheme).put("tile", l.tileColor).put("bar", l.barColor).put("clock", l.clockColor)
        .put("highlight", l.highlightColor).put("clockBg", l.clockBackground).put("clockBack", l.clockBackColor)
        .put("icon", l.iconColor).put("tileT", l.tileTransparency).put("barT", l.barTransparency)
        .put("clockT", l.clockTransparency).put("clockBackT", l.clockBackTransparency).put("iconT", l.iconTransparency)
        .put("tileS", l.tileShade).put("iconS", l.iconShade).put("barS", l.barShade).put("clockS", l.clockShade)
        .put("clockBackS", l.clockBackShade).put("highlightS", l.highlightShade)
        .put("tileText", l.tileTextColor).put("tileTextS", l.tileTextShade).put("tmText", l.textModeColor).put("tmTextS", l.textModeShade)

    private fun decodeLook(o: JSONObject): ThemeLook {
        val d = Settings()
        fun pct(k: String, def: Int) = o.optInt(k, def).coerceIn(0, 100)
        fun shade(k: String) = o.optInt(k, 0).coerceIn(SHADE_MIN, SHADE_MAX)
        return ThemeLook(
            o.optBoolean("dark", d.darkTheme), o.optInt("tile", 0), o.optInt("bar", 0), o.optInt("clock", 0),
            o.optInt("highlight", 0), o.optBoolean("clockBg", d.clockBackground), o.optInt("clockBack", 0), o.optInt("icon", 0),
            pct("tileT", d.tileTransparency), pct("barT", d.barTransparency), pct("clockT", d.clockTransparency),
            pct("clockBackT", d.clockBackTransparency), pct("iconT", d.iconTransparency),
            shade("tileS"), shade("iconS"), shade("barS"), shade("clockS"), shade("clockBackS"), shade("highlightS"),
            o.optInt("tileText", 0), shade("tileTextS"), o.optInt("tmText", 0), shade("tmTextS"),
        )
    }

    private fun encodeWallpaper(w: SavedWallpaper?): Any = when (w) {
        null -> JSONObject.NULL
        is SavedWallpaper.Drawn -> JSONObject().put("k", "drawn").put("c", JSONArray(listOf(w.c1, w.c2, w.c3, w.c4))).put("dark", w.dark)
        is SavedWallpaper.Picture -> JSONObject().put("k", "pic").put("f", w.file)
    }

    private fun decodeWallpaper(o: JSONObject?): SavedWallpaper? = when (o?.optString("k")) {
        "drawn" -> o.optJSONArray("c")?.takeIf { it.length() == 4 }?.let { c ->
            SavedWallpaper.Drawn(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), o.optBoolean("dark"))
        }
        // Only a plain file name: a picture can never point outside the wallpapers folder.
        "pic" -> o.optString("f").takeIf { NotesJson.isPlainFileName(it) }?.let { SavedWallpaper.Picture(it) }
        else -> null
    }

    private fun encodeLast(g: GlimpseWallpaper?): Any =
        if (g == null) JSONObject.NULL else JSONObject().put("w", encodeWallpaper(g.wallpaper)).put("id", g.id)

    private fun decodeLast(o: JSONObject?): GlimpseWallpaper? =
        o?.let { decodeWallpaper(it.optJSONObject("w"))?.let { w -> GlimpseWallpaper(w, it.optInt("id")) } }

    fun encode(t: SavedThemes): String {
        val list = JSONArray()
        t.list.forEach {
            list.put(JSONObject().put("id", it.id).put("n", it.name).put("look", encodeLook(it.look))
                .put("home", encodeWallpaper(it.home)).put("lock", encodeWallpaper(it.lock)))
        }
        return JSONObject().put("v", VERSION).put("themes", list)
            .put("lastHome", encodeLast(t.lastHome)).put("lastLock", encodeLast(t.lastLock)).toString()
    }

    fun decode(text: String): SavedThemes? = try {
        val root = JSONObject(text)
        if (root.getInt("v") != VERSION) null else {
            val arr = root.optJSONArray("themes") ?: JSONArray()
            val seen = HashSet<String>()
            SavedThemes(
                list = (0 until arr.length()).mapNotNull { i ->
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotEmpty() && seen.add(it) } ?: return@mapNotNull null
                    SavedTheme(
                        id, cleanThemeName(o.optString("n")) ?: "My theme", decodeLook(o.optJSONObject("look") ?: JSONObject()),
                        decodeWallpaper(o.optJSONObject("home")), decodeWallpaper(o.optJSONObject("lock")),
                    )
                }.take(SAVED_THEMES_MAX),
                lastHome = decodeLast(root.optJSONObject("lastHome")),
                lastLock = decodeLast(root.optJSONObject("lastLock")),
            )
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Saved themes on the phone only: `themes.json` in [dir] (temporary file, then rename) and the launcher's own copies
 * of wallpaper pictures in [dir]/wallpapers.
 */
class ThemeStore(private val dir: File) {
    private val file = File(dir, "themes.json")

    val wallpaperDir: File get() = File(dir, "wallpapers").also { it.mkdirs() }

    /** The last load found themes.json unreadable and kept it as themes.json.bad: no picture may be cleared away. */
    var setAside = false
        private set

    fun load(): SavedThemes {
        if (!file.exists()) return SavedThemes()
        val read = try { SavedThemesJson.decode(file.readText()) } catch (_: Exception) { null }
        if (read != null) return read
        setAside = true
        val bad = File(dir, "themes.json.bad")
        bad.delete()
        if (!file.renameTo(bad)) file.copyTo(bad, overwrite = true)
        return SavedThemes()
    }

    fun save(t: SavedThemes) {
        val temp = File(dir, "themes.json.tmp")
        temp.writeText(SavedThemesJson.encode(t))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    fun newWallpaperFile(): File = File(wallpaperDir, UUID.randomUUID().toString() + ".img")

    /** Deletes picture copies nothing uses any more, unless the last load set the file aside; returns how many. */
    fun removeUnusedWallpapersIfSafe(t: SavedThemes): Int {
        if (setAside) return 0
        val unused = (wallpaperDir.list()?.toList().orEmpty()).toSet() - t.pictureFiles()
        unused.filter { NotesJson.isPlainFileName(it) }.forEach { File(wallpaperDir, it).delete() }
        return unused.size
    }
}
