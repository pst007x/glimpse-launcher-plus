package com.glimpsemedia.glimpselauncher.data

import android.content.Context
import com.glimpsemedia.glimpselauncher.model.Contact
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.model.LayoutJson
import java.io.File

/** What the set-up person can switch. */
data class Settings(
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    /** Minutes of no touching after which the screen locks again; 0 means never. */
    val autoLockMinutes: Int = 10,
    /** Colours of the large tiles and of the bottom bar, as ARGB numbers. */
    val tileColor: Int = 0,
    val barColor: Int = 0,
    /** The clock and date; 0 is white. */
    val clockColor: Int = 0,
    /** Dark or light menus (and the standard tile and bar colours that go with them). */
    val darkTheme: Boolean = true,
    /** 0 digital, 1 time and date side by side, 2 analogue face. */
    val clockStyle: Int = 0,
    val clockBackground: Boolean = false,
    /** The colour behind the clock; 0 is the standard bar colour. */
    val clockBackColor: Int = 0,
    /** How see-through the tiles are, in percent: 0 (solid), 25, 50, 75 or 100. */
    val tileTransparency: Int = 0,
    /** The colour of highlights and selections; 0 is the standard yellow. */
    val highlightColor: Int = 0,
    /** How see-through each of the other chosen colours is, in percent (0 solid to 100 clear); the highlight colour is always solid. */
    val barTransparency: Int = 0,
    val clockTransparency: Int = 0,
    val clockBackTransparency: Int = 0,
    /** The Emergency button is on, and who it rings. */
    val emergencyEnabled: Boolean = false,
    val emergencyContact: Contact? = null,
    /** The first-run welcome has been shown. */
    val welcomeShown: Boolean = false,
    /** Free layout: icons can sit in any slot instead of packing to the top. */
    val customLayout: Boolean = false,
    /** Seconds from touching the Emergency picture to the call: 5, 10, 15 or 20. */
    val emergencyCountdown: Int = DEFAULT_EMERGENCY_COUNTDOWN,
    /** 0 small, 1 medium, 2 large (the size it always had). */
    val clockSize: Int = CLOCK_LARGE,
    /** Full screen: the phone's top bar is hidden; a swipe down from the top shows it for a moment. */
    val fullScreen: Boolean = false,
    /** The small square behind each icon: its colour (0 is the standard) and how see-through it is (100 hides it). */
    val iconColor: Int = 0,
    val iconTransparency: Int = 100,
    /**
     * The welcome page being shown. Kept with the settings because making Glimpse Launcher the home screen restarts
     * it, and the welcome must carry on where it was, not start again.
     */
    val welcomePage: Int = 0,
    /** Names under the tiles on the screens, and under the bottom-bar icons: two separate choices. */
    val showTileNames: Boolean = true,
    val showDockNames: Boolean = true,
    /** The lightning mark on tiles and bottom-bar icons that have a hold action. */
    val showHoldMark: Boolean = true,
    /** Search: holding the bottom bar's All apps button (locked) opens Search; its own hold action waits. */
    val holdAllAppsToSearch: Boolean = false,
    /** Search: Add folder location(s) and Add search engine(s) are switched on. */
    val searchFolders: Boolean = false,
    val searchEngines: Boolean = false,
    /** Plus only: Search reads every file and folder name on the phone (needs Android's All files access). */
    val allFilesSearch: Boolean = false,
    /** The padlock PIN's hash and salt (base64), or both null when there is no PIN. */
    val pinHash: String? = null,
    val pinSalt: String? = null,
    /** The notes screen shows its notes only after the PIN (needs a PIN). */
    val notesNeedPin: Boolean = false,
    /** Each colour made darker (−) or lighter (+), −50…50; 0 is the colour as picked. */
    val tileShade: Int = 0,
    val iconShade: Int = 0,
    val barShade: Int = 0,
    val clockShade: Int = 0,
    val clockBackShade: Int = 0,
    val highlightShade: Int = 0,
    /** Text mode: one text-only home screen instead of the tiles; its clock; the list on the bottom right; 0 small, 1 medium, 2 large text. */
    val textMode: Boolean = false,
    val textClock: Boolean = true,
    val textListRight: Boolean = false,
    /** Text mode's text: 1 small, 2 medium, 3 large (18, 22, 28 sp). */
    /** Text mode's text size, −2…2 (0 the usual 22 sp): see textStepSp. */
    val textStep: Int = 0,
    /** Text mode's clock: 0 small, 1 medium (as it always was), 2 large; apart from the text size. */
    val textClockSize: Int = 1,
    /** Settings > Help and support > Report bugs: crashes and errors are recorded on the phone. */
    val bugLog: Boolean = false,
    /** The bottom bar is shown, in both modes. */
    val showBar: Boolean = true,
    /** The bottom bar in text mode, apart from [showBar] (icon view). */
    val showBarText: Boolean = true,
    /** Apps shown in the bottom bar: 4 (two each side of All apps) or 6 (three each side). */
    val barSlots: Int = 4,
    /** Text mode's clock: 0 left, 1 centre, 2 right. */
    val textClockAlign: Int = 1,
    /** The names on the tiles and in the bottom bar: 0 is automatic (whatever reads on the tile colour). */
    val tileTextColor: Int = 0,
    val tileTextShade: Int = 0,
    /** Every word on the text screen (names, folder titles, the clock): 0 is automatic (white on dark, near-black on light). */
    val textModeColor: Int = 0,
    val textModeShade: Int = 0,
    /** The names under the tiles on the tile screens: 0 small, 1 medium (as before), 2 large. */
    /** The names and icons on the tiles, −2…2 steps of 10% from the grid's own size (0 as it is). */
    val tileNameStep: Int = 0,
    val tileIconStep: Int = 0,
    /** Quick unlock: the padlock opens after a 1-second hold instead of 3. */
    val quickUnlock: Boolean = false,
    /** How folders look on the screens: 0 as they always did (their apps' icons), 1 a folder icon in [folderColor]. */
    val folderStyle: Int = 0,
    /** Which of the [FOLDER_PICTURES] folder pictures the folder-icon look draws: 1 to 6 (3 is closest to the first one). */
    val folderIcon: Int = 3,
    /** The colour all folders share in the folder-icon look (0 the standard folder yellow), and darker or lighter. */
    val folderColor: Int = 0,
    val folderShade: Int = 0,
    /** The card behind each tile's icon and name; off, they sit on the wallpaper and the smaller grids can be used. */
    val showTiles: Boolean = true,
    /** The shapes of the tile (card), the icon background and the empty place: [SHAPE_DEFAULT] or [SHAPE_CIRCLE]. */
    val iconShape: Int = SHAPE_DEFAULT,
    val tileShape: Int = SHAPE_DEFAULT,
    val slotShape: Int = SHAPE_DEFAULT,
    /** When the last backup was saved (0: never); not itself backed up or restored. */
    val lastBackupMillis: Long = 0,
)

