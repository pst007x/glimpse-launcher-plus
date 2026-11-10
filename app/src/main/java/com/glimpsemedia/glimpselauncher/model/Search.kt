package com.glimpsemedia.glimpselauncher.model

/** The headings of the search window, in the order they are shown, with how many results each shows at first. */
enum class SearchHeading(val title: String, val limit: Int) {
    Apps("Apps", 8), Files("Files", 10), Folders("Folders", 10), Menu("Menu items", 8), Help("Help items", 6),
    Online("Online", Int.MAX_VALUE),
}

/**
 * The items whose name holds what was typed (spaces round it and capitals ignored): names that begin with it first,
 * then the rest, each A to Z. Nothing typed gives nothing. Plain text matching: "c++" or "50%" mean just that.
 */
fun <T> rankByName(items: List<T>, query: String, name: (T) -> String): List<T> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val hits = items.map { it to name(it).trim().lowercase() }.filter { (_, n) -> q in n }
    val (starts, rest) = hits.partition { (_, n) -> n.startsWith(q) }
    return (starts.sortedBy { it.second } + rest.sortedBy { it.second }).map { it.first }
}

/** Help topics: those whose title matches first (as [rankByName]), then those where only the text does, A to Z. */
fun <T> rankHelp(topics: List<T>, query: String, title: (T) -> String, text: (T) -> String): List<T> {
    val byTitle = rankByName(topics, query, title)
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val byText = topics.filter { it !in byTitle && q in text(it).lowercase() }.sortedBy { title(it).lowercase() }
    return byTitle + byText
}

/** One heading's results: those [shown] now and how many there are in all. */
data class Section<T>(val heading: SearchHeading, val shown: List<T>, val total: Int)

/** How many more results each "Load more" adds under a heading. */
const val LOAD_MORE = 50

/** How many results a heading shows after "Load more" was touched [more] times. */
fun shownCount(heading: SearchHeading, more: Int): Int =
    if (heading.limit == Int.MAX_VALUE) Int.MAX_VALUE else heading.limit + LOAD_MORE * more.coerceAtLeast(0)

/** A heading's section, or null when it has no results; each of [more] "Load more" touches shows [LOAD_MORE] more. */
fun <T> section(heading: SearchHeading, ranked: List<T>, more: Int): Section<T>? =
    if (ranked.isEmpty()) null else Section(heading, ranked.take(shownCount(heading, more)), ranked.size)
