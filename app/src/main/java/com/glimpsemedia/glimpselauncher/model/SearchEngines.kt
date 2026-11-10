package com.glimpsemedia.glimpselauncher.model

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** A web search the user added: a name, and an address with %s where the search words go. None come built in. */
data class SearchEngine(val name: String, val url: String)

/** A folder the user picked for Search: the picker's tree address and the folder's name. */
data class FolderLocation(val uri: String, val name: String)

/** The answer to checking a new search engine: fine, or what is wrong with each field. */
sealed interface EngineCheck {
    data class Ok(val engine: SearchEngine) : EngineCheck
    data class Bad(val nameError: String?, val addressError: String?) : EngineCheck
}

const val ENGINE_NAME_MAX = 30

/** Checks a search engine as typed: a name of 1 to 30 letters, and a web address holding %s. */
fun checkEngine(name: String, address: String): EngineCheck {
    val n = name.trim()
    val nameError = if (n.isEmpty() || n.length > ENGINE_NAME_MAX) "Give it a name (up to 30 letters)" else null
    val a = address.trim()
    // Checked with a marker in place of %s, which is put back in the saved address.
    val url = if ("%s" in a) webAddress(a.replace("%s", "QUERYMARK"))?.replace("QUERYMARK", "%s") else null
    val addressError = when {
        "%s" !in a -> "The address needs %s where your search words go"
        url == null -> "That is not a web address"
        else -> null
    }
    return if (nameError == null && url != null) EngineCheck.Ok(SearchEngine(n, url)) else EngineCheck.Bad(nameError, addressError)
}

/** The engine's address for [query]: the words (spaces round them dropped) put in place of %s, web-encoded. */
fun engineUrl(engine: SearchEngine, query: String): String =
    engine.url.replace("%s", URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20"))

/** The engine's site, shown under its line ("duckduckgo.com"), or its address when that cannot be read. */
fun engineHost(engine: SearchEngine): String =
    try { java.net.URI(engine.url.replace("%s", "x")).host } catch (_: Exception) { null } ?: engine.url

/** The folders with [picked] added at the end, unless that folder (same address) is already there. */
fun List<FolderLocation>.withPicked(picked: FolderLocation): List<FolderLocation> = if (any { it.uri == picked.uri }) this else this + picked

/** The saved search engines and folder locations, as JSON. Unreadable text or entries are left out. */
object SearchListsJson {
    fun engines(json: String?): List<SearchEngine> = read(json) { o ->
        val name = o.opt("name") as? String
        val url = o.opt("url") as? String
        if (name != null && url != null) SearchEngine(name, url) else null
    }

    fun encodeEngines(l: List<SearchEngine>): String =
        JSONArray().apply { l.forEach { put(JSONObject().put("name", it.name).put("url", it.url)) } }.toString()

    fun folders(json: String?): List<FolderLocation> = read(json) { o ->
        val uri = o.opt("uri") as? String
        val name = o.opt("name") as? String
        if (uri != null && name != null) FolderLocation(uri, name) else null
    }

    fun encodeFolders(l: List<FolderLocation>): String =
        JSONArray().apply { l.forEach { put(JSONObject().put("uri", it.uri).put("name", it.name)) } }.toString()

    private fun <T> read(json: String?, one: (JSONObject) -> T?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        val a = try { JSONArray(json) } catch (_: Exception) { return emptyList() }
        return (0 until a.length()).mapNotNull { i -> (a.opt(i) as? JSONObject)?.let(one) }
    }
}
