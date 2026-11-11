package com.glimpsemedia.glimpselauncher.model

/** The bottom bar holds up to six apps, three each side of All apps; Settings shows four or all six. */
const val DOCK_MAX = 6
const val DOCK_SIDE_MAX = 3

/**
 * What is on a page: an app (its [key] is the flattened component name, "package/Class") or a folder
 * of apps. Folders hold up to [FOLDER_MAX] apps.
 */
sealed interface Item {
    data class App(val key: String) : Item

    /** The All Apps button, placed on a page like an app. */
    data object AllApps : Item

    /** The Search tile: opens the search window. At most one. */
    data object Search : Item

    /** The Emergency button: rings the numbers set up in Settings, one after another. */
    data object Emergency : Item
    data class Folder(val id: String, val name: String, val apps: List<String>) : Item

    /**
     * A widget: [id] is the phone's own number for it, [provider] its component name. It sits on the grid with its
     * top-left square at the slot it is stored in, [cols] squares wide and [rows] tall, and the other squares it
     * covers hold [Covered].
     */
    data class Widget(val id: Int, val provider: String, val cols: Int, val rows: Int) : Item

    /** A square a widget covers: not shown, never dragged. */
    data object Covered : Item
}

/** The usual number of rows: 2×4 and 4×4 have 4. The grid (columns × rows) is a setting kept in the layout. */
const val PAGE_ROWS = 4
/** The columns of the two grids that work with tiles (2×4 and 4×4); the welcome offers these. */
val GRID_COLUMNS = listOf(2, 4)
/** Every grid a screen can have, as columns to rows. */
val GRIDS = listOf(2 to 4, 4 to 4, 4 to 5, 5 to 5, 5 to 6)
/** The smaller grids: only with the tiles switched off. */
val TIGHT_GRIDS = listOf(4 to 5, 5 to 5, 5 to 6)
/** The most squares a screen can have (5×6). */
val MAX_SLOTS = GRIDS.maxOf { (c, r) -> c * r }

/** The grid after the tiles are switched back on: a smaller grid becomes 4×4, the others stay. */
fun gridAfterTilesOn(cols: Int, rows: Int): Pair<Int, Int> = if ((cols to rows) in TIGHT_GRIDS) 4 to 4 else cols to rows

/** The sizes a widget can have on a grid of [columns] by [rows], as (squares across, squares down), smallest first. */
fun widgetSizes(columns: Int, rows: Int = PAGE_ROWS): List<Pair<Int, Int>> =
    if (columns <= 2 && rows == PAGE_ROWS) listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 2 to 3, 2 to 4)
    else (1..columns).flatMap { c -> (1..rows).map { r -> c to r } }.sortedWith(compareBy({ it.first * it.second }, { it.first }))

/** How Size of the widget asks: the short list on the 2-column grid, or squares across and down on its own. */
sealed interface WidgetSizeMenu {
    data class List(val sizes: kotlin.collections.List<Pair<Int, Int>>) : WidgetSizeMenu
    data class AcrossAndDown(val columns: Int, val rows: Int) : WidgetSizeMenu
}

fun widgetSizeMenu(columns: Int, rows: Int): WidgetSizeMenu =
    if (columns <= 2 && rows == PAGE_ROWS) WidgetSizeMenu.List(widgetSizes(columns, rows)) else WidgetSizeMenu.AcrossAndDown(columns, rows)

/** The size after picking [across] squares across, keeping the widget's height. */
fun sizeWithAcross(widget: Item.Widget, across: Int): Pair<Int, Int> = across to widget.rows

/** The size after picking [down] squares down, keeping the widget's width. */
fun sizeWithDown(widget: Item.Widget, down: Int): Pair<Int, Int> = widget.cols to down

/**
 * The widget size that fits the grid for a widget that wants at least [minWidthDp] by [minHeightDp]: the smallest
 * allowed size at least that big. [cellWidthDp] and [cellHeightDp] are the size of one square.
 */
fun snapWidgetSize(minWidthDp: Int, minHeightDp: Int, cellWidthDp: Int, cellHeightDp: Int, columns: Int = 2, gridRows: Int = PAGE_ROWS): Pair<Int, Int> {
    val cols = ((minWidthDp + cellWidthDp - 1) / cellWidthDp.coerceAtLeast(1)).coerceIn(1, columns)
    val rows = ((minHeightDp + cellHeightDp - 1) / cellHeightDp.coerceAtLeast(1)).coerceIn(1, gridRows)
    val sizes = widgetSizes(columns, gridRows)
    return sizes.firstOrNull { (c, r) -> c >= cols && r >= rows } ?: sizes.last()
}

/**
 * One screen of [columns] across and [rows] down. [items] is by slot, row by row; a null is an empty slot, so with the
 * free layout switched on an icon can sit in any slot. Without it the layout keeps every page packed to the
 * top (see [Layout.compacted]).
 */
data class Page(val items: List<Item?> = emptyList(), val columns: Int = 2, val rows: Int = PAGE_ROWS) {
    init {
        require((columns to rows) in GRIDS) { "A screen is 2×4, 4×4, 4×5, 5×5 or 5×6" }
        require(items.size <= slots) { "A page holds at most $slots items" }
    }

    /** How many squares this screen has. */
    val slots: Int get() = columns * rows

    val count: Int get() = items.count { it != null }
    val isFull: Boolean get() = count >= slots
    val isEmpty: Boolean get() = count == 0
    /** A widget or a big item is here (they cover squares): the page keeps its places and is never packed. */
    val hasWidgets: Boolean get() = items.any { it is Item.Widget || it == Item.Covered }

