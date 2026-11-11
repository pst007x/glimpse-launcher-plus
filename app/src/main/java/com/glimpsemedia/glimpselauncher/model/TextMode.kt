package com.glimpsemedia.glimpselauncher.model

/** The built-in entries a text-mode list or folder can hold beside apps. */
const val GLIMPSE_SETTINGS = "glimpse:settings"
const val SYSTEM_SETTINGS = "system:settings"
/** Opens the search window. */
const val GLIMPSE_SEARCH = "glimpse:search"

fun isBuiltInEntry(key: String) = key == GLIMPSE_SETTINGS || key == SYSTEM_SETTINGS || key == GLIMPSE_SEARCH

/** The mono icons a text-mode folder can have. */
enum class TextIcon { Folder, Files, Images, Messages, Videos, Work, Film, Shopping, Music, Travel, Search, Health, AI, Money, Security, Settings, Games, Social, News, Tools, Star,
    // Added in 1.0.19: after the first 21, so the pickers keep their old order at the top.
    Phone, Email, Camera, Calendar, Maps, Weather, Food, Fitness, Sport, Kids, School, Books, Podcasts, TV, Notes, Home, Car, Cloud, Documents, Pets }

/** A saved icon name back to its icon; an unknown name reads as Folder. */
fun textIconOf(name: String?): TextIcon = TextIcon.entries.firstOrNull { it.name == name } ?: TextIcon.Folder

/** A text-mode folder: its entries are app keys ("package/Class") or built-in entries. */
data class TextFolder(val id: String, val name: String, val icon: TextIcon, val entries: List<String>, val color: Int = 0)

/** The text screen's main list and its folders, separate from the tile screens. */
data class TextLayout(val apps: List<String> = emptyList(), val folders: List<TextFolder> = emptyList(), val widgets: List<TextWidget> = emptyList())

/** The widgets screen's heights, in dp: Small, Medium, Large, Extra large. */
val TEXT_WIDGET_HEIGHTS_DP = listOf(110, 200, 300, 420)

/** A widget on text mode's widgets screen: its host id, its provider component, and its height (0..3). */
data class TextWidget(val id: Int, val provider: String, val height: Int)

/** The height nearest a widget's own minimum height (the smaller one on a tie). */
fun textWidgetHeightFor(minHeightDp: Int): Int =
    TEXT_WIDGET_HEIGHTS_DP.indices.minBy { kotlin.math.abs(TEXT_WIDGET_HEIGHTS_DP[it] - minHeightDp) }

fun TextLayout.withWidget(w: TextWidget): TextLayout =
    if (widgets.any { it.id == w.id }) copy(widgets = widgets.map { if (it.id == w.id) w else it }) else copy(widgets = widgets + w)
fun TextLayout.withoutWidget(id: Int): TextLayout = copy(widgets = widgets.filter { it.id != id })
fun TextLayout.widgetMoved(id: Int, by: Int): TextLayout = copy(widgets = widgets.moved(widgets.indexOfFirst { it.id == id }, by))
fun TextLayout.widgetResized(id: Int, height: Int): TextLayout =
    copy(widgets = widgets.map { if (it.id == id) it.copy(height = height.coerceIn(0, 3)) else it })

/** The widget ids the host no longer [known]s (their app was uninstalled, or the host forgot them). */
fun TextLayout.widgetsGone(known: (Int) -> Boolean): List<Int> = widgets.map { it.id }.filterNot(known)

/** Settings lists a text-mode list the way the screen shows it: the last item at the top. */
fun <T> shownInScreenOrder(items: List<T>): List<T> = items.reversed()

/** A move on the screen (−1 up, +1 down) as a move in the stored list: up on screen is later in the list. */
fun onScreenMove(by: Int): Int = -by

/** The list with the item at [index] moved [by] places (kept within the list); a bad [index] changes nothing. */
fun <T> List<T>.moved(index: Int, by: Int): List<T> {
    if (index < 0) return this
    val to = (index + by).coerceIn(0, size - 1)
    if (to == index) return this
    return toMutableList().also { it.add(to, it.removeAt(index)) }
}

fun TextLayout.withApp(key: String): TextLayout = if (key in apps) this else copy(apps = apps + key)
fun TextLayout.withoutApp(key: String): TextLayout = copy(apps = apps - key)
fun TextLayout.appMoved(key: String, by: Int): TextLayout = copy(apps = apps.moved(apps.indexOf(key), by))

fun TextLayout.withFolder(f: TextFolder): TextLayout =
    if (folders.any { it.id == f.id }) copy(folders = folders.map { if (it.id == f.id) f else it }) else copy(folders = folders + f)
fun TextLayout.withoutFolder(id: String): TextLayout = copy(folders = folders.filter { it.id != id })
fun TextLayout.folderMoved(id: String, by: Int): TextLayout = copy(folders = folders.moved(folders.indexOfFirst { it.id == id }, by))

fun TextFolder.withEntry(key: String): TextFolder = if (key in entries) this else copy(entries = entries + key)
fun TextFolder.withoutEntry(key: String): TextFolder = copy(entries = entries - key)
fun TextFolder.entryMoved(key: String, by: Int): TextFolder = copy(entries = entries.moved(entries.indexOf(key), by))

/** Apps no longer installed leave the list and every folder; the built-in entries always stay. */
fun TextLayout.withoutMissing(installed: Set<String>): TextLayout {
    fun keep(key: String) = isBuiltInEntry(key) || key in installed
    return copy(apps = apps.filter(::keep), folders = folders.map { f -> f.copy(entries = f.entries.filter(::keep)) })
}

const val STARTING_FOLDER_ID = "tf-settings"

/** The first text layout: the phone's own Phone, Messages, Gallery and browser (missing ones left out), and a Settings folder. */
fun startingTextLayout(phone: String?, messages: String?, gallery: String?, browser: String?): TextLayout = TextLayout(
    apps = listOfNotNull(phone, messages, gallery, browser).distinct(),
    folders = listOf(TextFolder(STARTING_FOLDER_ID, "Settings", TextIcon.Folder, listOf(SYSTEM_SETTINGS, GLIMPSE_SETTINGS))),
)

/** The order a bottom-up column is drawn in from the top: the first item ends up lowest. */
fun <T> drawnBottomUp(items: List<T>): List<T> = items.reversed()

/** What tapping "Glimpse Settings" does. */
enum class SettingsTap { Open, AskPin, HoldPadlock }

/** Never a way round the padlock: locked, it asks for the PIN, or (with no PIN) says to hold the padlock. */
fun glimpseSettingsTap(unlocked: Boolean, hasPin: Boolean): SettingsTap = when {
    unlocked -> SettingsTap.Open
    hasPin -> SettingsTap.AskPin
    else -> SettingsTap.HoldPadlock
}