/** Text mode's size as stored (once five steps, 0..4) as one of the three sizes: Small 1, Medium 2, Large 3. */
fun textSizeFromStored(stored: Int): Int = stored.coerceIn(1, 3)

/** Whether the bottom bar shows now: its own switch for icon view and for text mode. */
fun barShown(s: Settings): Boolean = if (s.textMode) s.showBarText else s.showBar

/** Text mode's bar switch: its own once chosen, else what the one bar switch was before there were two. */
fun showBarTextFromOld(stored: Boolean?, showBar: Boolean): Boolean = stored ?: showBar

/** Text mode's old three sizes (0 small, 1 medium, 2 large) on the five-size scale; none saved reads as medium. */
fun textSizeFromOld(old: Int?): Int = when (old) { 0 -> 1; 2 -> 3; else -> 2 }

/** The Text mode switch widget: only the mode changes. */
fun Settings.withTextModeToggled(): Settings = copy(textMode = !textMode)

const val CLOCK_SMALL = 0
const val CLOCK_MEDIUM = 1
const val CLOCK_LARGE = 2

const val DEFAULT_EMERGENCY_COUNTDOWN = 20
val EMERGENCY_COUNTDOWN_CHOICES = listOf(5, 10, 15, 20)

/** The colours the first versions stored as their standard; they now mean "standard". */
private const val OLD_TILE_STANDARD = 0xE61B2430.toInt()
private const val OLD_BAR_STANDARD = 0xF2121A24.toInt()

class SettingsStore(context: Context) {
    // Read through a tolerant view: a value of the wrong type (a damaged or crafted restore) reads as the default
    // instead of stopping the launcher from starting.
    private val prefs = TolerantPrefs(context.getSharedPreferences("settings", Context.MODE_PRIVATE))

