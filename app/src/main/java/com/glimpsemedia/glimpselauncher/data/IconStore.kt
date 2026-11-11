package com.glimpsemedia.glimpselauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.glimpsemedia.glimpselauncher.model.GC_BUILT_IN
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_COLOUR
import com.glimpsemedia.glimpselauncher.model.GcRole
import com.glimpsemedia.glimpselauncher.model.gcColourFor
import com.glimpsemedia.glimpselauncher.model.gcDrawableFor
import com.glimpsemedia.glimpselauncher.model.IconChoice
import com.glimpsemedia.glimpselauncher.model.IconSource
import com.glimpsemedia.glimpselauncher.model.encodeIconChoices
import com.glimpsemedia.glimpselauncher.model.parseIconChoices
import com.glimpsemedia.glimpselauncher.model.squareCut
import com.glimpsemedia.glimpselauncher.model.WholePack
import com.glimpsemedia.glimpselauncher.model.backFor
import com.glimpsemedia.glimpselauncher.model.chooseIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

/** Where a finished pack icon is kept on storage: pack, its version, the app (and the day, for calendars). */
fun iconCacheName(pack: String, version: Long, key: String, day: Int?, stamp: Long = 0, variant: String = ""): String =
    "$pack/$version/" + key.replace('/', '_').replace(Regex("[^A-Za-z0-9._$-]"), "_") + (day?.let { "-$it" } ?: "") +
        (if (stamp != 0L) "-u$stamp" else "") + (if (variant.isNotEmpty()) "-" + variant.replace(Regex("[^A-Za-z0-9_]"), "_") else "") + ".png"

/**
 * Every app icon Glimpse shows goes through here: the chosen pack (or none), finished 192 × 192 pictures kept in
 * memory and on storage, so screens stay quick. Changing the pack bumps [version], which redraws the icons.
 */
class IconStore(context: Context) {
    private val app = context.applicationContext
    val packs = IconPacks(app)
    private val cacheDir = File(app.cacheDir, "icons")
    /** Chosen pictures and saved copies of per-app picks: kept, and backed up. */
    val pickedDir = File(app.filesDir, "icons/picked")
    /** Saved copies of the whole pack's icons, so they still show if the pack app is uninstalled (not backed up). */
    private val packDir = File(app.filesDir, "icons/pack")
    private var started = false
    private var day = 0
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    @Volatile private var choices: Map<String, IconChoice> = parseIconChoices(prefs.getString(CHOICES_KEY, null))

    /** Goes up whenever icons may look different (another pack, a new choice), so every icon is read again. */
    var version by mutableIntStateOf(0)
        private set

    @Volatile private var pack: String = ""
    private val memory = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    /** The pack chosen for all apps ("" for none). */
    fun setPack(id: String) {
        if (id == pack && started) return
        // Another pack (or none) chosen by the person: the old pack's saved copies go. Not at start-up.
        if (started && id != pack) packDir.listFiles()?.forEach { it.delete() }
        pack = id
        started = true
        memory.evictAll()
        version++
    }

    /** The whole pack's name for Settings, and whether it is gone but its saved icons are still shown. */
    fun usingSavedCopies(): Boolean = pack.isNotEmpty() && !packs.isInstalled(pack) && (packDir.list()?.isNotEmpty() == true)

    /** Which app the phone uses for each main job (its default dialer, camera, browser…): read again on resume. */
    @Volatile private var roles: Map<String, Set<GcRole>>? = null

