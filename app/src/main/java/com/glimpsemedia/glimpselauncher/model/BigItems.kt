package com.glimpsemedia.glimpselauncher.model

// Big items: an app, folder, All apps, Search or Emergency taking 2 × 2 squares. It sits in its top-left square (the
// anchor) and the other three hold Item.Covered, as a widget's do; Layout.bigItems names the big ones by colorKey.

/** What Make it big did: the new layout, or why nothing changed. */
sealed interface BigResult {
    data class Done(val layout: Layout) : BigResult
    /** Every 2 × 2 block around the item has a widget in it. */
    data object NoRoom : BigResult
    /** Not an item that can be big (a widget, a covered or empty square, one already big). */
    data object NotAllowed : BigResult
}

/** Whether [item] is a kind that can be made big. */
fun canBeBig(item: Item?): Boolean = item is Item.App || item is Item.Folder || item == Item.AllApps || item == Item.Search || item == Item.Emergency

/** The four squares of the 2 × 2 block with its top-left at [anchor], or null when it would leave the grid. */
fun Page.blockSquares(anchor: Int): List<Int>? {
    if (anchor < 0 || anchor >= slots) return null
    val col = anchor % columns
    val row = anchor / columns
    if (col + 1 >= columns || row + 1 >= rows) return null
    return listOf(anchor, anchor + 1, anchor + columns, anchor + columns + 1)
}

/**
 * Which square owns each square: itself for an item, the anchor for a square a widget or big item covers, -1 for an
 * empty square or a covered square nothing owns. [big] names the big items; null takes any item whose block is all
 * covered (for a layout already repaired).
 */
fun Page.owners(big: Set<String>? = null): IntArray {
    val own = IntArray(slots) { -1 }
    fun at(i: Int) = items.getOrNull(i)
    for (i in 0 until slots) {
        val w = at(i) as? Item.Widget ?: continue
        val col = i % columns
        val row = i / columns
        for (r in 0 until w.rows) for (c in 0 until w.cols) {
            val s = (row + r) * columns + col + c
            if (col + c < columns && s < slots) own[s] = i
        }
    }
    for (i in 0 until slots) {
        val item = at(i) ?: continue
        if (item == Item.Covered || item is Item.Widget) continue
        own[i] = i
        val isBig = if (big != null) item.colorKey in big else true
        if (!isBig) continue
        val block = blockSquares(i) ?: continue
        val rest = block.drop(1)
        if (rest.all { at(it) == Item.Covered && own[it] == -1 }) rest.forEach { own[it] = i }
    }
    return own
}

/** The square that owns [slot] (the anchor of whatever covers it), or null when it is empty. */
fun Page.ownerOf(slot: Int, big: Set<String>? = null): Int? = owners(big).getOrNull(slot)?.takeIf { it >= 0 }

/** The squares a big item at [anchor] takes. */
private fun Page.bigSquares(anchor: Int): List<Int> = blockSquares(anchor) ?: listOf(anchor)

/** [item] big at [anchor] (its block must be empty). */
private fun Page.withBigAt(anchor: Int, item: Item): Page {
    var p = this
    for ((n, s) in bigSquares(anchor).withIndex()) p = p.set(s, if (n == 0) item else Item.Covered)
    return p
}

/** The first 2 × 2 block of empty squares, by its anchor, or null. */
fun Page.freeBlock(): Int? = (0 until slots).firstOrNull { a -> blockSquares(a)?.all { items.getOrNull(it) == null } == true }

/** [item] big in the first free 2 × 2 block, or null when there is none. */
fun Page.withBigItem(item: Item): Page? = freeBlock()?.let { withBigAt(it, item) }

/** What was in the way: the items, in reading order, each with whether it was big. */
private data class Moved(val item: Item, val big: Boolean)

/**
 * Puts [moved] on page [from] onwards: a normal item in the first empty square, a big one in the first free 2 × 2
 * block (on the page where the normal ones would go), or normal if that page has none. Pages are added at the end as
 * needed. Returns the pages and the keys that had to become normal.
 */