    private fun shadeOf(name: String) =
        prefs.getInt(name, 0).coerceIn(com.glimpsemedia.glimpselauncher.model.SHADE_MIN, com.glimpsemedia.glimpselauncher.model.SHADE_MAX)

    fun load() = Settings(
        showClock = prefs.getBoolean("showClock", true),
        showDate = prefs.getBoolean("showDate", true),
        autoLockMinutes = prefs.getInt("autoLockMinutes", 10),
        tileColor = prefs.getInt("tileColor", 0).takeIf { it != OLD_TILE_STANDARD } ?: 0,
        barColor = prefs.getInt("barColor", 0).takeIf { it != OLD_BAR_STANDARD } ?: 0,
        clockColor = prefs.getInt("clockColor", 0),
        darkTheme = prefs.getBoolean("darkTheme", true),
        clockStyle = prefs.getInt("clockStyle", 0),
        clockBackground = prefs.getBoolean("clockBackground", false),
        clockBackColor = prefs.getInt("clockBackColor", 0),
        tileTransparency = prefs.getInt("tileTransparency", 0),
        highlightColor = prefs.getInt("highlightColor", 0),
        barTransparency = prefs.getInt("barTransparency", 0),
        clockTransparency = prefs.getInt("clockTransparency", 0),
        clockBackTransparency = prefs.getInt("clockBackTransparency", 0),
        emergencyEnabled = prefs.getBoolean("emergencyEnabled", false),
        emergencyContact = decodeContacts(prefs.getString("emergencyContacts", null)).firstOrNull(),
        welcomeShown = prefs.getBoolean("welcomeShown", false),
        customLayout = prefs.getBoolean("customLayout", false),
        emergencyCountdown = prefs.getInt("emergencyCountdown", DEFAULT_EMERGENCY_COUNTDOWN)
            .takeIf { it in EMERGENCY_COUNTDOWN_CHOICES } ?: DEFAULT_EMERGENCY_COUNTDOWN,
        clockSize = prefs.getInt("clockSize", CLOCK_LARGE).coerceIn(CLOCK_SMALL, CLOCK_LARGE),
        fullScreen = prefs.getBoolean("fullScreen", false),
        iconColor = prefs.getInt("iconColor", 0),
        iconTransparency = prefs.getInt("iconTransparency", 100),
        welcomePage = prefs.getInt("welcomePage", 0),
        showTileNames = prefs.getBoolean("showTileNames", true),
        showDockNames = prefs.getBoolean("showDockNames", true),
        showHoldMark = prefs.getBoolean("showHoldMark", true),
        holdAllAppsToSearch = prefs.getBoolean("holdAllAppsToSearch", false),
        searchFolders = prefs.getBoolean("searchFolders", false),
        searchEngines = prefs.getBoolean("searchEngines", false),
        allFilesSearch = prefs.getBoolean("allFilesSearch", false),
        pinHash = prefs.getString("pinHash", null),
        pinSalt = prefs.getString("pinSalt", null),
        notesNeedPin = prefs.getBoolean("notesNeedPin", false) && prefs.getString("pinHash", null) != null,
        tileShade = shadeOf("tileShade"),
        iconShade = shadeOf("iconShade"),
        barShade = shadeOf("barShade"),
        clockShade = shadeOf("clockShade"),
        clockBackShade = shadeOf("clockBackShade"),
        highlightShade = shadeOf("highlightShade"),
        textMode = prefs.getBoolean("textMode", false),
        textClock = prefs.getBoolean("textClock", true),
        textListRight = prefs.getBoolean("textListRight", false),
        // Text mode's size is a step now ("textStep"); before, Small, Medium or Large ("textSize5", and "textSize"
        // before that). An old choice keeps its size.
        textStep = if (prefs.contains("textStep")) stepFromStored(prefs.getInt("textStep", 0), TEXT_STEPS)
            else textStepFromOld(textSizeFromStored(if (prefs.contains("textSize5")) prefs.getInt("textSize5", 2)
                else textSizeFromOld(if (prefs.contains("textSize")) prefs.getInt("textSize", 1) else null))),
        textClockSize = prefs.getInt("textClockSize", 1).coerceIn(0, 2),
        bugLog = prefs.getBoolean("bugLog", false),
        showBar = prefs.getBoolean("showBar", true),
        showBarText = showBarTextFromOld(if (prefs.contains("showBarText")) prefs.getBoolean("showBarText", true) else null, prefs.getBoolean("showBar", true)),
        barSlots = prefs.getInt("barSlots", 4).takeIf { it == 4 || it == 6 } ?: 4,
        textClockAlign = prefs.getInt("textClockAlign", 1).coerceIn(0, 2),
        tileTextColor = prefs.getInt("tileTextColor", 0),
        tileTextShade = shadeOf("tileTextShade"),
        textModeColor = prefs.getInt("textModeColor", 0),
        textModeShade = shadeOf("textModeShade"),
        // The name size was Small, Medium or Large ("tileNameSize"); it is a step now.
        tileNameStep = if (prefs.contains("tileNameStep")) stepFromStored(prefs.getInt("tileNameStep", 0), NAME_STEPS)
            else nameStepFromOld(prefs.getInt("tileNameSize", 1).coerceIn(0, 2)),
        tileIconStep = stepFromStored(prefs.getInt("tileIconStep", 0), ICON_STEPS),
        quickUnlock = prefs.getBoolean("quickUnlock", false),
        folderStyle = folderStyleFromStored(prefs.getInt("folderStyle", 0)),
        folderIcon = folderIconFromStored(prefs.getInt("folderIcon", 3)),
        folderColor = prefs.getInt("folderColor", 0),
        folderShade = shadeOf("folderShade"),
        showTiles = prefs.getBoolean("showTiles", true),
        iconShape = shapeFromStored(prefs.getInt("iconShape", 0)),
        tileShape = shapeFromStored(prefs.getInt("tileShape", 0)),
        slotShape = shapeFromStored(prefs.getInt("slotShape", 0)),
        lastBackupMillis = prefs.getLong("lastBackupMillis", 0L),
    )

