package com.glimpsemedia.glimpselauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import com.glimpsemedia.glimpselauncher.model.AppFilter
import com.glimpsemedia.glimpselauncher.model.DrawableEntry
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_COLOUR
import com.glimpsemedia.glimpselauncher.model.Tag
import com.glimpsemedia.glimpselauncher.model.appFilterOf
import com.glimpsemedia.glimpselauncher.model.drawableListFrom
import com.glimpsemedia.glimpselauncher.model.drawableListOf
import org.xmlpull.v1.XmlPullParser

/** An icon pack installed on the phone. */
data class PackInfo(val pkg: String, val label: String)

/** The actions icon packs declare so launchers find them (Nova, ADW, Apex, Go, Teslacoil). */
val PACK_ACTIONS = listOf(
    "org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.teslacoilsw.launcher.THEME", "com.anddoes.launcher.THEME",
    "com.gau.go.launcherex.theme", "org.adw.ActivityStarter.THEMES",
    // Newer launchers' own signals.
    "ch.deletescape.lawnchair.ICONPACK", "net.oneplus.launcher.icons.ACTION_PICK_ICON", "com.dlto.atom.launcher.THEME",
)

/** Where Import icon packs keeps the packs it found (in the settings, so a backup keeps them). */
const val IMPORTED_KEY = "importedPacks"

/** Packs that announce themselves with a category on their main activity instead. */
val PACK_CATEGORIES = listOf("com.fede.launcher.THEME_ICONPACK", "com.anddoes.launcher.THEME", "com.teslacoilsw.launcher.THEME")

/**
 * Finding and reading icon packs: which are installed, their appfilter (which drawable each app gets, and the rules
 * for the rest), their list of icons, and the drawables themselves. Reads only the pack apps' own files.
 */
class IconPacks(context: Context) {
    private val app = context.applicationContext
    private val pm = app.packageManager
    private val filters = HashMap<String, Pair<Long, AppFilter>>()
    private val resources = java.util.concurrent.ConcurrentHashMap<String, Resources>()
    /** The version of each pack whose files have been read, to notice a pack updated (or removed) while Glimpse runs. */
    private val seen = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** Glimpse Colour (built in) first, then every installed pack, A to Z by name. */
    fun installed(): List<PackInfo> {
        val signalled = signalledPacks()
        val all = signalled + com.glimpsemedia.glimpselauncher.model.importedStillThere(imported(), signalled.toSet(), ::isInstalled)
        return listOf(PackInfo(GLIMPSE_COLOUR, "Glimpse Colour (built in)")) + all.distinct().mapNotNull(::info).sortedBy { it.label.lowercase() }
    }

    private fun info(p: String): PackInfo? = try { PackInfo(p, pm.getApplicationInfo(p, 0).loadLabel(pm).toString()) } catch (_: Exception) { null }

    /** The packs that announce themselves with one of the usual signals. */
    private fun signalledPacks(): List<String> {
        fun q(i: Intent) = try { pm.queryIntentActivities(i, 0) } catch (_: Exception) { emptyList() }
        val found = PACK_ACTIONS.flatMap { q(Intent(it)) } + PACK_CATEGORIES.flatMap { q(Intent(Intent.ACTION_MAIN).addCategory(it)) }
        return found.map { it.activityInfo.packageName }.distinct()
    }

    private val prefs get() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private fun imported(): List<String> = com.glimpsemedia.glimpselauncher.model.decodeImported(prefs.getString(IMPORTED_KEY, null))

