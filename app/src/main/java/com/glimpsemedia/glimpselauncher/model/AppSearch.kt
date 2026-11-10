package com.glimpsemedia.glimpselauncher.model

/** The heading an app is listed under: its first letter, or "#" for a number or symbol. */
fun initialOf(label: String): String {
    val c = label.trim().firstOrNull() ?: return "#"
    return if (c.isLetter()) c.uppercaseChar().toString() else "#"
}

private fun key(label: String) = label.trim().lowercase()

/**
 * The apps to show for what has been typed so far, A to Z. Nothing typed: all of them. Otherwise the apps whose
 * name begins with it ("g", then "go", then "goo"); if none does, those with a word that begins with it
 * ("maps" finds "Google Maps"), and then those that contain it anywhere.
 */
fun <T> searchApps(items: List<T>, query: String, label: (T) -> String): List<T> {
    val sorted = items.sortedWith(compareBy({ initialOf(label(it)) == "#" }, { key(label(it)) }))
    val q = query.trim().lowercase()
    if (q.isEmpty()) return sorted
    val starts = sorted.filter { key(label(it)).startsWith(q) }
    if (starts.isNotEmpty()) return starts
    val word = sorted.filter { key(label(it)).split(' ', '-', '_', '.').any { w -> w.startsWith(q) } }
    if (word.isNotEmpty()) return word
    return sorted.filter { q in key(label(it)) }
}

/** The apps in the order given, grouped under their first letter ("#" last). */
fun <T> groupedByInitial(items: List<T>, label: (T) -> String): List<Pair<String, List<T>>> =
    items.groupBy { initialOf(label(it)) }.toList()