    /** The slot where a widget [cols] by [rows] fits (all the squares it needs are empty), or null. */
    fun widgetSpot(cols: Int, rows: Int): Int? = (0 until slots).firstOrNull { anchor ->
        val col = anchor % columns
        val row = anchor / columns
        col + cols <= columns && row + rows <= this.rows &&
            (0 until rows).all { r -> (0 until cols).all { c -> items.getOrNull((row + r) * columns + col + c) == null } }
    }

    /** The widget put at [anchor], with the squares it covers marked. */
    fun withWidget(anchor: Int, widget: Item.Widget): Page {
        var p = this
        val col = anchor % columns
        val row = anchor / columns
        for (r in 0 until widget.rows) for (c in 0 until widget.cols) {
            p = p.set((row + r) * columns + col + c, if (r == 0 && c == 0) widget else Item.Covered)
        }
        return p
    }

    /** Whether a widget [cols] by [rows] fits with its top-left square at [anchor]. */
    fun widgetSpotAt(anchor: Int, cols: Int, rows: Int): Boolean {
        val col = anchor % columns
        val row = anchor / columns
        return col + cols <= columns && row + rows <= this.rows &&
            (0 until rows).all { r -> (0 until cols).all { c -> items.getOrNull((row + r) * columns + col + c) == null } }
    }

    /** The page without the widget at [anchor] and the squares it covered. */
    fun withoutWidget(anchor: Int): Page {
        val widget = items.getOrNull(anchor) as? Item.Widget ?: return this
        var p = this
        val col = anchor % columns
        val row = anchor / columns
        for (r in 0 until widget.rows) for (c in 0 until widget.cols) p = p.set((row + r) * columns + col + c, null)
        return p
    }

    /** Puts [item] in [slot] (null empties it), padding with empty slots and dropping empty ones at the end. */
    fun set(slot: Int, item: Item?): Page {
        val list = items.toMutableList()
        while (list.size <= slot) list.add(null)
        list[slot] = item
        while (list.isNotEmpty() && list.last() == null) list.removeAt(list.size - 1)
        return Page(list, columns, rows)
    }

    /** Puts [item] in the first empty slot. Null when the page is full. */
    fun withItem(item: Item): Page? {
        if (isFull) return null
        val gap = items.indexOfFirst { it == null }
        return if (gap >= 0) set(gap, item) else Page(items + item, columns, rows)
    }

    fun packed(): Page = Page(items.filterNotNull(), columns, rows)

    /** Drops empty slots at the end only. */
    fun packedEnd(): Page = items.toMutableList().let { l -> while (l.isNotEmpty() && l.last() == null) l.removeAt(l.size - 1); Page(l, columns, rows) }
}

/** The name a tile's own colour is kept under: the app's key, "f:" and the folder's id, or "all". */
val Item.colorKey: String
    get() = when (this) {
        is Item.App -> key
        is Item.Folder -> "f:$id"
        Item.AllApps -> "all"
        Item.Search -> "search"
        Item.Emergency -> "emergency"
        is Item.Widget -> "widget:$id"
        Item.Covered -> "covered"
    }

/**
 * The whole home screen: the pages (at least one), the bottom-bar apps, and the tiles given a colour of their
 * own ([tileColors]: [colorKey] to an ARGB number; a tile not listed has the standard colour).
 */
