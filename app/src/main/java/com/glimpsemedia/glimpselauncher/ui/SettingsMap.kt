package com.glimpsemedia.glimpselauncher.ui

/** One part of a Settings page: a small heading (or none) and the settings under it, by their shown names. */
data class SettingsBlock(val heading: String?, val rows: List<String>)

/** A Settings page. [opensElsewhere]: it has no page of its own and opens another screen (Help, About). */
data class SettingsSection(
    val id: String,
    val title: String,
    val icon: String,
    val blocks: List<SettingsBlock> = emptyList(),
    val opensElsewhere: Boolean = false,
)

/** One entry on the first Settings page, holding its [sections] in order. */
data class SettingsGroup(val id: String, val title: String, val icon: String, val sections: List<String>)

/** A search result: a setting called [label] on the page [section]; [row] is null when the result is the page itself. */
data class SettingsHit(val label: String, val section: String, val row: String?)

/**
 * The shape of Settings: five groups, the pages in each, and the settings on each page. The pages, the search and
 * the tests all read this one list, so they cannot drift apart. Row names are exactly as shown on the page.
 */
object SettingsMap {
    private fun b(heading: String?, vararg rows: String) = SettingsBlock(heading, rows.toList())

    private val all = listOf(
        SettingsSection("screens", "Screens", "screens", listOf(
            b("Layout", "Tiles behind the icons", "Grid", "Free layout: put icons in any square"),
            b("Add to this screen", "Add apps to this screen", "Add widgets", "Add the All apps icon"),
            b("Screen", "Change the wallpaper", "Full screen: hide the top bar"),
        )),
        SettingsSection("bar", "Bottom bar", "bar", listOf(
            b("Show the bar", "Show in icon view", "Show in text mode"),
            b("Apps in the bar", "Apps in the bar", "Add an app to the bottom bar"),
        )),
        SettingsSection("search", "Search", "search", searchSectionRows(com.glimpsemedia.glimpselauncher.BuildConfig.PLUS)),
        SettingsSection("clock", "Clock", "clock", listOf(
            b("Icon view", "Show the time on the first screen", "Show the date under the time", "Clock style", "Clock size", "A background behind the clock"),
            b("Text mode view", "Show the clock", "Size", "Position"),
        )),
        SettingsSection("text", "Text mode", "text", listOf(
            b(null, "Use text mode"),
            b("Look", "Text size", "List side", "Text colour (names, folders and the clock)"),
            b("Apps and folders", "Apps and folders"),
        )),
        SettingsSection("themes", "Themes", "themes", listOf(
            b("Themes", "Themes", "Save current theme"),
            b("Wallpaper", "Theme wallpaper"),
        )),
        SettingsSection("menus", "Menus and highlight", "menus", listOf(
            b(null, "Menus and standard colours", "Highlight colour (selections and touched icons)"),
            b("Text", "Menu text size"),
        )),
        SettingsSection("tiles", "Tiles and icons", "tiles", listOf(
            b("Shapes", "Shapes preview", "Tile shape", "Icon background shape", "Empty place shape"),
            b("Icon pack", "Icon pack"),
            b("Colours", "Tile colour", "Icon background colour", "Text on the tiles and the bar"),
            b("Names", "Names on the tiles", "Names in the bottom bar"),
            b("Sizes", "Name size on the tiles", "Icon size on the tiles"),
            b("Folders", "Folder icon", "Folder picture", "Folder colour"),
            b("Hold actions", "Mark tiles that have a hold action"),
        )),
        SettingsSection("barclock", "Bar and clock colours", "colours", listOf(
            b("Bottom bar", "Colour of the bottom bar"),
            b("Clock", "Colour of the clock and date", "Colour behind the clock"),
        )),
        SettingsSection("lock", "Locking and PIN", "lock", listOf(
            b("Locking", "Lock again after no touching for", "Quick unlock: hold the padlock for 1 second"),
            b("PIN", "Set a PIN", "Change the PIN", "Remove the PIN"),
            b("Notes", "Notes need the PIN"),
        )),
        SettingsSection("emergency", "Emergency button", "sos", listOf(
            b(null, "Emergency button"),
            b("Number to ring", "Number to ring", "Seconds before it rings"),
            b("On the screen", "Add the Emergency icon to the screen"),
        )),
        SettingsSection("badges", "Notification numbers", "badge", listOf(
            b(null, "Show a number on icons with new messages"),
        )),
        SettingsSection("backup", "Backup and restore", "backup", listOf(
            b(null, "Back up to a file", "Restore from a file"),
        )),
        SettingsSection("reset", "Start again", "reset", listOf(
            b(null, "Clear all apps from the screens", "Add all apps to the screens", "Show the welcome again"),
        )),
        SettingsSection("sethome", "Set as home screen", "homeset", listOf(
            b(null, "Set as home screen"),
        )),
        SettingsSection("help", "Help", "help", opensElsewhere = true),
        SettingsSection("about", "About Glimpse Launcher", "info", opensElsewhere = true),
        SettingsSection("bugs", "Report bugs", "bug", listOf(
            b("Get in touch", "Report a bug by email", "Suggest a feature", "Visit the website"),
            b("Bug log", "Bug log", "Email the bug log", "Clear the bug log"),
        )),
    )

    val groups: List<SettingsGroup> = listOf(
        SettingsGroup("home", "Home screen", "grid", listOf("screens", "bar", "search", "clock", "text")),
        SettingsGroup("colours", "Colours and themes", "palette", listOf("themes", "menus", "tiles", "barclock")),
        SettingsGroup("safety", "Safety and privacy", "shield", listOf("lock", "emergency", "badges")),
        SettingsGroup("setup", "Your setup", "box", listOf("backup", "reset", "sethome")),
        SettingsGroup("help", "Help and support", "help", listOf("help", "about", "bugs")),
    )

    /** Every page by id, in group order. */
    val sections: Map<String, SettingsSection> = groups.flatMap { it.sections }.associateWith { id -> all.first { it.id == id } }

    fun groupOf(sectionId: String): SettingsGroup = groups.first { sectionId in it.sections }

    /** Settings whose name holds [query] (case and surrounding spaces ignored), pages before their settings, at most [max]. */
    fun search(query: String, max: Int = 12): List<SettingsHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<SettingsHit>()
        for (sec in sections.values) {
            if (q in sec.title.lowercase()) out += SettingsHit(sec.title, sec.id, null)
            for (block in sec.blocks) for (row in block.rows) {
                // A setting named like its page ("Themes") is already listed as the page.
                if (q in row.lowercase() && row != sec.title) out += SettingsHit(row, sec.id, row)
            }
            if (out.size >= max) break
        }
        return out.take(max)
    }
}

/** Settings › Search's rows; All files and folders is only in Glimpse Launcher Plus. */
fun searchSectionRows(plus: Boolean): List<SettingsBlock> = listOf(
    SettingsBlock(null, listOf("Search icon", "Hold All apps to search")),
    SettingsBlock("What is searched", (if (plus) listOf("All files and folders") else emptyList()) + listOf("Add folder location(s)", "Add search engine(s)")),
)