private fun Layout.placeMoved(start: List<Page>, from: Int, moved: List<Moved>): Pair<List<Page>, Set<String>> {
    val pages = start.toMutableList()
    val lost = mutableSetOf<String>()
    var p = from
    for (m in moved) {
        while (true) {
            if (p >= pages.size) pages += Page(columns = columns, rows = rows)
            val page = pages[p]
            val bigPlaced = if (m.big) page.withBigItem(m.item) else null
            if (bigPlaced != null) { pages[p] = bigPlaced; break }
            val placed = page.withItem(m.item)
            if (placed != null) {
                if (m.big) lost += m.item.colorKey
                pages[p] = placed
                break
            }
            p++
        }
    }
    return pages to lost
}

/**
 * Puts big [item] with its block at [anchor] on page [pageIndex] of [pages] (the item itself already taken off):
 * whatever is in the block moves out of the way ([placeMoved]). Null when a widget is in the block.
 */
private fun Layout.bigInto(pages: List<Page>, pageIndex: Int, anchor: Int, item: Item): Pair<List<Page>, Set<String>>? {
    val page = pages[pageIndex]
    val block = page.blockSquares(anchor) ?: return null
    val own = page.owners(bigItems)
    if (block.any { s -> page.items.getOrNull(own[s].takeIf { it >= 0 } ?: -1) is Item.Widget }) return null
    val anchors = block.map { own[it] }.filter { it >= 0 }.distinct().sorted()
    var cleared = page
    val moved = anchors.map { a ->
        val thing = page.items[a]!!
        val squares = (0 until page.slots).filter { s -> own[s] == a }
        val isBig = thing.colorKey in bigItems && squares.size > 1
        for (s in squares) cleared = cleared.set(s, null)
        Moved(thing, isBig)
    }
    val list = pages.toMutableList()
    list[pageIndex] = cleared.withBigAt(anchor, item)
    return placeMoved(list, pageIndex, moved)
}

/** Make it big: the item at [slot] takes 2 × 2 squares (see the spec for where it grows and what moves). */
fun Layout.makeBig(pageIndex: Int, slot: Int): BigResult {
    val page = pages.getOrNull(pageIndex) ?: return BigResult.NotAllowed
    val item = page.items.getOrNull(slot)
    if (!canBeBig(item) || item!!.colorKey in bigItems) return BigResult.NotAllowed
    val c = columns
    // Right and down, left and down, right and up, left and up.
    for (anchor in listOf(slot, slot - 1, slot - c, slot - c - 1)) {
        val block = page.blockSquares(anchor) ?: continue
        if (slot !in block) continue
        val without = pages.toMutableList().also { it[pageIndex] = page.set(slot, null) }
        val (out, lost) = bigInto(without, pageIndex, anchor, item) ?: continue
        return BigResult.Done(copy(pages = out, bigItems = bigItems + item.colorKey - lost))
    }
    return BigResult.NoRoom
}

/** Make it normal: the big item at [anchor] goes back to one square; the other three are emptied. */
fun Layout.makeNormal(pageIndex: Int, anchor: Int): Layout {
    val page = pages.getOrNull(pageIndex) ?: return this
    val item = page.items.getOrNull(anchor) ?: return this
    if (item.colorKey !in bigItems) return this
    val own = page.owners(bigItems)
    var p = page
    for (s in 0 until page.slots) if (s != anchor && own[s] == anchor) p = p.set(s, null)
    return copy(pages = pages.mapIndexed { i, it -> if (i == pageIndex) p else it }, bigItems = bigItems - item.colorKey)
}

/** The big item at ([pageIndex], [anchor]) taken off its page: the pages without it, and the item. */
private fun Layout.liftBig(pageIndex: Int, anchor: Int): Pair<List<Page>, Item>? {
    val page = pages.getOrNull(pageIndex) ?: return null
    val item = page.items.getOrNull(anchor) ?: return null
    if (item.colorKey !in bigItems) return null
    val own = page.owners(bigItems)
    var p = page
    for (s in 0 until page.slots) if (own[s] == anchor) p = p.set(s, null)
    return pages.mapIndexed { i, it -> if (i == pageIndex) p else it } to item
}

/**
 * Drags the big item at [fromAnchor] so its top-left is at [wantedAnchor] of [toPage], kept on the grid; whatever is
 * there moves out of the way. Null when a widget is in the way (it stays where it was).
 */