data class Layout(
    val pages: List<Page>,
    val dock: List<String>,
    val tileColors: Map<String, Int> = emptyMap(),
    /** A tile's own transparency in percent ([colorKey] to 0, 25, 50, 75 or 100); a tile not listed follows the setting. */
    val tileTransparency: Map<String, Int> = emptyMap(),
    /** The small square behind a tile's icon: its own colour and transparency, kept apart from the tile's. */
    val iconColors: Map<String, Int> = emptyMap(),
    val iconTransparency: Map<String, Int> = emptyMap(),
    /** The tags for filtering All apps, in the order they were made. */
    val tags: List<AppTag> = emptyList(),
    /** The grid's columns, 2, 4 or 5 (with [rows], one of [GRIDS]); every page has the same. */
    val columns: Int = 2,
    /** Each tile's hold action, under its [holdKey] (screen tiles) or [dockHoldKey] (bottom-bar icons). */
    val holdActions: Map<String, HoldAction> = emptyMap(),
    /** A tile's own colour and its own icon square made darker (−) or lighter (+), −50…50, under its [colorKey]. */
    val tileShades: Map<String, Int> = emptyMap(),
    val iconShades: Map<String, Int> = emptyMap(),
    /** How many of the [dock] apps (counted from the start) sit left of All apps; the rest sit right of it. */
    val dockLeft: Int = maxOf(dock.size - DOCK_SIDE_MAX, minOf(2, dock.size)),
    /** A tile's own name colour, and how much darker (−) or lighter (+), under its [colorKey]; not listed is automatic. */
    val textColors: Map<String, Int> = emptyMap(),
    val textShades: Map<String, Int> = emptyMap(),
    /** The grid's rows (4, 5 or 6), with [columns]: one of [GRIDS]. */
    val rows: Int = PAGE_ROWS,
    /** A folder's own colour for the folder-icon look, under its [colorKey]; a folder not listed has the shared colour. */
    val folderColors: Map<String, Int> = emptyMap(),
    /** A folder's badge on the folder-icon look (a [TextIcon] name), under its [colorKey]; most folders have none. */
    val folderBadges: Map<String, String> = emptyMap(),
    /** The items that take 2 × 2 squares, by [colorKey] (see BigItems.kt); most layouts have none. */
    val bigItems: Set<String> = emptySet(),
) {
    init {
        require(pages.isNotEmpty()) { "There is always at least one page" }
        require(pages.all { it.columns == columns && it.rows == rows }) { "Every page has the layout's $columns × $rows grid" }
        require(dock.size <= DOCK_MAX) { "The bottom bar holds at most $DOCK_MAX apps" }
        require(dockLeft in 0..dock.size && dockLeft <= DOCK_SIDE_MAX && dock.size - dockLeft <= DOCK_SIDE_MAX) { "At most $DOCK_SIDE_MAX apps each side of All apps" }
    }

    /** Every app placed anywhere (pages, folders, bottom bar). */
    /** The apps on the screens only: one in the bottom bar may be on a screen too, and the other way round. */
    fun screenKeys(): Set<String> = pages.flatMap { pg -> pg.items.flatMap { when (it) { is Item.App -> listOf(it.key); is Item.Folder -> it.apps; else -> emptyList() } } }.toSet()

    fun placedKeys(): Set<String> = buildSet {
        addAll(dock)
        pages.forEach { page ->
            page.items.forEach { item ->
                when (item) {
                    is Item.App -> add(item.key)
                    is Item.Folder -> addAll(item.apps)
                    Item.AllApps, Item.Search, Item.Emergency, is Item.Widget, Item.Covered, null -> Unit
                }
            }
        }
    }

    /** The same layout without apps that are no longer installed; empty folders go too. */
    fun withoutMissing(installed: Set<String>): Layout {
        // A removed app leaves its slot empty; a layout that is not free is packed again by the caller.
        val pages = pages.map { page ->
            Page(columns = page.columns, rows = page.rows, items = page.items.map { item ->
                when (item) {
                    is Item.App -> item.takeIf { it.key in installed }
                    is Item.Folder -> item.copy(apps = item.apps.filter { it in installed })
                        .takeIf { it.apps.isNotEmpty() }
                    Item.AllApps, Item.Search, Item.Emergency, is Item.Widget, Item.Covered -> item
                    null -> null
                }
            }).packedEnd()
        }
        // Only the missing apps go: the tiles' own colours and the tags stay.
        return copy(
            pages = pages, dock = dock.filter { it in installed }, dockLeft = dock.take(dockLeft).count { it in installed },
            tags = tags.map { it.copy(apps = it.apps.filter { k -> k in installed }.toSet()) },
            // An action that opens an app goes with that app.
            holdActions = holdActions.filterValues { it !is HoldAction.OpenApp || it.key in installed },
        )
    }

    /** Gives a tile its hold action; null removes it. */
    fun withHoldAction(key: String, action: HoldAction?): Layout =
        copy(holdActions = if (action == null) holdActions - key else holdActions + (key to action))

    /** Without the hold actions of tiles that are no longer on a page or in the bottom bar. */
    fun withoutStrayHolds(): Layout {
        val keys = pages.flatMap { it.items }.mapNotNull { it?.holdKey }.toSet() + dock.map { dockHoldKey(it) } + dockHoldKey(null)
        val kept = holdActions.filterKeys { it in keys }
        return if (kept == holdActions) this else copy(holdActions = kept)
    }

    /** Gives the tile its own colour; 0 goes back to the standard colour. */
    fun withTileColor(item: Item, argb: Int): Layout =
        if (argb == 0) copy(tileColors = tileColors - item.colorKey, tileShades = tileShades - item.colorKey)
        else copy(tileColors = tileColors + (item.colorKey to argb))

    /** Makes the tile's own colour darker or lighter; 0 removes the change. */
    fun withTileShade(item: Item, percent: Int): Layout =
        copy(tileShades = if (percent == 0) tileShades - item.colorKey else tileShades + (item.colorKey to percent.coerceIn(SHADE_MIN, SHADE_MAX)))

    /** Makes the tile's own icon square darker or lighter; 0 removes the change. */
    fun withIconShade(item: Item, percent: Int): Layout =
        copy(iconShades = if (percent == 0) iconShades - item.colorKey else iconShades + (item.colorKey to percent.coerceIn(SHADE_MIN, SHADE_MAX)))

    /** Gives the tile its own transparency; null goes back to following the setting. */
    fun withTileTransparency(item: Item, percent: Int?): Layout =
        copy(tileTransparency = if (percent == null) tileTransparency - item.colorKey else tileTransparency + (item.colorKey to percent))

    /** Gives the tile's icon square its own colour; 0 goes back to the setting. */
    fun withIconColor(item: Item, argb: Int): Layout =
        if (argb == 0) copy(iconColors = iconColors - item.colorKey, iconShades = iconShades - item.colorKey)
        else copy(iconColors = iconColors + (item.colorKey to argb))

    /** Gives a folder its own colour (the folder-icon look); 0 goes back to the colour all folders share. */
    fun withFolderColor(item: Item, argb: Int): Layout =
        if (argb == 0) copy(folderColors = folderColors - item.colorKey) else copy(folderColors = folderColors + (item.colorKey to argb))

    /** Gives a folder a badge (the folder-icon look); null takes it off. */
    fun withFolderBadge(item: Item, icon: TextIcon?): Layout =
        if (icon == null) copy(folderBadges = folderBadges - item.colorKey) else copy(folderBadges = folderBadges + (item.colorKey to icon.name))

    /** A folder's mono badge, or null when it has none (or a colour one). */
    fun badgeOf(item: Item): TextIcon? = monoBadgeIn(folderBadges, item.colorKey)

    /** Gives a folder a colour badge (the Glimpse Colour drawing for that subject), saved as "color:<subject>". */
    fun withColourBadge(item: Item, icon: TextIcon): Layout = copy(folderBadges = folderBadges + (item.colorKey to COLOUR_BADGE + icon.name))

    /** A folder's colour badge subject, or null. */
    fun colourBadgeOf(item: Item): TextIcon? = colourBadgeIn(folderBadges, item.colorKey)

    /** Either kind of badge (for the menu's Add / Change line). */
    fun anyBadgeOf(item: Item): TextIcon? = badgeOf(item) ?: colourBadgeOf(item)

    /** Gives the tile's name its own colour; 0 goes back to automatic (and drops its darker/lighter change). */
    fun withTextColor(item: Item, argb: Int): Layout =
        if (argb == 0) copy(textColors = textColors - item.colorKey, textShades = textShades - item.colorKey)
        else copy(textColors = textColors + (item.colorKey to argb))

    /** Makes the tile's own name colour darker or lighter; 0 removes the change. */
    fun withTextShade(item: Item, percent: Int): Layout =
        copy(textShades = if (percent == 0) textShades - item.colorKey else textShades + (item.colorKey to percent.coerceIn(SHADE_MIN, SHADE_MAX)))

    /** Gives the tile's icon square its own transparency; null goes back to the setting. */
    fun withIconTransparency(item: Item, percent: Int?): Layout =
        copy(iconTransparency = if (percent == null) iconTransparency - item.colorKey else iconTransparency + (item.colorKey to percent))

    /** Without the colours and transparencies of tiles that are no longer on a page. */
    fun withoutStrayColors(): Layout {
        val onPages = pages.flatMap { it.items }.filterNotNull().map { it.colorKey }.toSet()
        val colors = tileColors.filterKeys { it in onPages }
        val see = tileTransparency.filterKeys { it in onPages }
        val icons = iconColors.filterKeys { it in onPages }
        val iconSee = iconTransparency.filterKeys { it in onPages }
        val shades = tileShades.filterKeys { it in onPages }
        val iconShaded = iconShades.filterKeys { it in onPages }
        val texts = textColors.filterKeys { it in onPages }
        val textShaded = textShades.filterKeys { it in onPages }
        val folders = folderColors.filterKeys { it in onPages }
        val badges = folderBadges.filterKeys { it in onPages }
        return if (colors == tileColors && see == tileTransparency && icons == iconColors && iconSee == iconTransparency &&
            shades == tileShades && iconShaded == iconShades && texts == textColors && textShaded == textShades && folders == folderColors && badges == folderBadges) this
        else copy(tileColors = colors, tileTransparency = see, iconColors = icons, iconTransparency = iconSee, tileShades = shades, iconShades = iconShaded,
            textColors = texts, textShades = textShaded, folderColors = folders, folderBadges = badges)
    }

    /** How many squares each screen has. */
    val slotsPerPage: Int get() = columns * rows

    /**
     * The same things on a grid of [n] columns. Icons and folders keep their order and flow onto as many screens
     * as they need (free-layout gaps close up). Each widget keeps its size on screen (its width in squares doubles
     * going to 4 columns and halves, rounded up, going to 2) and goes back on the screen it shared with the items
     * around it, at the nearest free place, smaller if it must.
     */
    fun withColumns(n: Int): Layout = withGrid(n, PAGE_ROWS)

    /**
     * The same things on a grid of [n] columns by [r] rows (one of [GRIDS]), as [withColumns] describes: widgets
     * grow or shrink in width with the columns and in height with the rows (rounded down growing, up shrinking).
     */
    fun withGrid(n: Int, r: Int): Layout {
        require((n to r) in GRIDS) { "Not one of the grids: $n × $r" }
        if (n == columns && r == rows) return this
        // Each old page: its items (no gaps, no covered squares) and its widgets.
        data class Old(val items: List<Item>, val widgets: List<Pair<Int, Item.Widget>>)
        val old = pages.map { p ->
            Old(
                p.items.filterNotNull().filter { it !is Item.Widget && it != Item.Covered },
                p.items.withIndex().mapNotNull { (i, it) -> (it as? Item.Widget)?.let { w -> i to w } },
            )
        }
        val scale = { w: Item.Widget ->
            val cols = if (n >= columns) w.cols * n / columns else (w.cols * n + columns - 1) / columns
            val down = if (r >= rows) w.rows * r / rows else (w.rows * r + rows - 1) / rows
            w.copy(cols = cols.coerceIn(1, n), rows = down.coerceIn(1, r))
        }
        val slots = n * r
        // A big item takes the first free 2 × 2 block, or one square if there is none (it is made normal afterwards).
        fun fitted(page: Page, item: Item): Page? =
            (if (item.colorKey in bigItems) page.withBigItem(item) else null) ?: page.withItem(item)
        val out = mutableListOf<Page>()
        var current = Page(columns = n, rows = r)
        fun startPage() { if (!current.isEmpty) out += current; current = Page(columns = n, rows = r) }
        for (o in old) {
            if (o.widgets.isNotEmpty()) {
                // A page with widgets stays a page of its own: widgets first, then its items around them.
                startPage()
                var page = Page(columns = n, rows = r)
                // A widget with no room left (narrower grid, page already full) gets a page of its own after this one.
                val homeless = mutableListOf<Item.Widget>()
                for ((anchor, w) in o.widgets) {
                    val scaled = scale(w)
                    val row = ((anchor / columns) * r / rows).coerceAtMost(r - scaled.rows)
                    val col = (anchor % columns) * n / columns
                    page = placeWidgetNear(page, scaled, row * n + col.coerceAtMost(n - scaled.cols)) ?: page.also { homeless += scaled }
                }
                val leftover = mutableListOf<Item>()
                for (item in o.items) page = fitted(page, item) ?: page.also { leftover += item }
                out += page
                for (w in homeless) {
                    out += placeWidgetNear(Page(columns = n, rows = r), w, 0) ?: error("a widget always fits an empty page")
                }
                for (item in leftover) { current = fitted(current, item) ?: run { startPage(); fitted(current, item)!! } }
            } else {
                for (item in o.items) {
                    current = fitted(current, item) ?: run { out += current; fitted(Page(columns = n, rows = r), item)!! }
                }
            }
        }
        if (!current.isEmpty || out.isEmpty()) out += current
        return copy(pages = out.map { if (it.hasWidgets) it else it.packed() }, columns = n, rows = r)
    }

    /** [widget] at [wanted] if it fits there, else the nearest free place, else the largest smaller size that fits; null if none. */
    private fun placeWidgetNear(page: Page, widget: Item.Widget, wanted: Int): Page? {
        val n = page.columns
        val sizes = listOf(widget.cols to widget.rows) + widgetSizes(n, page.rows)
            .filter { (c, r) -> c <= widget.cols && r <= widget.rows && (c to r) != (widget.cols to widget.rows) }
            .sortedByDescending { (c, r) -> c * r }
        for ((c, r) in sizes) {
            val anchor = (0 until page.slots).filter { page.widgetSpotAt(it, c, r) }
                .minByOrNull { Math.abs(it % n - wanted % n) + Math.abs(it / n - wanted / n) } ?: continue
            return page.withWidget(anchor, widget.copy(cols = c, rows = r))
        }
        return null
    }

    /** The names of the tags [key] has. */
    fun tagsOf(key: String): Set<String> = tags.filter { key in it.apps }.map { it.name }.toSet()

    /** The apps with the tag [name]. */
    fun appsTagged(name: String): Set<String> = tags.firstOrNull { it.name == name }?.apps.orEmpty()

    private fun tagNamed(name: String) = tags.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    /** A new, empty tag; nothing when the name is blank or already a tag (ignoring case). */
    fun withNewTag(name: String): Layout {
        val clean = cleanTagName(name) ?: return this
        return if (tagNamed(clean) != null) this else copy(tags = tags + AppTag(clean, emptySet()))
    }

    /** Gives [key] the tag [name] (making the tag if needed), or takes it away. */
    fun withAppTagged(key: String, name: String, on: Boolean): Layout {
        val base = if (on) withNewTag(name) else this
        val tag = base.tagNamed(name) ?: return this
        return base.copy(tags = base.tags.map { if (it != tag) it else it.copy(apps = if (on) it.apps + key else it.apps - key) })
    }

    /** Renames a tag; nothing when the new name is blank or another tag already has it. */
    fun withTagRenamed(from: String, to: String): Layout {
        val clean = cleanTagName(to) ?: return this
        val other = tagNamed(clean)
        if (other != null && other.name != from) return this
        return copy(tags = tags.map { if (it.name == from) it.copy(name = clean) else it })
    }

    /** Deletes a tag; its apps stay where they are. */
    fun withoutTag(name: String): Layout = copy(tags = tags.filter { it.name != name })

    /** Whether the tag [name] can move [by] places (−1 up, +1 down) in the list. */
    fun canMoveTag(name: String, by: Int): Boolean {
        val i = tags.indexOfFirst { it.name == name }
        return i >= 0 && i + by in tags.indices
    }

    /** The tag [name] moved [by] places (−1 up, +1 down); the same layout at the ends or for a tag not there. */
    fun withTagMoved(name: String, by: Int): Layout {
        if (!canMoveTag(name, by)) return this
        val i = tags.indexOfFirst { it.name == name }
        return copy(tags = tags.toMutableList().apply { add(i + by, removeAt(i)) })
    }

    /** Start again: [fresh] screens and bar, keeping the tags (they sort All apps, not the screens). */
    fun startedAgainAs(fresh: Layout): Layout = fresh.copy(tags = tags)

    /** Every page packed to the top: the layout when the free layout is off. */
    fun compacted(): Layout = copy(pages = pages.map { if (it.hasWidgets) it else it.packed() })

    /** Every widget on the pages. */
    fun widgets(): List<Item.Widget> = pages.flatMap { p -> p.items.filterIsInstance<Item.Widget>() }
}