    /**
     * Import icon packs: looks through every app with an icon in All apps for the appfilter every icon pack carries,
     * and keeps those the usual signals missed in the list. Slow (it opens each app's files): call off the main thread.
     * Returns the names of the packs it found.
     */
    fun importPacks(): List<String> {
        val launchable = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }
        } catch (_: Exception) { emptyList() }
        val listed = (signalledPacks() + imported()).toSet() + app.packageName
        val found = com.glimpsemedia.glimpselauncher.model.newlyFound(launchable, listed).filter(::hasAppFilter)
        if (found.isNotEmpty()) {
            val keep = (imported().filter(::isInstalled) + found).distinct()
            prefs.edit().putString(IMPORTED_KEY, com.glimpsemedia.glimpselauncher.model.encodeImported(keep)).apply()
        }
        return found.mapNotNull(::info).map { it.label }
    }

    /** Whether [pkg] carries an appfilter (res/xml or assets), read without keeping its files open. */
    @Suppress("DiscouragedApi")
    private fun hasAppFilter(pkg: String): Boolean = try {
        val r = pm.getResourcesForApplication(pkg)
        r.getIdentifier("appfilter", "xml", pkg) != 0 || try { r.assets.open("appfilter.xml").close(); true } catch (_: java.io.IOException) { false }
    } catch (_: Exception) { false }

    fun isInstalled(pkg: String): Boolean = pkg == GLIMPSE_COLOUR || try { pm.getPackageInfo(pkg, 0); true } catch (_: PackageManager.NameNotFoundException) { false }

    /** The pack's version code, so pictures made from an older version are never shown; -1 when not installed. */
    fun versionOf(pkg: String): Long = if (pkg == GLIMPSE_COLOUR) com.glimpsemedia.glimpselauncher.BuildConfig.VERSION_CODE.toLong() else try {
        val i = pm.getPackageInfo(pkg, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) i.longVersionCode else @Suppress("DEPRECATION") i.versionCode.toLong()
    } catch (_: Exception) { -1 }

    fun appIcon(pkg: String): Drawable? = if (pkg == GLIMPSE_COLOUR) drawable(pkg, "grid_2x2") else try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }

    private fun res(pkg: String): Resources? = resources[pkg] ?: try {
        pm.getResourcesForApplication(pkg).also { resources[pkg] = it; seen[pkg] = versionOf(pkg) }
    } catch (_: Exception) { null }

    /** Forgets what was read from packs updated or removed since, and names them (their icons are redrawn). */
    @Synchronized
    fun dropChanged(): Set<String> {
        val changed = com.glimpsemedia.glimpselauncher.model.changedPacks(seen, ::versionOf)
        changed.forEach { resources.remove(it); filters.remove(it); seen.remove(it) }
        return changed
    }

    /**
     * What [pkg] draws for one app (`package/class`), for the picker's Suggested row: the whole pack's appfilter when it
     * is already read, else the file read only up to the app's entry and not kept.
     */
    fun suggestionFor(pkg: String, component: String): String? {
        if (pkg == GLIMPSE_COLOUR) return null
        synchronized(this) { filters[pkg] }?.let { (ver, f) -> if (ver == versionOf(pkg)) return (f.items[component] ?: f.byPackage[component.substringBefore('/')])?.takeIf { resolves(pkg, it) } }
        return try { withTags(pkg, "appfilter") { com.glimpsemedia.glimpselauncher.model.firstDrawableFor(it, component) } } catch (_: Exception) { null }
            ?.takeIf { resolves(pkg, it) }
    }

    /** The pack's appfilter (read once per pack version); an empty one when it cannot be read. */
    @Synchronized
    fun appFilter(pkg: String): AppFilter {
        if (pkg == GLIMPSE_COLOUR) return AppFilter()   // it covers apps by what they do (IconStore), not by name
        val v = versionOf(pkg)
        filters[pkg]?.let { (ver, f) -> if (ver == v) return f }
        resources.remove(pkg)
        val f = try { appFilterOf(tags(pkg, "appfilter").asSequence()) } catch (_: Exception) { AppFilter() }
        filters[pkg] = v to f
        return f
    }

    /** The pack's icons for the picker: its drawable.xml, else the drawables its appfilter names. */
    fun drawableList(pkg: String): List<DrawableEntry> = if (pkg == GLIMPSE_COLOUR)
        com.glimpsemedia.glimpselauncher.model.GC_CATEGORIES.flatMap { (title, names) -> names.map { DrawableEntry(it, title) } }.distinctBy { it.name } else
        (try { drawableListOf(tags(pkg, "drawable").asSequence()) } catch (_: Exception) { emptyList() })
            .filter { resolves(pkg, it.name) }
            .ifEmpty { drawableListFrom(appFilter(pkg)).filter { resolves(pkg, it.name) } }

    fun resolves(pkg: String, name: String): Boolean = drawableId(pkg, name) != 0

    @Suppress("DiscouragedApi")
    private fun drawableId(pkg: String, name: String): Int =
        if (pkg == GLIMPSE_COLOUR) app.resources.getIdentifier("gc_$name", "drawable", app.packageName)
        else res(pkg)?.getIdentifier(name, "drawable", pkg) ?: 0

    fun drawable(pkg: String, name: String): Drawable? {
        val r = if (pkg == GLIMPSE_COLOUR) app.resources else res(pkg) ?: return null
        val id = drawableId(pkg, name)
        if (id == 0) return null
        return try { r.getDrawable(id, null) } catch (_: Exception) { null }
    }

    /** The start tags of the pack's `res/xml/<name>` (preferred) or `assets/<name>.xml`; empty when neither exists. */
    private fun tags(pkg: String, name: String): List<Tag> = withTags(pkg, name) { it.toList() }

    /** Hands [block] the file's start tags as they are read (it may stop early); the file is closed afterwards. */
    @Suppress("DiscouragedApi")
    private fun <T> withTags(pkg: String, name: String, block: (Sequence<Tag>) -> T): T {
        val r = res(pkg) ?: return block(emptySequence())
        val id = r.getIdentifier(name, "xml", pkg)
        if (id != 0) r.getXml(id).use { return block(read(it)) }
        return try {
            r.assets.open("$name.xml").use { stream ->
                val p = Xml.newPullParser()
                p.setInput(stream, null)
                block(read(p))
            }
        } catch (_: java.io.IOException) { block(emptySequence()) }
    }

    private fun read(p: XmlPullParser): Sequence<Tag> = sequence {
        var e = p.eventType
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG) yield(Tag(p.name, (0 until p.attributeCount).associate { p.getAttributeName(it) to (p.getAttributeValue(it) ?: "") }))
            e = try { p.next() } catch (_: Exception) { XmlPullParser.END_DOCUMENT }
        }
    }
}

private inline fun <T> android.content.res.XmlResourceParser.use(block: (android.content.res.XmlResourceParser) -> T): T =
    try { block(this) } finally { close() }
