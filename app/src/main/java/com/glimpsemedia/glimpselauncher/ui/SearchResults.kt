package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.SearchEngine
import com.glimpsemedia.glimpselauncher.model.SearchHeading
import com.glimpsemedia.glimpselauncher.model.Section
import com.glimpsemedia.glimpselauncher.model.WalkResult
import com.glimpsemedia.glimpselauncher.model.engineUrl
import com.glimpsemedia.glimpselauncher.model.rankByName
import com.glimpsemedia.glimpselauncher.model.rankHelp
import com.glimpsemedia.glimpselauncher.model.section

/** An app as Search sees it: its key and its name. */
data class AppName(val key: String, val label: String)

/**
 * What Search looks through. [files] is null when Add folder location(s) is off; [engines] is empty when
 * Add search engine(s) is off or none were added. [menu] finds Settings rows (the Settings map's own search).
 */
data class SearchInput(
    val apps: List<AppName>,
    val files: WalkResult?,
    val help: List<Pair<String, String>>,
    val engines: List<SearchEngine>,
    val menu: (String) -> List<SettingsHit> = { SettingsMap.search(it, max = 200) },
) {
    /** The file and folder names made ready once for this input, not on every letter typed. */
    val names: NameIndex by lazy { NameIndex(files?.entries.orEmpty()) }
}

/**
 * Found files and folders with their names lowercased and trimmed once: Glimpse Launcher Plus can find 200,000 of
 * them, and Search ranks them again with each letter typed.
 */
class NameIndex(entries: List<Entry>) {
    // Only the places in [entries] are kept, not copies of the entries: Plus's list makes each one again when asked.
    private val fileAt: IntArray
    private val folderAt: IntArray
    val fileKeys: Array<String>
    val folderKeys: Array<String>

    init {
        val fa = ArrayList<Int>(); val fo = ArrayList<Int>(); val fk = ArrayList<String>(); val ok = ArrayList<String>()
        for (i in entries.indices) {
            val e = entries[i]
            // trim() and lowercase() give back the same string when nothing changes, so most names are not copied.
            if (e.isFolder) { fo += i; ok += e.name.trim().lowercase() } else { fa += i; fk += e.name.trim().lowercase() }
        }
        fileAt = fa.toIntArray(); folderAt = fo.toIntArray(); fileKeys = fk.toTypedArray(); folderKeys = ok.toTypedArray()
    }

    val files: List<Entry> = view(entries, fileAt)
    val folders: List<Entry> = view(entries, folderAt)

    private fun view(entries: List<Entry>, at: IntArray): List<Entry> = object : java.util.AbstractList<Entry>(), java.util.RandomAccess {
        override val size get() = at.size
        override fun get(index: Int) = entries[at[index]]
    }
}

/**
 * The same order as [rankByName] (names beginning with the query first, then the rest, each A to Z, ties in their
 * first order), but for a list whose names are ready in [keys], and keeping only the first [limit] (null: all) while
 * still counting every match.
 */
fun <T> rankReady(items: List<T>, keys: Array<String>, query: String, limit: Int?): Pair<List<T>, Int> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList<T>() to 0
    // Each match as (0 begins with the query / 1 contains it, its name, its place) so the order is total.
    val order = Comparator<Int> { a, b ->
        val ga = if (keys[a].startsWith(q)) 0 else 1
        val gb = if (keys[b].startsWith(q)) 0 else 1
        if (ga != gb) ga - gb else keys[a].compareTo(keys[b]).let { if (it != 0) it else a - b }
    }
    var total = 0
    if (limit == null) {
        val all = ArrayList<Int>()
        for (i in keys.indices) if (q in keys[i]) { all += i; total++ }
        return all.sortedWith(order).map { items[it] } to total
    }
    // The best [limit] so far, worst on top, so each new match is compared with one name only.
    val best = java.util.PriorityQueue(minOf(limit, 1023) + 1, order.reversed())
    for (i in keys.indices) {
        if (q !in keys[i]) continue
        total++
        best += i
        if (best.size > limit) best.poll()
    }
    return best.sortedWith(order).map { items[it] } to total
}

private fun <T> readySection(heading: SearchHeading, items: List<T>, keys: Array<String>, query: String, more: Int, hit: (T) -> SearchHit): Section<SearchHit>? {
    val (shown, total) = rankReady(items, keys, query, com.glimpsemedia.glimpselauncher.model.shownCount(heading, more))
    return if (total == 0) null else Section(heading, shown.map(hit), total)
}

/** Settings results that open their own screen (Help, About), which needs no unlocking. */
fun opensWithoutUnlock(section: String): Boolean = section == "help" || section == "about"

/** One line in the search results. */
sealed interface SearchHit {
    data class App(val app: AppName) : SearchHit
    data class File(val entry: Entry) : SearchHit
    data class Folder(val entry: Entry) : SearchHit
    data class Menu(val hit: SettingsHit) : SearchHit
    data class Help(val title: String) : SearchHit
    data class Online(val engine: SearchEngine, val url: String) : SearchHit
}

/** The results for [query], heading by heading in the fixed order; [more] says how often each heading's "Load more" was touched. */
fun buildResults(input: SearchInput, query: String, more: Map<SearchHeading, Int>): List<Section<SearchHit>> {
    if (query.isBlank()) return emptyList()
    fun sec(h: SearchHeading, hits: List<SearchHit>) = section(h, hits, more[h] ?: 0)
    val n = input.names
    return listOfNotNull(
        sec(SearchHeading.Apps, rankByName(input.apps, query) { it.label }.map { SearchHit.App(it) }),
        readySection(SearchHeading.Files, n.files, n.fileKeys, query, more[SearchHeading.Files] ?: 0) { SearchHit.File(it) },
        readySection(SearchHeading.Folders, n.folders, n.folderKeys, query, more[SearchHeading.Folders] ?: 0) { SearchHit.Folder(it) },
        sec(SearchHeading.Menu, rankByName(input.menu(query), query) { it.label }.map { SearchHit.Menu(it) }),
        sec(SearchHeading.Help, rankHelp(input.help, query, { it.first }, { it.second }).map { SearchHit.Help(it.first) }),
        sec(SearchHeading.Online, input.engines.map { SearchHit.Online(it, engineUrl(it, query)) }),
    )
}
