package com.glimpsemedia.glimpselauncher.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How far in a picture can be zoomed past filling the screen. */
const val MAX_PICTURE_ZOOM = 5f

/**
 * Where a picture sits on the screen: [zoom] 1 just fills it (no edges show); [x] and [y] are the middle of the part
 * shown, as fractions of the picture's width and height (0.5, 0.5 is centred).
 */
data class PicturePlace(val zoom: Float = 1f, val x: Float = 0.5f, val y: Float = 0.5f)

/**
 * Screen [screen]'s own wallpaper (2 is the second screen): [source] is the upright picture kept so it can be placed
 * again, [shown] the screen-sized part that is drawn; both are file names in the screens folder.
 */
data class ScreenPicture(val screen: Int, val source: String, val shown: String, val place: PicturePlace)

/** The picture screen [screen] (1 the home screen) shows, or null: then it shows the default wallpaper. */
fun pictureForScreen(screen: Int, on: Boolean, list: List<ScreenPicture>): ScreenPicture? =
    if (!on || screen < 2) null else list.firstOrNull { it.screen == screen }

/** The highest screen number that can have its own picture (as stored in screens.json). */
const val MAX_SCREEN_PICTURE = 99

/**
 * The next screen number "Add another screen" gives (2 first), or null at [MAX_SCREEN_PICTURE]. A screen not made yet
 * can be set up too: no screen is made for it, and its picture shows once that screen is made.
 */
fun nextScreenToAdd(list: List<ScreenPicture>): Int? =
    (2..MAX_SCREEN_PICTURE).firstOrNull { n -> list.none { it.screen == n } }

/** The pictures to load: only those of screens that exist now, so screens set up ahead cost no memory. */
fun picturesToLoad(list: List<ScreenPicture>, screenCount: Int): List<ScreenPicture> = list.filter { it.screen <= screenCount }

/** A row's name in Settings: a screen not made yet says when its picture shows. */
fun screenRowLabel(screen: Int, screenCount: Int): String =
    if (screen > screenCount) "Screen $screen (shows once this screen is made)" else "Screen $screen"

/** The rows of the screens list, in order: those with a picture, and [adding] while its picture is being chosen. */
fun screenRows(list: List<ScreenPicture>, adding: Int?): List<Int> = (list.map { it.screen } + listOfNotNull(adding)).distinct().sorted()

/** Every file the pictures use (anything else in the screens folder can go). */
fun screenPictureFiles(list: List<ScreenPicture>): Set<String> = list.flatMap { listOf(it.source, it.shown) }.toSet()

/** Screen pixels per picture pixel when the picture just fills the screen. */
private fun fillScale(pw: Int, ph: Int, sw: Int, sh: Int) = max(sw.toFloat() / pw, sh.toFloat() / ph)

private fun okSizes(pw: Int, ph: Int, sw: Int, sh: Int) = pw > 0 && ph > 0 && sw > 0 && sh > 0

/** The part shown, as a fraction of the picture: its width and height. */
private fun window(pw: Int, ph: Int, sw: Int, sh: Int, zoom: Float): Pair<Float, Float> {
    val scale = fillScale(pw, ph, sw, sh) * zoom
    return min(1f, sw / scale / pw) to min(1f, sh / scale / ph)
}

/** [place] made possible: zoom from 1 to [MAX_PICTURE_ZOOM], and the middle where the shown part stays inside the picture. */
fun clampPlace(place: PicturePlace, pw: Int, ph: Int, sw: Int, sh: Int): PicturePlace {
    val zoom = place.zoom.takeIf { !it.isNaN() }?.coerceIn(1f, MAX_PICTURE_ZOOM) ?: 1f
    if (!okSizes(pw, ph, sw, sh)) return PicturePlace(zoom)
    val (w, h) = window(pw, ph, sw, sh, zoom)
    fun mid(v: Float, size: Float) = (v.takeIf { !it.isNaN() } ?: 0.5f).coerceIn(size / 2, 1 - size / 2)
    return PicturePlace(zoom, mid(place.x, w), mid(place.y, h))
}