// ---- Editing. Every function returns a new layout; the old one is never changed. ----

val Layout.hasAllAppsIcon: Boolean get() = pages.any { Item.AllApps in it.items }
val Layout.hasEmergencyIcon: Boolean get() = pages.any { Item.Emergency in it.items }
val Layout.hasSearchIcon: Boolean get() = pages.any { Item.Search in it.items }

/** Puts [item] on the first page with room (a new page if all are full), unless it is already placed. */
fun Layout.add(item: Item): Layout {
    if (item is Item.App && item.key in screenKeys()) return this
    if (item == Item.AllApps && hasAllAppsIcon) return this
    if (item == Item.Emergency && hasEmergencyIcon) return this
    if (item == Item.Search && hasSearchIcon) return this
    val room = pages.indexOfFirst { !it.isFull }
    return if (room >= 0) {
        copy(pages = pages.mapIndexed { i, p -> if (i == room) p.withItem(item)!! else p })
    } else {
        copy(pages = pages + Page(listOf(item), columns, rows))
    }
}

/** Puts [item] on page [pageIndex] at the end; null when that page is full. */
fun Layout.addToPage(pageIndex: Int, item: Item): Layout? {
    if (item is Item.App && item.key in screenKeys()) return this
    if (item == Item.AllApps && hasAllAppsIcon) return this
    if (item == Item.Emergency && hasEmergencyIcon) return this
    if (item == Item.Search && hasSearchIcon) return this
    val page = pages.getOrNull(pageIndex)?.withItem(item) ?: return null
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) page else p })
}

