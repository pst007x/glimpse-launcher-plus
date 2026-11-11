package com.glimpsemedia.glimpselauncher.model

import org.json.JSONObject
import kotlin.math.abs

/** One start tag of an icon pack's XML file (appfilter.xml, drawable.xml), with its attributes. */
data class Tag(val name: String, val attrs: Map<String, String>)

/**
 * What an icon pack's appfilter says: [items] maps an app (`package/class`) to a drawable name, [byPackage] the first
 * drawable given for each package; [backs], [masks], [uppers] and [scale] are the rules for apps the pack does not
 * draw; [calendars] maps a calendar app to the prefix of its day-of-the-month drawables.
 */
data class AppFilter(
    val items: Map<String, String> = emptyMap(),
    val byPackage: Map<String, String> = emptyMap(),
    val backs: List<String> = emptyList(),
    val masks: List<String> = emptyList(),
    val uppers: List<String> = emptyList(),
    val scale: Float = 1f,
    val calendars: Map<String, String> = emptyMap(),
) {
    /** Whether the pack says how to fit apps it does not draw. */
    val hasRules get() = backs.isNotEmpty() || masks.isNotEmpty() || uppers.isNotEmpty() || scale != 1f
}

/** `ComponentInfo{pkg/cls}` → `pkg/cls` (a class starting with "." gets the package in front), or null if malformed. */
private fun componentOf(raw: String?): String? {
    val inner = raw?.trim()?.removePrefix("ComponentInfo{")?.removeSuffix("}") ?: return null
    if (inner == raw.trim()) return null
    val pkg = inner.substringBefore('/', "").trim()
    val cls = inner.substringAfter('/', "").trim()
    if (pkg.isEmpty() || cls.isEmpty()) return null
    return "$pkg/" + if (cls.startsWith(".")) pkg + cls else cls
}

/** The `imgN` attributes in N order (packs number from 0 or from 1). */
private fun images(attrs: Map<String, String>): List<String> =
    attrs.entries.mapNotNull { (k, v) -> k.removePrefix("img").toIntOrNull()?.takeIf { k.startsWith("img") && v.isNotBlank() }?.let { it to v.trim() } }
        .sortedBy { it.first }.map { it.second }

fun appFilterOf(tags: Sequence<Tag>): AppFilter {
    val items = LinkedHashMap<String, String>()
    val byPackage = HashMap<String, String>()
    val backs = ArrayList<String>(); val masks = ArrayList<String>(); val uppers = ArrayList<String>()
    val calendars = HashMap<String, String>()
    var scale = 1f
    for (t in tags) when (t.name) {
        "item" -> {
            val c = componentOf(t.attrs["component"]) ?: continue
            val d = t.attrs["drawable"]?.trim().orEmpty().ifEmpty { null } ?: continue
            items.putIfAbsent(c, d)
            byPackage.putIfAbsent(c.substringBefore('/'), d)
        }
        "iconback" -> backs += images(t.attrs)
        "iconmask" -> masks += images(t.attrs)
        "iconupon" -> uppers += images(t.attrs)
        "scale" -> scale = t.attrs["factor"]?.trim()?.toFloatOrNull()?.takeIf { it > 0f && it.isFinite() } ?: 1f
        "calendar" -> {
            val c = componentOf(t.attrs["component"]) ?: continue
            val p = t.attrs["prefix"]?.trim().orEmpty().ifEmpty { null } ?: continue
            calendars[c] = p
        }
    }
    return AppFilter(items, byPackage, backs, masks, uppers, scale, calendars)
}

/** One icon of a pack, for the picker, under its category heading when the pack has them. */
data class DrawableEntry(val name: String, val category: String?)

/** A pack's drawable.xml: its icons in order, under their categories, each once. */
fun drawableListOf(tags: Sequence<Tag>): List<DrawableEntry> {
    var category: String? = null
    val seen = HashSet<String>()
    val out = ArrayList<DrawableEntry>()
    for (t in tags) when (t.name) {
        "category" -> category = t.attrs["title"]?.trim()?.ifEmpty { null }
        "item" -> t.attrs["drawable"]?.trim()?.ifEmpty { null }?.let { if (seen.add(it)) out += DrawableEntry(it, category) }
    }
    return out
}

/** For a pack without drawable.xml: the drawables its appfilter names, A to Z, each once. */
fun drawableListFrom(filter: AppFilter): List<DrawableEntry> =
    filter.items.values.toSortedSet().map { DrawableEntry(it, null) }

/** A calendar app's drawable for a day of the month: the pack's prefix and the day ("calendar_11"). */
fun calendarName(prefix: String, day: Int): String = prefix + day

/** An icon chosen for one app (or a built-in tile): a pack's drawable, or a picture kept by Glimpse. */
sealed interface IconChoice {
    data class Pack(val pack: String, val drawable: String) : IconChoice
    data class Picture(val file: String) : IconChoice
}

/** Where an app's icon comes from, once every rule has been weighed. */
sealed interface IconSource {
    data class Picture(val file: String) : IconSource
    data class PackDrawable(val pack: String, val drawable: String) : IconSource
    data class SavedCopy(val file: String) : IconSource
    data class Fitted(val pack: String) : IconSource
    data object Own : IconSource
}

