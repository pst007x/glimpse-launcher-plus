package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.data.SavedTheme
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.THEME_PRESETS
import com.glimpsemedia.glimpselauncher.data.isTheme
import com.glimpsemedia.glimpselauncher.data.look
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the Settings summaries are made from: the settings, plus the few facts kept elsewhere (the grid lives in the
 * layout, the theme name is worked out with [themeNameOf], notification access is the phone's).
 */
data class SummaryFacts(
    val s: Settings,
    val columns: Int,
    val themeName: String?,
    val isDefaultHome: Boolean,
    val versionName: String,
    val zone: ZoneId = ZoneId.systemDefault(),
    val badges: Boolean = false,
    /** Plus: whether Android allows All files access now (the switch can be on while it is taken away). */
    val allFilesPermitted: Boolean = true,
    /** The grid's rows, with [columns]. */
    val rows: Int = 4,
)

/** The name of the theme the settings match: a ready-made one first, then a saved one; null when neither (custom). */
fun themeNameOf(s: Settings, saved: List<SavedTheme>): String? =
    THEME_PRESETS.firstOrNull { s.isTheme(it) }?.name ?: s.look().let { look -> saved.firstOrNull { it.look == look }?.name }

private const val DOT = " · "
private fun onOff(b: Boolean) = if (b) "on" else "off"
private fun OnOff(b: Boolean) = if (b) "On" else "Off"
private fun size(n: Int) = when (n) { 0 -> "Small"; 2 -> "Large"; else -> "Medium" }
private fun grid(f: SummaryFacts) = "${f.columns}×${f.rows}"
private fun theme(f: SummaryFacts) = f.themeName ?: "Custom"
private fun menus(f: SummaryFacts) = if (f.s.darkTheme) "Dark menus" else "Light menus"
private fun emergencySet(s: Settings) = s.emergencyEnabled && s.emergencyContact?.isUsable == true
private fun backup(f: SummaryFacts): String {
    if (f.s.lastBackupMillis <= 0) return "No backup yet"
    val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK).format(Instant.ofEpochMilli(f.s.lastBackupMillis).atZone(f.zone))
    return "Last backup $day"
}

/** The line under a group on the first Settings page. */
fun groupSummary(groupId: String, f: SummaryFacts): String = when (groupId) {
    "home" -> "${grid(f)} grid${DOT}Clock ${onOff(f.s.showClock)}${DOT}Text mode ${onOff(f.s.textMode)}"
    "colours" -> theme(f) + DOT + menus(f)
    "safety" -> "PIN ${onOff(f.s.pinHash != null)}${DOT}Emergency ${if (emergencySet(f.s)) "set" else "not set"}"
    "setup" -> backup(f)
    "help" -> "Help, About, Report bugs"
    else -> ""
}

/** The line under a page's name on its group page. */
fun sectionSummary(sectionId: String, f: SummaryFacts): String {
    val s = f.s
    return when (sectionId) {
        "screens" -> "${grid(f)} grid${DOT}Free layout ${onOff(s.customLayout)}" + if (s.showTiles) "" else "${DOT}No tiles"
        "search" -> {
            // Plus: All files and folders covers the picked folders (the Play edition never uses it).
            val allFiles = s.allFilesSearch && com.glimpsemedia.glimpselauncher.BuildConfig.PLUS
            val files = when {
                allFiles && !f.allFilesPermitted -> "${DOT}all files (not enabled)"
                allFiles -> "${DOT}all files"
                s.searchFolders -> "${DOT}folders"
                else -> ""
            }
            "Apps, menus and help" + files + (if (s.searchEngines) "${DOT}web" else "")
        }
        "bar" -> "${s.barSlots} apps$DOT${if (s.showBar) "shown" else "hidden"} in icon view"
        "clock" -> if (!s.showClock) "Off in icon view" else {
            val style = when (s.clockStyle) { 1 -> "Side by side"; 2 -> "Round"; else -> "Digital" }
            style + DOT + size(s.clockSize)
        }
        "text" -> "${OnOff(s.textMode)}${DOT}text size ${stepLabel(s.textStep)}"
        "themes" -> theme(f)
        "menus" -> menus(f) + DOT + (if (s.highlightColor == 0) "Standard" else "Custom") + " highlight"
        "tiles" -> (if (!s.showTiles) "No" else if (s.tileColor == 0) "Standard" else "Custom") + " tiles${DOT}names ${stepLabel(s.tileNameStep)}, icons ${stepLabel(s.tileIconStep)}"
        "barclock" -> (if (s.barColor == 0) "Standard" else "Custom") + " bar$DOT" + (if (s.clockColor == 0) "White" else "Custom") + " clock"
        "lock" -> "PIN ${onOff(s.pinHash != null)}$DOT" + if (s.autoLockMinutes == 0) "lock after: Off" else "lock after ${s.autoLockMinutes} min"
        "emergency" -> if (emergencySet(s)) s.emergencyContact!!.let { c -> c.name.ifBlank { c.number } } else "Not set"
        "badges" -> OnOff(f.badges)
        "backup" -> backup(f)
        "reset" -> "Clear, fill or show the welcome"
        "sethome" -> if (f.isDefaultHome) "Glimpse is your home screen" else "Not your home screen yet"
        "help" -> "What everything does"
        "about" -> "Version ${f.versionName}"
        "bugs" -> "Bug log ${onOff(s.bugLog)}"
        else -> ""
    }
}