/** Like [addToPage] but into the square [slot] when it is empty (the free layout); otherwise the first empty square. */
fun Layout.addToPageAt(pageIndex: Int, slot: Int, item: Item): Layout? {
    val page = pages.getOrNull(pageIndex) ?: return null
    if (slot !in 0 until slotsPerPage || page.items.getOrNull(slot) != null) return addToPage(pageIndex, item)
    if (item is Item.App && item.key in screenKeys()) return this
    if (item == Item.AllApps && hasAllAppsIcon) return this
    if (item == Item.Emergency && hasEmergencyIcon) return this
    if (item == Item.Search && hasSearchIcon) return this
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) p.set(slot, item) else p })
}

/** Takes an item off a page. With [keepGaps] its slot stays empty (free layout); otherwise the rest close up. */
fun Layout.removeFromPage(pageIndex: Int, itemIndex: Int, keepGaps: Boolean = false): Layout {
    val page = pages.getOrNull(pageIndex) ?: return this
    if (itemIndex !in page.items.indices) return this
    return copy(pages = pages.mapIndexed { i, p ->
        when {
            i != pageIndex -> p
            keepGaps || p.hasWidgets -> p.set(itemIndex, null)  // a page with widgets never closes up: the widgets stay where they are
            else -> Page(p.items.filterIndexed { j, _ -> j != itemIndex }, p.columns, p.rows)
        }
    })
}