/** The pack chosen for all apps, with its appfilter. */
data class WholePack(val pack: String, val filter: AppFilter)

/**
 * Which icon an app (or built-in tile) [key] shows: its own choice (a picture; a pack drawable, or that drawable's saved
 * copy when the pack is gone), then the whole pack (today's calendar drawable, the app's drawable, the package's,
 * the saved copy when the pack is gone, the pack's fitting rules), then the app's own icon.
 */
fun chooseIcon(
    key: String,
    component: String,
    choice: IconChoice?,
    wholePack: WholePack?,
    resolves: (pack: String, drawable: String) -> Boolean,
    perAppCopy: (key: String) -> String?,
    packCopy: (key: String) -> String?,
    packInstalled: (pack: String) -> Boolean,
    day: Int,
): IconSource {
    when (choice) {
        is IconChoice.Picture -> return IconSource.Picture(choice.file)
        is IconChoice.Pack ->
            if (packInstalled(choice.pack)) {
                if (resolves(choice.pack, choice.drawable)) return IconSource.PackDrawable(choice.pack, choice.drawable)
            } else perAppCopy(key)?.let { return IconSource.SavedCopy(it) }
        null -> Unit
    }
    val whole = wholePack ?: return IconSource.Own
    if (!packInstalled(whole.pack)) return packCopy(key)?.let { IconSource.SavedCopy(it) } ?: IconSource.Own
    val f = whole.filter
    fun found(d: String?) = d?.takeIf { resolves(whole.pack, it) }?.let { IconSource.PackDrawable(whole.pack, it) }
    f.calendars[component]?.let { prefix -> (found(calendarName(prefix, day)) ?: found(prefix))?.let { return it } }
    found(f.items[component])?.let { return it }
    found(f.byPackage[component.substringBefore('/')])?.let { return it }
    return if (f.hasRules) IconSource.Fitted(whole.pack) else IconSource.Own
}

/** The back picture an app gets when a pack has several: always the same one for the same app. */
fun backFor(key: String, backs: List<String>): String? = if (backs.isEmpty()) null else backs[abs(key.hashCode() % backs.size)]

private val APPLY_EXTRAS = mapOf(
    "org.adw.launcher.SET_THEME" to "org.adw.launcher.theme.NAME",
    "com.anddoes.launcher.SET_THEME" to "com.anddoes.launcher.THEME_PACKAGE_NAME",
    "com.teslacoilsw.launcher.APPLY_ICON_THEME" to "com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE",
)

/** The pack an apply request names, or null (an action Glimpse does not answer, or no package given). */
fun applyPackage(action: String?, extras: Map<String, String?>): String? =
    APPLY_EXTRAS[action]?.let { extras[it] }?.trim()?.ifEmpty { null }

/** An apply request Glimpse acts on (after asking): its pack, only when [isPack] says it is an installed icon pack. */
fun acceptApply(action: String?, extras: Map<String, String?>, isPack: (String) -> Boolean): String? =
    applyPackage(action, extras)?.takeIf(isPack)

/** The icon choices still wanted: those of apps still installed, and the built-in tiles' (keys starting "glimpse:"). */
fun prunedChoices(choices: Map<String, IconChoice>, installed: Set<String>): Map<String, IconChoice> =
    choices.filterKeys { it.startsWith("glimpse:") || it in installed }

/** The picker's search: drawable names read with underscores as spaces, capitals ignored; nothing typed shows all. */
fun matchesSearch(drawable: String, query: String): Boolean {
    val q = query.trim().lowercase()
    return q.isEmpty() || q in drawable.replace('_', ' ').lowercase()
}

/** The centred square of a [w] × [h] picture: left, top, size. */
fun squareCut(w: Int, h: Int): IntArray {
    val size = minOf(w, h)
    return intArrayOf((w - size) / 2, (h - size) / 2, size)
}

/** The saved per-app icon choices (prefs `iconChoices`); anything unreadable reads as no choice. */
fun parseIconChoices(json: String?): Map<String, IconChoice> {
    val o = try { JSONObject(json ?: return emptyMap()) } catch (_: Exception) { return emptyMap() }
    val out = LinkedHashMap<String, IconChoice>()
    for (k in o.keys()) {
        val v = o.optJSONObject(k) ?: continue
        val picture = v.optString("picture")
        val pack = v.optString("pack"); val drawable = v.optString("drawable")
        when {
            picture.isNotEmpty() -> if (isPlainFileName(picture)) out[k] = IconChoice.Picture(picture)
            pack.isNotEmpty() && drawable.isNotEmpty() -> out[k] = IconChoice.Pack(pack, drawable)
        }
    }
    return out
}

fun encodeIconChoices(choices: Map<String, IconChoice>): String = JSONObject().also { o ->
    choices.forEach { (k, c) ->
        o.put(k, when (c) {
            is IconChoice.Pack -> JSONObject().put("pack", c.pack).put("drawable", c.drawable)
            is IconChoice.Picture -> JSONObject().put("picture", c.file)
        })
    }
}.toString()