/** The part of a [pw]×[ph] picture that fills a [sw]×[sh] screen at [place]: left, top, width, height in picture pixels. */
fun cropRect(pw: Int, ph: Int, sw: Int, sh: Int, place: PicturePlace): IntArray {
    if (!okSizes(pw, ph, sw, sh)) return intArrayOf(0, 0, max(pw, 0), max(ph, 0))
    val p = clampPlace(place, pw, ph, sw, sh)
    val (w, h) = window(pw, ph, sw, sh, p.zoom)
    val cw = (w * pw).roundToInt().coerceIn(1, pw)
    val ch = (h * ph).roundToInt().coerceIn(1, ph)
    val l = (p.x * pw - cw / 2f).roundToInt().coerceIn(0, pw - cw)
    val t = (p.y * ph - ch / 2f).roundToInt().coerceIn(0, ph - ch)
    return intArrayOf(l, t, cw, ch)
}

/** A finger moved [dx], [dy] screen pixels: the picture moves with it. */
fun movePlace(place: PicturePlace, dx: Float, dy: Float, pw: Int, ph: Int, sw: Int, sh: Int): PicturePlace {
    if (!okSizes(pw, ph, sw, sh)) return place
    val p = clampPlace(place, pw, ph, sw, sh)
    val scale = fillScale(pw, ph, sw, sh) * p.zoom
    return clampPlace(p.copy(x = p.x - dx / scale / pw, y = p.y - dy / scale / ph), pw, ph, sw, sh)
}

/** A pinch by [factor] (above 1 zooms in), around the middle of the part shown. */
fun zoomPlace(place: PicturePlace, factor: Float, pw: Int, ph: Int, sw: Int, sh: Int): PicturePlace =
    clampPlace(place.copy(zoom = place.zoom * factor), pw, ph, sw, sh)

/**
 * Where home screen [homeIndex]'s picture is drawn while the pager shows [homeCurrent] moved on by [fraction]: in
 * screen widths from the left edge (0 exactly on screen, −1 just gone to the left).
 */
fun slideOffset(homeIndex: Int, homeCurrent: Int, fraction: Float): Float = homeIndex - homeCurrent - fraction

/** The screen pictures as text for `screens.json`, and back. Anything unreadable, or from a newer version, gives null. */
object ScreenPicturesJson {
    private const val VERSION = 1

    fun encode(list: List<ScreenPicture>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("n", it.screen).put("src", it.source).put("pic", it.shown)
                .put("z", it.place.zoom.toDouble()).put("x", it.place.x.toDouble()).put("y", it.place.y.toDouble()))
        }
        return JSONObject().put("v", VERSION).put("screens", arr).toString()
    }

    fun decode(text: String): List<ScreenPicture>? = try {
        val root = JSONObject(text)
        if (root.getInt("v") != VERSION) null else {
            val arr = root.getJSONArray("screens")
            val out = ArrayList<ScreenPicture>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val n = o.opt("n") as? Number ?: continue
                val src = o.opt("src") as? String ?: continue
                val pic = o.opt("pic") as? String ?: continue
                fun num(k: String, d: Float): Float? = when (val v = o.opt(k)) { null -> d; is Number -> v.toFloat(); else -> null }
                val z = num("z", 1f) ?: continue
                val x = num("x", 0.5f) ?: continue
                val y = num("y", 0.5f) ?: continue
                val screen = n.toInt()
                if (screen !in 2..MAX_SCREEN_PICTURE || !NotesJson.isPlainFileName(src) || !NotesJson.isPlainFileName(pic)) continue
                if (out.any { it.screen == screen }) continue
                out += ScreenPicture(screen, src, pic, PicturePlace(z, x, y))
            }
            out
        }
    } catch (_: Exception) { null }
}