    private fun rolesByPackage(): Map<String, Set<GcRole>> = roles ?: run {
        val pm = app.packageManager
        fun main(category: String) = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(category)
        val asks = listOf(
            GcRole.PHONE to android.content.Intent(android.content.Intent.ACTION_DIAL),
            GcRole.MESSAGES to main(android.content.Intent.CATEGORY_APP_MESSAGING),
            GcRole.CAMERA to android.content.Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            GcRole.GALLERY to main(android.content.Intent.CATEGORY_APP_GALLERY),
            GcRole.CALENDAR to main(android.content.Intent.CATEGORY_APP_CALENDAR),
            GcRole.CLOCK to android.content.Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS),
            GcRole.CALCULATOR to main(android.content.Intent.CATEGORY_APP_CALCULATOR),
            GcRole.CONTACTS to main(android.content.Intent.CATEGORY_APP_CONTACTS),
            GcRole.EMAIL to main(android.content.Intent.CATEGORY_APP_EMAIL),
            GcRole.BROWSER to main(android.content.Intent.CATEGORY_APP_BROWSER),
            GcRole.MAPS to main(android.content.Intent.CATEGORY_APP_MAPS),
            GcRole.MUSIC to main(android.content.Intent.CATEGORY_APP_MUSIC),
            GcRole.STORE to main(android.content.Intent.CATEGORY_APP_MARKET),
            GcRole.SETTINGS to android.content.Intent(android.provider.Settings.ACTION_SETTINGS),
        ) + (if (android.os.Build.VERSION.SDK_INT >= 29) listOf(GcRole.FILES to main(android.content.Intent.CATEGORY_APP_FILES)) else emptyList())
        val out = HashMap<String, MutableSet<GcRole>>()
        for ((role, intent) in asks) {
            // Only the app the phone would open for it (its default); "ask every time" counts as none.
            val pkg = try { pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName } catch (_: Exception) { null }
            if (pkg != null && pkg != "android" && !pkg.contains("resolver")) out.getOrPut(pkg) { HashSet() } += role
        }
        out.also { roles = it }
    }

    /** Glimpse Colour: the app's drawing when it is one of the phone's main apps, else its own logo in a Glimpse colour. */
    private fun glimpseColour(info: AppInfo): ImageBitmap? {
        val drawing = gcDrawableFor(rolesByPackage()[info.component.packageName].orEmpty())
        return if (drawing != null) cached(GLIMPSE_COLOUR, info.key, null, variant = drawing) { packs.drawable(GLIMPSE_COLOUR, drawing)?.let { IconMaker.draw(it) } }
        else cached(GLIMPSE_COLOUR, info.key, null, stamp = updated(info), variant = "tile") {
            val category = try { app.packageManager.getApplicationInfo(info.component.packageName, 0).category } catch (_: Exception) { -1 }
            own(info)?.let { IconMaker.glimpse(it, gcColourFor(category, info.topics, info.key)) }
        }
    }

    /** A new day: calendar icons are read again. Called on resume and at midnight. */
    fun checkDay() {
        roles = null
        val d = today()
        val changed = packs.dropChanged()
        // The whole pack was updated: its saved copies are made again from the new version (kept if it was removed).
        if (com.glimpsemedia.glimpselauncher.model.refreshPackCopies(changed, pack, packs.isInstalled(pack))) packDir.listFiles()?.forEach { it.delete() }
        if ((day != 0 && d != day) || changed.isNotEmpty()) { memory.evictAll(); version++ }
        day = d
    }

    /** Drops the icon choices (and pictures) of apps that are no longer installed. */
    fun prune(installed: Set<String>) {
        val keep = com.glimpsemedia.glimpselauncher.model.prunedChoices(choices, installed)
        if (keep.size == choices.size) return
        (choices - keep.keys).values.filterIsInstance<IconChoice.Picture>().forEach { File(pickedDir, it.file).delete() }
        (choices.keys - keep.keys).forEach { File(pickedDir, copyName(it)).delete() }
        choices = keep
        prefs.edit().putString(CHOICES_KEY, encodeIconChoices(choices)).apply()
    }

    private fun safe(key: String) = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun copyName(key: String) = "copy-" + safe(key) + ".png"

    private fun writeCopy(file: File, b: ImageBitmap) {
        try { file.parentFile?.mkdirs(); file.outputStream().use { b.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) } } catch (_: Exception) { }
    }

    private fun today() = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    /** An app's (or built-in tile's) own icon choice, if it has one. */
    fun choiceFor(key: String): IconChoice? = choices[key]

    /** Gives [key] its own icon, or (null) takes it away; a picture no longer used is deleted. */
    fun setChoice(key: String, choice: IconChoice?) {
        val old = choices[key]
        choices = if (choice == null) choices - key else choices + (key to choice)
        prefs.edit().putString(CHOICES_KEY, encodeIconChoices(choices)).apply()
        com.glimpsemedia.glimpselauncher.model.filesToDropOnChoice(copyName(key), old, choice).forEach { File(pickedDir, it).delete() }
        // Other icons are unchanged; only this one's saved copy (now deleted) must not be shown from memory.
        memory.remove("copy/picked/" + copyName(key))
        version++
    }

    /** How many icons have been changed one by one (apps and built-in tiles). */
    fun singleChoiceCount(): Int = choices.size

    /** Apply the pack to every icon: clears every single choice and its pictures and copies; every icon is redrawn. */
    fun clearAllChoices() {
        val old = choices
        choices = emptyMap()
        prefs.edit().putString(CHOICES_KEY, encodeIconChoices(choices)).apply()
        for ((key, c) in old) com.glimpsemedia.glimpselauncher.model.filesToDropOnChoice(copyName(key), c, null).forEach { File(pickedDir, it).delete() }
        memory.evictAll()
        version++
    }

    /**
     * Keeps a picture from the phone as [key]'s icon: decoded at most 1024 px wide (so a huge photo cannot run out of
     * memory), cut to its centred square, made 192 × 192 and saved in [pickedDir]. Null when it cannot be read.
     */
    suspend fun savePicture(key: String, uri: android.net.Uri): IconChoice.Picture? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1024) sample *= 2
            val full = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: return@withContext null
            val (l, t, size) = squareCut(full.width, full.height).let { Triple(it[0], it[1], it[2]) }
            // Photos taken sideways say so in their EXIF data: turned (and mirrored) the right way up.
            val orientation = try { app.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } ?: 1 } catch (_: Exception) { 1 }
            val (turn, mirror) = com.glimpsemedia.glimpselauncher.model.exifTurn(orientation)
            val matrix = android.graphics.Matrix().apply { setRotate(turn.toFloat()); if (mirror) postScale(-1f, 1f) }
            val square = Bitmap.createScaledBitmap(Bitmap.createBitmap(full, l, t, size, size, matrix, true), ICON_PX, ICON_PX, true)
            pickedDir.mkdirs()
            val name = "pic-" + key.replace(Regex("[^A-Za-z0-9._-]"), "_") + "-" + System.currentTimeMillis() + ".png"
            File(pickedDir, name).outputStream().use { square.compress(Bitmap.CompressFormat.PNG, 100, it) }
            IconChoice.Picture(name)
        } catch (_: Exception) { null }
    }

    /** A built-in tile's (All apps, Search, Emergency) chosen icon; null keeps Glimpse's own drawing. */
    suspend fun bitmapForKey(key: String): ImageBitmap? = withContext(Dispatchers.IO) {
        when (val c = choices[key]) {
            is IconChoice.Picture -> picture(c.file)
            is IconChoice.Pack -> {
                val copy = File(pickedDir, copyName(key))
                if (packs.isInstalled(c.pack)) cached(c.pack, "own-$key-${c.drawable}", null) { packs.drawable(c.pack, c.drawable)?.let { IconMaker.draw(it) } }
                    ?.also { if (!copy.isFile) writeCopy(copy, it) }
                // The pack was uninstalled: the copy saved when it was in use.
                else if (copy.isFile) savedCopy("picked/" + copy.name) else null
            }
            // With Glimpse Colour chosen, the built-in tiles take its matching drawings.
            null -> GC_BUILT_IN[key]?.takeIf { pack == GLIMPSE_COLOUR }?.let { d -> cached(GLIMPSE_COLOUR, key, null) { packs.drawable(GLIMPSE_COLOUR, d)?.let { IconMaker.draw(it) } } }
        }
    }

    /** One icon of a pack, for the picker's grid. */
    suspend fun packIcon(pack: String, drawable: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val name = "grid/$pack/$drawable"
        memory.get(name) ?: safely { packs.drawable(pack, drawable)?.let { IconMaker.draw(it) } }?.asImageBitmap()?.also { memory.put(name, it) }
    }

    /** An app's own icon, unthemed (the picker's Original). */
    suspend fun originalFor(info: AppInfo): ImageBitmap? = withContext(Dispatchers.IO) { ownBitmap(info) }

    private fun picture(file: String): ImageBitmap? = memory.get("pic/$file") ?: safely {
        BitmapFactory.decodeFile(File(pickedDir, file).path)?.asImageBitmap()?.also { memory.put("pic/$file", it) }
    }

    /** The app's icon as it should look now; null only when even the app's own icon cannot be read. */
    suspend fun bitmapFor(info: AppInfo): ImageBitmap? = withContext(Dispatchers.IO) {
        val day = today()
        val p = pack
        if (p == GLIMPSE_COLOUR && choices[info.key] == null) return@withContext glimpseColour(info)
        val whole = if (p.isEmpty()) null else WholePack(p, packs.appFilter(p))
        val source = chooseIcon(
            key = info.key, component = info.key, choice = choices[info.key], wholePack = whole,
            resolves = packs::resolves,
            perAppCopy = { k -> copyName(k).takeIf { File(pickedDir, it).isFile }?.let { "picked/$it" } },
            packCopy = { k -> (safe(k) + ".png").takeIf { File(packDir, it).isFile }?.let { "pack/$it" } },
            packInstalled = packs::isInstalled, day = day,
        )
        if (com.glimpsemedia.glimpselauncher.model.glimpseColourApplies(p, source)) return@withContext glimpseColour(info)
        val result = when (source) {
            is IconSource.Picture -> picture(source.file) ?: ownBitmap(info)
            is IconSource.SavedCopy -> savedCopy(source.file) ?: ownBitmap(info)
            is IconSource.PackDrawable -> cached(source.pack, if (source.pack == whole?.pack && choices[info.key] == null) info.key else "own-${info.key}-${source.drawable}",
                if (whole?.filter?.calendars?.containsKey(info.key) == true) day else null) {
                packs.drawable(source.pack, source.drawable)?.let { IconMaker.draw(it) }
            }
            is IconSource.Fitted -> cached(source.pack, info.key, null, stamp = updated(info)) {
                val f = whole!!.filter
                own(info)?.let { o ->
                    IconMaker.fitted(
                        o, backFor(info.key, f.backs)?.let { packs.drawable(source.pack, it) },
                        f.masks.firstOrNull()?.let { packs.drawable(source.pack, it) },
                        f.uppers.firstOrNull()?.let { packs.drawable(source.pack, it) }, f.scale,
                    )
                }
            }
            else -> ownBitmap(info)
        }
        // Saved copies: a per-app pick in icons/picked (backed up), the whole pack's in icons/pack.
        if (result != null) when {
            source is IconSource.PackDrawable && choices[info.key] != null -> File(pickedDir, copyName(info.key)).let { if (!it.isFile) writeCopy(it, result) }
            source is IconSource.PackDrawable || source is IconSource.Fitted -> File(packDir, safe(info.key) + ".png").let { if (!it.isFile || source.let { s -> s is IconSource.PackDrawable && whole?.filter?.calendars?.containsKey(info.key) == true }) writeCopy(it, result) }
        }
        result
    }

    private fun savedCopy(rel: String): ImageBitmap? = memory.get("copy/$rel") ?: safely {
        BitmapFactory.decodeFile(File(app.filesDir, "icons/$rel").path)?.asImageBitmap()?.also { memory.put("copy/$rel", it) }
    }

    private fun own(info: AppInfo) = try { app.packageManager.getActivityIcon(info.component) } catch (_: Exception) { null }

    private fun ownBitmap(info: AppInfo): ImageBitmap? = safely { own(info)?.toBitmap(ICON_PX, ICON_PX)?.asImageBitmap() }

    /** When the app was last installed or updated, so an icon made from its own logo is made again after an update. */
    private fun updated(info: AppInfo): Long = try { app.packageManager.getPackageInfo(info.component.packageName, 0).lastUpdateTime } catch (_: Exception) { 0 }

    /**
     * Deletes stored pack icons that can no longer be shown: older versions of a pack, and packs not in use (neither the
     * whole pack nor any app's pick). They are made again if needed.
     */
    suspend fun tidyCache() = withContext(Dispatchers.IO) {
        val inUse = (listOf(pack) + choices.values.filterIsInstance<IconChoice.Pack>().map { it.pack }).filter { it.isNotEmpty() }.distinct()
        val keep = inUse.associateWith { packs.versionOf(it) }
        val dirs = cacheDir.listFiles()?.filter { it.isDirectory }?.associate { d -> d.name to (d.list()?.toList().orEmpty()) }.orEmpty()
        com.glimpsemedia.glimpselauncher.model.staleVersionDirs(dirs, keep).forEach { File(cacheDir, it).deleteRecursively() }
        cacheDir.listFiles()?.forEach { if (it.isDirectory && it.list()?.isEmpty() == true) it.delete() }
    }

    /** A pack icon from memory, else from storage, else made now and kept in both. */
    private fun cached(pack: String, key: String, day: Int?, stamp: Long = 0, variant: String = "", make: () -> Bitmap?): ImageBitmap? {
        val name = iconCacheName(pack, packs.versionOf(pack), key, day, stamp, variant)
        memory.get(name)?.let { return it }
        val file = File(cacheDir, name)
        val fromDisk = if (file.isFile) safely { BitmapFactory.decodeFile(file.path) } else null
        val bmp = fromDisk ?: safely(make)?.also { b ->
            try { file.parentFile?.mkdirs(); file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } } catch (_: Exception) { }
        } ?: return null
        return bmp.asImageBitmap().also { memory.put(name, it) }
    }

    private companion object { const val CHOICES_KEY = "iconChoices" }
}