/**
 * Free layout: puts the item at (fromPage, fromIndex) in exactly slot [toSlot] of [toPage]. An occupied slot
 * swaps places with it. Null when the slot is not on the page.
 */
fun Layout.placeItem(fromPage: Int, fromIndex: Int, toPage: Int, toSlot: Int): Layout? {
    val item = itemAt(fromPage, fromIndex) ?: return null
    if (item is Item.Widget || item is Item.Covered) return null
    // A big item is moved with moveBig, and one in the target square is not swapped into a single square.
    if (item.colorKey in bigItems) return null
    if (toSlot !in 0 until slotsPerPage || toPage !in pages.indices) return null
    if (fromPage == toPage && fromIndex == toSlot) return this
    val target = itemAt(toPage, toSlot)
    if (target is Item.Widget || target is Item.Covered) return null
    if (target != null && target.colorKey in bigItems) return null
    val list = pages.toMutableList()
    list[toPage] = list[toPage].set(toSlot, item)
    list[fromPage] = list[fromPage].set(fromIndex, target)
    return copy(pages = list)
}

/**
 * Moves an item to [toIndex] on page [toPage] (the others shift along; [toIndex] past the end means last).
 * Returns null when the move is not allowed: a different page that is already full.
 */
fun Layout.moveItem(fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int): Layout? {
    val source = pages.getOrNull(fromPage)?.items?.getOrNull(fromIndex) ?: return null
    if (toPage !in pages.indices) return null
    if (source is Item.Widget || source is Item.Covered) return null
    if (fromPage != toPage && pages[toPage].isFull) return null
    val without = removeFromPage(fromPage, fromIndex)
    if (without.pages[toPage].hasWidgets) {
        val placed = without.pages[toPage].withItem(source) ?: return null
        return without.copy(pages = without.pages.mapIndexed { i, p -> if (i == toPage) placed else p })
    }
    val target = without.pages[toPage].packed().items
    val at = toIndex.coerceIn(0, target.size)
    return without.copy(pages = without.pages.mapIndexed { i, p ->
        if (i == toPage) Page(target.toMutableList().apply { add(at, source) }, columns, rows) else p
    })
}

fun Layout.addPage(): Layout = copy(pages = pages + Page(columns = columns, rows = rows))

/** Puts the widget on page [pageIndex] if it fits there; null when it does not. */
fun Layout.addWidgetToPage(pageIndex: Int, widget: Item.Widget): Layout? {
    val page = pages.getOrNull(pageIndex) ?: return null
    val spot = page.widgetSpot(widget.cols, widget.rows) ?: return null
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) p.withWidget(spot, widget) else p })
}

/** A widget put with its top-left at [slot] of [pageIndex] when every square it needs is empty (the free layout); otherwise as [addWidget]. */
fun Layout.addWidgetAt(widget: Item.Widget, pageIndex: Int, slot: Int): Layout {
    val page = pages.getOrNull(pageIndex)
    if (page != null && slot in 0 until slotsPerPage) {
        val col = slot % columns
        val row = slot / columns
        val fits = col + widget.cols <= columns && row + widget.rows <= rows &&
            (0 until widget.rows).all { r -> (0 until widget.cols).all { c -> page.items.getOrNull((row + r) * columns + col + c) == null } }
        if (fits) return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) p.withWidget(slot, widget) else p })
    }
    return addWidget(widget, pageIndex)
}

/** Puts the widget on page [preferred] if it fits, else on the first page where it fits, else on a new page. */
fun Layout.addWidget(widget: Item.Widget, preferred: Int = 0): Layout {
    // The empty screen waiting after the last one: a widget put there starts a new screen
    if (preferred == pages.size) return copy(pages = pages + Page(columns = columns, rows = rows).withWidget(0, widget))
    addWidgetToPage(preferred, widget)?.let { return it }
    for (i in pages.indices) addWidgetToPage(i, widget)?.let { return it }
    return copy(pages = pages + Page(columns = columns, rows = rows).withWidget(0, widget))
}

/** Takes the widget at [anchor] off page [pageIndex]. */
fun Layout.removeWidget(pageIndex: Int, anchor: Int): Layout =
    copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) p.withoutWidget(anchor) else p })

/**
 * Moves the widget at ([fromPage], [fromAnchor]) so its top-left square is [toAnchor] of [toPage]. Null when the
 * squares it needs there are not all empty.
 */