/** The one extra an apply action names its pack in (the only one Glimpse reads), or null for any other action. */
fun applyExtraKey(action: String?): String? = APPLY_EXTRAS[action]

/** Files in the picked folder that go when [key]'s choice changes: its saved copy, and an old picture no longer used. */
fun filesToDropOnChoice(copyName: String, old: IconChoice?, new: IconChoice?): List<String> =
    if (old == new) emptyList() else listOf(copyName) + listOfNotNull((old as? IconChoice.Picture)?.file)

/** The packs whose version is no longer the one [seen] when they were read (updated, or uninstalled). */
fun changedPacks(seen: Map<String, Long>, versionOf: (String) -> Long): Set<String> =
    seen.filter { (p, v) -> versionOf(p) != v }.keys

/**
 * The drawable a pack's appfilter gives [component] (`package/class`), read only as far as needed: the exact app's
 * drawable as soon as it is found, else the first one given for its package.
 */
fun firstDrawableFor(tags: Sequence<Tag>, component: String): String? {
    val pkg = component.substringBefore('/')
    var byPackage: String? = null
    for (t in tags) {
        if (t.name != "item") continue
        val c = componentOf(t.attrs["component"]) ?: continue
        val d = t.attrs["drawable"]?.trim().orEmpty().ifEmpty { null } ?: continue
        if (c == component) return d
        if (byPackage == null && c.substringBefore('/') == pkg) byPackage = d
    }
    return byPackage
}

/**
 * The icon cache's `pack/version` folders that can go: [dirs] lists each pack folder's version folders, [keep] the
 * packs in use with their current version. Older versions, and packs no longer in use, go.
 */
fun staleVersionDirs(dirs: Map<String, List<String>>, keep: Map<String, Long>): List<String> =
    dirs.flatMap { (p, versions) -> versions.filter { it != keep[p]?.toString() }.map { "$p/$it" } }.sorted()

/** Whether Settings offers "Find icon packs": while no pack besides the built-in Glimpse Colour is installed. */
fun showFindPacks(installed: List<String>): Boolean = installed.none { it != GLIMPSE_COLOUR }

/** A name that stays inside the folder it is used in: not empty, not "." or "..", no folder separators. */
fun isPlainFileName(name: String): Boolean = name.isNotEmpty() && name != "." && name != ".." && '/' !in name && '\\' !in name

/** With Glimpse Colour chosen, an app whose own pick cannot be shown (and that the pack gives nothing) gets Glimpse Colour. */
fun glimpseColourApplies(pack: String, source: IconSource): Boolean = pack == GLIMPSE_COLOUR && source == IconSource.Own

/** Whether the whole pack's saved copies are made again: the pack changed and is still installed (updated, not removed). */
fun refreshPackCopies(changed: Set<String>, pack: String, installed: Boolean): Boolean = pack in changed && installed

/** A photo's EXIF orientation as degrees to turn and whether to mirror (left to right) after turning. */
fun exifTurn(orientation: Int): Pair<Int, Boolean> = when (orientation) {
    2 -> 0 to true; 3 -> 180 to false; 4 -> 180 to true; 5 -> 90 to true
    6 -> 90 to false; 7 -> 270 to true; 8 -> 270 to false; else -> 0 to false
}

// ---- Import icon packs: packs found by their files, not by a signal ----

/** The apps found holding an appfilter that the list does not have yet, A to Z by package (never Glimpse Colour). */
fun newlyFound(candidates: List<String>, listed: Set<String>): List<String> =
    candidates.filter { it != GLIMPSE_COLOUR && it !in listed }.distinct().sorted()

/** The imported packs to add to the list: still installed, and not already found by their signal. */
fun importedStillThere(imported: List<String>, listed: Set<String>, isInstalled: (String) -> Boolean): List<String> =
    imported.filter { it !in listed && isInstalled(it) }

private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

fun encodeImported(list: List<String>): String = list.joinToString("\n")

/** The imported packs as stored; anything that is not a package name is dropped. */
fun decodeImported(text: String?): List<String> =
    text.orEmpty().split('\n').map { it.trim() }.filter { PACKAGE_NAME.matches(it) }.distinct()

/** What Import says when it is done, with the names of the packs it found. */
fun importMessage(found: List<String>): String = when (found.size) {
    0 -> "No more icon packs found"
    1 -> "Found 1 more icon pack: ${found[0]}"
    else -> "Found ${found.size} more icon packs: ${found.joinToString(", ")}"
}

// ---- Apply the pack to every icon ----

/** The button, shown only while [changed] icons have been changed one by one. */
fun applyAllLabel(changed: Int): String? = if (changed <= 0) null else "Apply the pack to every icon ($changed changed one by one)"

/** The question before clearing [changed] single choices; [pack] is the chosen pack's name, null for None. */
fun applyAllWarning(changed: Int, pack: String?): String {
    val what = if (changed == 1) "the icon you changed one by one (and any picture you chose for it)"
        else "the $changed icons you changed one by one (and any pictures you chose for them)"
    val result = if (pack == null) "every icon is the app's own" else "every icon comes from $pack"
    return "This clears $what, so $result. This can't be undone."
}
