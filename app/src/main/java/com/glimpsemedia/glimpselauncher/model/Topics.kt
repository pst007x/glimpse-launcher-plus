package com.glimpsemedia.glimpselauncher.model

/**
 * A kind of app people look for by what it does rather than by its name. [words] are what a person might type
 * ("web", "internet" or "browse" all mean the browsers); the list of related apps for what has been typed uses
 * every topic with a word that begins with it.
 */
enum class Topic(val title: String, val words: List<String>) {
    INTERNET("Internet", listOf("internet", "web", "browser", "browse", "online", "website", "surf")),
    SOCIAL("Social", listOf("social", "media", "friends", "network", "chat", "facebook", "post")),
    MESSAGES("Messages", listOf("messages", "message", "messaging", "text", "texts", "sms", "chat")),
    PHONE("Phone", listOf("phone", "call", "calls", "dial", "ring", "telephone")),
    PHOTOS("Photos", listOf("photos", "photo", "pictures", "picture", "images", "camera", "gallery", "selfie")),
    VIDEO("Video", listOf("video", "videos", "film", "films", "movies", "television", "tv", "watch")),
    MUSIC("Music", listOf("music", "songs", "song", "radio", "listen", "audio", "podcast", "podcasts")),
    GAMES("Games", listOf("games", "game", "play", "puzzle", "cards")),
    NEWS("News", listOf("news", "newspaper", "headlines")),
    MAPS("Maps", listOf("maps", "map", "navigation", "directions", "travel", "gps", "route", "satnav")),
    EMAIL("Email", listOf("email", "mail", "letter")),
    OFFICE("Documents", listOf("documents", "document", "office", "work", "productivity", "files", "spreadsheet")),
    ACCESSIBILITY("Accessibility", listOf("accessibility", "easier", "magnify", "screenreader")),
}

private const val MIN_RELATED_LETTERS = 2

/**
 * The topics for what has been typed: those with a word that begins with it. Needs two letters, so a single
 * "p" does not start listing every kind of app.
 */
fun topicsFor(query: String): List<Topic> {
    val q = query.trim().lowercase()
    if (q.length < MIN_RELATED_LETTERS) return emptyList()
    return Topic.entries.filter { t -> t.words.any { it.startsWith(q) } }
}

/**
 * The apps related to what was typed, after the ones that narrow it: for each topic, the apps in that topic that
 * are not in [already], A to Z. Topics with no app left are left out.
 */
fun <T> relatedApps(
    items: List<T>,
    query: String,
    already: Collection<T>,
    label: (T) -> String,
    topicsOf: (T) -> Set<Topic>,
): List<Pair<Topic, List<T>>> {
    val shown = already.toSet()
    return topicsFor(query).mapNotNull { topic ->
        val apps = searchApps(items.filter { topic in topicsOf(it) && it !in shown }, "", label)
        if (apps.isEmpty()) null else topic to apps
    }
}