fun Layout.moveWidget(fromPage: Int, fromAnchor: Int, toPage: Int, toAnchor: Int): Layout? {
    val widget = pages.getOrNull(fromPage)?.items?.getOrNull(fromAnchor) as? Item.Widget ?: return null
    if (toPage !in pages.indices) return null
    val emptied = pages[fromPage].withoutWidget(fromAnchor)
    val target = if (toPage == fromPage) emptied else pages[toPage]
    if (!target.widgetSpotAt(toAnchor, widget.cols, widget.rows)) return null
    val placed = target.withWidget(toAnchor, widget)
    return copy(pages = pages.mapIndexed { i, p ->
        when (i) {
            toPage -> placed
            fromPage -> emptied
            else -> p
        }
    })
}

/**
 * Like [moveWidget], but fits the widget to what is there instead of refusing: the position is moved to keep it on
 * the grid, then to the nearest free place of the same size, and if there is none the widget takes the largest
 * smaller size that fits. Null only when not even the smallest size fits on that page.
 */
fun Layout.moveWidgetFitting(fromPage: Int, fromAnchor: Int, toPage: Int, wantedAnchor: Int): Layout? {
    val widget = pages.getOrNull(fromPage)?.items?.getOrNull(fromAnchor) as? Item.Widget ?: return null
    if (toPage !in pages.indices) return null
    val emptied = pages[fromPage].withoutWidget(fromAnchor)
    val target = if (toPage == fromPage) emptied else pages[toPage]
    val wantCol = wantedAnchor % columns
    val wantRow = wantedAnchor / columns
    // The same size first, then smaller and smaller sizes (largest first)
    val sizes = listOf(widget.cols to widget.rows) +
        widgetSizes(columns, rows).filter { (c, r) -> c * r < widget.cols * widget.rows || (c <= widget.cols && r <= widget.rows && c * r <= widget.cols * widget.rows && (c to r) != (widget.cols to widget.rows)) }
            .sortedByDescending { (c, r) -> c * r }
    for ((c, r) in sizes) {
        val col = wantCol.coerceIn(0, columns - c)
        val row = wantRow.coerceIn(0, rows - r)
        val anchor = (0 until slotsPerPage).filter { target.widgetSpotAt(it, c, r) }
            .minByOrNull { Math.abs(it % columns - col) + Math.abs(it / columns - row) } ?: continue
        val placed = target.withWidget(anchor, widget.copy(cols = c, rows = r))
        return copy(pages = pages.mapIndexed { i, p ->
            when (i) {
                toPage -> placed
                fromPage -> emptied
                else -> p
            }
        })
    }
    return null
}

/**
 * Gives the widget at [anchor] a new size, in the same place if it fits there, else in the first place on the page
 * where it fits. Null when it fits nowhere on the page.
 */
fun Layout.resizeWidget(pageIndex: Int, anchor: Int, cols: Int, rows: Int): Layout? {
    val page = pages.getOrNull(pageIndex) ?: return null
    val old = page.items.getOrNull(anchor) as? Item.Widget ?: return null
    val without = page.withoutWidget(anchor)
    val resized = old.copy(cols = cols, rows = rows)
    val spot = (if (without.widgetSpotAt(anchor, cols, rows)) anchor else without.widgetSpot(cols, rows)) ?: return null
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) without.withWidget(spot, resized) else p })
}

/**
 * The pages without empty ones at the end: the last screen goes away by itself once its last item is taken off, and
 * a new one is made by dragging something onto the empty screen after the last. An empty screen with others after it
 * stays (it was left behind on purpose); Remove this empty screen takes it away. The first page always stays.
 */
fun Layout.withoutEmptyExtraPages(): Layout {
    var keep = pages.size
    while (keep > 1 && pages[keep - 1].isEmpty) keep--
    return if (keep == pages.size) this else copy(pages = pages.take(keep))
}

/** Removes an empty page; the last page can never be removed. */
fun Layout.removePageIfEmpty(pageIndex: Int): Layout {
    val page = pages.getOrNull(pageIndex) ?: return this
    return if (page.isEmpty && pages.size > 1) copy(pages = pages.filterIndexed { i, _ -> i != pageIndex }) else this
}

fun Layout.removeFromDock(key: String): Layout {
    val i = dock.indexOf(key)
    return if (i < 0) this else copy(dock = dock - key, dockLeft = if (i < dockLeft) dockLeft - 1 else dockLeft)
}

/** The mono badge saved for [key] in a folder-badge map, if it has one (see [Layout.folderBadges]). */
fun monoBadgeIn(badges: Map<String, String>, key: String): TextIcon? = badges[key]?.let { n -> TextIcon.entries.firstOrNull { it.name == n } }

/** The colour badge saved for [key] in a folder-badge map, if it has one. */
fun colourBadgeIn(badges: Map<String, String>, key: String): TextIcon? = badges[key]?.takeIf { it.startsWith(COLOUR_BADGE) }
    ?.removePrefix(COLOUR_BADGE)?.let { n -> TextIcon.entries.firstOrNull { it.name == n } }

/** The most apps a folder holds (as many as the 2 × 4 grid's screen). */
const val FOLDER_MAX = 8

private fun Layout.itemAt(page: Int, index: Int): Item? = pages.getOrNull(page)?.items?.getOrNull(index)

/**
 * Drops the app at (fromPage, fromIndex) onto the item at (toPage, toIndex): onto another app they become
 * a new folder named "Folder" (id [newId]) in that place; onto a folder with room it joins it. Null when
 * that is not allowed: only apps go into folders, not onto the All apps icon, not into a full folder.
 */