    fun save(s: Settings) {
        prefs.edit().putBoolean("showClock", s.showClock).putBoolean("showDate", s.showDate)
            .putInt("autoLockMinutes", s.autoLockMinutes).putInt("tileColor", s.tileColor)
            .putInt("barColor", s.barColor).putBoolean("welcomeShown", s.welcomeShown)
            .putBoolean("customLayout", s.customLayout).putInt("clockColor", s.clockColor)
            .putBoolean("darkTheme", s.darkTheme).putInt("clockStyle", s.clockStyle)
            .putBoolean("clockBackground", s.clockBackground).putInt("clockBackColor", s.clockBackColor)
            .putInt("tileTransparency", s.tileTransparency).putInt("highlightColor", s.highlightColor)
            .putInt("barTransparency", s.barTransparency).putInt("clockTransparency", s.clockTransparency)
            .putInt("clockBackTransparency", s.clockBackTransparency)
            .putBoolean("emergencyEnabled", s.emergencyEnabled).putInt("emergencyCountdown", s.emergencyCountdown)
            .putInt("clockSize", s.clockSize).putBoolean("fullScreen", s.fullScreen)
            .putInt("iconColor", s.iconColor).putInt("iconTransparency", s.iconTransparency).putInt("welcomePage", s.welcomePage)
            .putBoolean("showTileNames", s.showTileNames).putBoolean("showDockNames", s.showDockNames).putBoolean("showHoldMark", s.showHoldMark).putBoolean("notesNeedPin", s.notesNeedPin)
            .putBoolean("holdAllAppsToSearch", s.holdAllAppsToSearch).putBoolean("searchFolders", s.searchFolders).putBoolean("searchEngines", s.searchEngines).putBoolean("allFilesSearch", s.allFilesSearch)
            .putInt("tileShade", s.tileShade).putInt("iconShade", s.iconShade).putInt("barShade", s.barShade)
            .putInt("clockShade", s.clockShade).putInt("clockBackShade", s.clockBackShade).putInt("highlightShade", s.highlightShade)
            .putBoolean("textMode", s.textMode).putBoolean("textClock", s.textClock).putBoolean("textListRight", s.textListRight)
            .putInt("textStep", s.textStep).putInt("tileNameStep", s.tileNameStep).putInt("tileIconStep", s.tileIconStep).putBoolean("quickUnlock", s.quickUnlock).putInt("folderStyle", s.folderStyle).putInt("folderIcon", s.folderIcon).putInt("folderColor", s.folderColor).putInt("folderShade", s.folderShade).putBoolean("showTiles", s.showTiles).putInt("iconShape", s.iconShape).putInt("tileShape", s.tileShape).putInt("slotShape", s.slotShape).putLong("lastBackupMillis", s.lastBackupMillis).putBoolean("showBarText", s.showBarText)
            .putInt("textClockSize", s.textClockSize).putBoolean("bugLog", s.bugLog).putBoolean("showBar", s.showBar)
            .putInt("barSlots", s.barSlots).putInt("textClockAlign", s.textClockAlign)
            .putInt("tileTextColor", s.tileTextColor).putInt("tileTextShade", s.tileTextShade)
            .putInt("textModeColor", s.textModeColor).putInt("textModeShade", s.textModeShade)
            .putString("pinHash", s.pinHash).putString("pinSalt", s.pinSalt)
            .putString("emergencyContacts", encodeContacts(listOfNotNull(s.emergencyContact))).apply()
    }
}