fun Layout.moveBig(fromPage: Int, fromAnchor: Int, toPage: Int, wantedAnchor: Int): Layout? {
    if (toPage !in pages.indices) return null
    val (lifted, item) = liftBig(fromPage, fromAnchor) ?: return null
    val col = (wantedAnchor % columns).coerceIn(0, columns - 2)
    val row = (wantedAnchor / columns).coerceIn(0, rows - 2)
    val (out, lost) = bigInto(lifted, toPage, row * columns + col, item) ?: return null
    return copy(pages = out, bigItems = bigItems - lost)
}

/** Move to the next screen for a big item: the first free 2 × 2 block on [toPage]; null when there is none. */
fun Layout.moveBigToPage(fromPage: Int, fromAnchor: Int, toPage: Int): Layout? {
    if (toPage !in pages.indices) return null
    val (lifted, item) = liftBig(fromPage, fromAnchor) ?: return null
    val placed = lifted[toPage].withBigItem(item) ?: return null
    return copy(pages = lifted.mapIndexed { i, p -> if (i == toPage) placed else p })
}

/**
 * The layout made consistent, never trusting what was stored: a big item whose block leaves the grid or is not free
 * becomes normal, a covered square nothing covers is emptied, and keys with no item on a screen are dropped.
 */
fun Layout.withBigRepaired(): Layout {
    var big = bigItems.filter { k -> pages.any { p -> p.items.any { it != null && it != Item.Covered && it !is Item.Widget && it.colorKey == k } } }.toSet()
    var out = pages
    while (true) {
        val failed = mutableSetOf<String>()
        out = pages.map { page ->
            val claimed = BooleanArray(page.slots)
            fun at(i: Int) = page.items.getOrNull(i)
            for (i in 0 until page.slots) {
                val w = at(i) as? Item.Widget ?: continue
                for (r in 0 until w.rows) for (c in 0 until w.cols) {
                    val s = (i / page.columns + r) * page.columns + i % page.columns + c
                    if (i % page.columns + c < page.columns && s < page.slots) claimed[s] = true
                }
            }
            val list = MutableList(page.slots) { at(it) }
            for (i in 0 until page.slots) {
                val item = list[i] ?: continue
                if (item == Item.Covered || item is Item.Widget || item.colorKey !in big) continue
                val rest = page.blockSquares(i)?.drop(1)
                if (rest == null || !rest.all { (list[it] == Item.Covered && !claimed[it]) || list[it] == null }) { failed += item.colorKey; continue }
                rest.forEach { claimed[it] = true; list[it] = Item.Covered }
            }
            for (i in 0 until page.slots) if (list[i] == Item.Covered && !claimed[i]) list[i] = null
            Page(list, page.columns, page.rows).packedEnd()
        }
        if (failed.isEmpty()) break
        big = big - failed
    }
    val samePages = out.indices.all { out[it] == pages[it] }
    return if (samePages && big == bigItems) this else copy(pages = if (samePages) pages else out, bigItems = big)
}

/** A big folder's panel: [grid] across and down (2 or 3), the first [shown] apps, and [more] past them ("+N"). */
data class BigFolderCells(val grid: Int, val shown: Int, val more: Int)

/** The smallest a small icon may be, so it is easy to touch. */
const val BIG_FOLDER_MIN_CELL_DP = 48f

/**
 * The panel for [apps] apps in a panel [sideDp] across: 3 × 3 only when each cell can be [BIG_FOLDER_MIN_CELL_DP]
 * (8 dp padding each side, 6 dp gaps), else 2 × 2. When they do not all fit, the last cell is "+N".
 */
fun bigFolderCells(apps: Int, sideDp: Float): BigFolderCells {
    val n = apps.coerceAtLeast(0)
    val threeFit = (sideDp - 16f - 12f) / 3f >= BIG_FOLDER_MIN_CELL_DP
    return when {
        n <= 4 -> BigFolderCells(2, n, 0)
        threeFit && n <= 9 -> BigFolderCells(3, n, 0)
        threeFit -> BigFolderCells(3, 8, n - 8)
        else -> BigFolderCells(2, 3, n - 3)
    }
}