fun Layout.dropOnto(fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int, newId: String, keepGaps: Boolean = false): Layout? {
    if (fromPage == toPage && fromIndex == toIndex) return null
    val dragged = itemAt(fromPage, fromIndex) as? Item.App ?: return null
    val replacement: Item = when (val target = itemAt(toPage, toIndex) ?: return null) {
        is Item.App -> Item.Folder(newId, "Folder", listOf(target.key, dragged.key))
        is Item.Folder -> if (target.apps.size >= FOLDER_MAX) return null else target.copy(apps = target.apps + dragged.key)
        Item.AllApps, Item.Search, Item.Emergency, is Item.Widget, Item.Covered -> return null
    }
    val without = removeFromPage(fromPage, fromIndex, keepGaps)
    val at = if (!keepGaps && fromPage == toPage && fromIndex < toIndex) toIndex - 1 else toIndex
    return without.copy(pages = without.pages.mapIndexed { i, p ->
        if (i == toPage) Page(p.items.mapIndexed { j, it -> if (j == at) replacement else it }, p.columns, p.rows) else p
    })
}

/**
 * Takes [key] out of the folder at (page, index). It goes on the same page if there is room, else the first
 * page with room. A folder left with one app turns back into that app; an empty one goes.
 */
fun Layout.takeOutOfFolder(page: Int, index: Int, key: String, keepGaps: Boolean = false): Layout {
    val folder = itemAt(page, index) as? Item.Folder ?: return this
    if (key !in folder.apps) return this
    val rest = folder.apps - key
    val replaced = pages.mapIndexed { i, p ->
        if (i != page) p else Page(columns = p.columns, rows = p.rows, items = p.items.mapIndexed { j, it ->
            if (j != index) it else when (rest.size) {
                0 -> null
                1 -> Item.App(rest[0])
                else -> folder.copy(apps = rest)
            }
        }.let { l -> if (keepGaps || p.hasWidgets) l else l.filterNotNull() }).packedEnd()
    }
    val base = copy(pages = replaced)
    return base.addToPage(page, Item.App(key)) ?: base.add(Item.App(key))
}

/** Moves [key] to place [to] (0 is first) inside the folder at (page, index). */
fun Layout.moveInFolder(page: Int, index: Int, key: String, to: Int): Layout {
    val folder = itemAt(page, index) as? Item.Folder ?: return this
    if (key !in folder.apps) return this
    val rest = folder.apps - key
    val moved = rest.toMutableList().apply { add(to.coerceIn(0, rest.size), key) }
    return copy(pages = pages.mapIndexed { i, p ->
        if (i != page) p else Page(p.items.mapIndexed { j, it -> if (j == index) folder.copy(apps = moved) else it }, p.columns, p.rows)
    })
}

/**
 * Puts [key] at the end of the folder at [page]/[index]; unchanged if it is already there. Null when that is not a
 * folder or the folder already holds [FOLDER_MAX] apps.
 */
fun Layout.addToFolder(page: Int, index: Int, key: String): Layout? {
    val folder = itemAt(page, index) as? Item.Folder ?: return null
    if (key in folder.apps) return this
    if (folder.apps.size >= FOLDER_MAX) return null
    return copy(pages = pages.mapIndexed { i, p ->
        if (i != page) p else Page(p.items.mapIndexed { j, it -> if (j == index) folder.copy(apps = folder.apps + key) else it }, p.columns, p.rows)
    })
}

fun Layout.renameFolder(page: Int, index: Int, name: String): Layout {
    val folder = itemAt(page, index) as? Item.Folder ?: return this
    val clean = name.trim().ifEmpty { "Folder" }.take(24)
    return copy(pages = pages.mapIndexed { i, p ->
        if (i != page) p else Page(p.items.mapIndexed { j, it -> if (j == index) folder.copy(name = clean) else it }, p.columns, p.rows)
    })
}

object DefaultLayout {
    /**
     * A first layout, clean: one empty screen ready for apps to be added, and [preferredDock] (those that are
     * installed, at most [DOCK_MAX]) in the bottom bar.
     */
    fun build(installed: List<String>, preferredDock: List<String>, columns: Int = 2, rows: Int = PAGE_ROWS): Layout {
        val have = installed.toSet()
        // A fresh bar has four apps, two each side, to suit the standard four places.
        val dock = preferredDock.filter { it in have }.distinct().take(4)
        // Start again keeps the grid that is set; one that is not a grid of ours is 2 × 4.
        val (c, r) = (columns to rows).takeIf { it in GRIDS } ?: (2 to PAGE_ROWS)
        return Layout(listOf(Page(columns = c, rows = r)), dock, columns = c, rows = r)
    }

    /** The same bottom bar, with every other app in [installed], in the order given, filling pages of the grid. */
    fun buildFilled(installed: List<String>, preferredDock: List<String>, columns: Int = 2, rows: Int = PAGE_ROWS): Layout {
        val empty = build(installed, preferredDock, columns, rows)
        val (c, r) = empty.columns to empty.rows
        val rest = installed.filter { it !in empty.dock }
        val pages = rest.chunked(c * r).map { chunk -> Page(chunk.map { Item.App(it) }, c, r) }
        return Layout(pages.ifEmpty { listOf(Page(columns = c, rows = r)) }, empty.dock, columns = c, rows = r)
    }
}

/** Whether the welcome's "n × 4" button is the grid that is set (not when a smaller grid such as 4 × 5 is). */
fun welcomeGridPicked(columns: Int, rows: Int, n: Int): Boolean = columns == n && rows == PAGE_ROWS

/** A tag for filtering All apps: its name and the apps that have it. */
data class AppTag(val name: String, val apps: Set<String>)

/** A tag name trimmed to at most 24 characters, or null when it is blank. */
fun cleanTagName(name: String): String? = name.trim().take(24).ifEmpty { null }