/** SharedPreferences reads that fall back to the default when a stored value has another type. */
private class TolerantPrefs(private val p: android.content.SharedPreferences) {
    fun getInt(k: String, d: Int) = runCatching { p.getInt(k, d) }.getOrDefault(d)
    fun getLong(k: String, d: Long) = runCatching { p.getLong(k, d) }.getOrDefault(d)
    fun getBoolean(k: String, d: Boolean) = runCatching { p.getBoolean(k, d) }.getOrDefault(d)
    fun getString(k: String, d: String?): String? = runCatching { p.getString(k, d) }.getOrDefault(d)
    fun contains(k: String) = p.contains(k)
    fun edit(): android.content.SharedPreferences.Editor = p.edit()
}

/** The layout in one small file, written to a temporary file first so a crash cannot leave half of it. */
class LayoutStore(context: Context) {
    private val file = File(context.filesDir, "layout.json")

    fun load(): Layout? = try {
        if (file.exists()) LayoutJson.decode(file.readText()) else null
    } catch (_: Exception) {
        null
    }

    fun save(layout: Layout) {
        val temp = File(file.parentFile, "layout.json.tmp")
        temp.writeText(LayoutJson.encode(layout))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }
}

private fun encodeContacts(list: List<Contact>): String {
    val arr = org.json.JSONArray()
    list.forEach { arr.put(org.json.JSONObject().put("n", it.name).put("p", it.number)) }
    return arr.toString()
}

private fun decodeContacts(text: String?): List<Contact> = try {
    if (text == null) emptyList() else org.json.JSONArray(text).let { a ->
        (0 until a.length()).map { Contact(a.getJSONObject(it).getString("n"), a.getJSONObject(it).getString("p")) }
    }
} catch (_: Exception) {
    emptyList()
}

/** The shapes: the usual rounded one, or a circle (fully rounded ends on a long tile). */
const val SHAPE_DEFAULT = 0
const val SHAPE_CIRCLE = 2

/** A stored shape. The old Square (1) looked like the default and reads as it; so does any damaged or future value. */
fun shapeFromStored(v: Int): Int = if (v == SHAPE_CIRCLE) SHAPE_CIRCLE else SHAPE_DEFAULT

/** The steps each size slider offers: names and text mode's text up to 150%, icons up to 200%. */
val NAME_STEPS = -2..3
val TEXT_STEPS = -2..3
val ICON_STEPS = -2..5

/** A stored size step, kept in its slider's [range] (a damaged or future value never goes past the ends). */
fun stepFromStored(v: Int, range: IntRange): Int = v.coerceIn(range.first, range.last)

/** The old tile name size (0 Small, 1 Medium, 2 Large) as a step: −1, 0, +1. */
fun nameStepFromOld(old: Int): Int = (old - 1).coerceIn(-1, 1)

/** The old text mode size (1 Small 18 sp, 2 Medium 22 sp, 3 Large 28 sp) as the step with the same size: −1, 0, +2. */
fun textStepFromOld(size: Int): Int = when (size) { 1 -> -1; 3 -> 2; else -> 0 }

/** A stored folder look: 0 or 1 (anything else is the usual look, 0). */
fun folderStyleFromStored(v: Int): Int = if (v == 1) 1 else 0

/** How many folder pictures the folder-icon look offers. */
const val FOLDER_PICTURES = 6

/** A stored folder picture: 1 to [FOLDER_PICTURES] (anything else is the default, 3). */
fun folderIconFromStored(v: Int): Int = if (v in 1..FOLDER_PICTURES) v else 3
