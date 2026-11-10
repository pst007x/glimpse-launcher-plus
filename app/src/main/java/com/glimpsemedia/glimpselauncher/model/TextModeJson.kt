package com.glimpsemedia.glimpselauncher.model

import org.json.JSONArray
import org.json.JSONObject

/** The text layout as text for `textmode.json`, and back. Anything unreadable, or from a newer version, gives null. */
object TextModeJson {
    private const val VERSION = 1

    fun encode(t: TextLayout): String {
        val folders = JSONArray()
        t.folders.forEach { folders.put(JSONObject().put("id", it.id).put("n", it.name).put("i", it.icon.name).put("e", JSONArray(it.entries)).put("c", it.color)) }
        val widgets = JSONArray()
        t.widgets.forEach { widgets.put(JSONObject().put("id", it.id).put("p", it.provider).put("h", it.height)) }
        return JSONObject().put("v", VERSION).put("apps", JSONArray(t.apps)).put("folders", folders).put("w", widgets).toString()
    }

    private fun entries(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }.filter { '/' in it || isBuiltInEntry(it) }.distinct()

    fun decode(text: String): TextLayout? = try {
        val root = JSONObject(text)
        if (root.getInt("v") != VERSION) null else {
            val arr = root.optJSONArray("folders") ?: JSONArray()
            val seen = HashSet<String>()
            val folders = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TextFolder(o.getString("id"), o.optString("n"), textIconOf(o.optString("i")), entries(o.optJSONArray("e")), o.optInt("c", 0))
            }.mapIndexed { i, f -> if (seen.add(f.id)) f else f.copy(id = "${f.id}-$i").also { seen.add(it.id) } }
            val w = root.optJSONArray("w") ?: JSONArray()
            val widgets = (0 until w.length()).mapNotNull { i ->
                val o = w.optJSONObject(i) ?: return@mapNotNull null
                if (!o.has("id") || !o.has("p")) null
                else TextWidget(o.getInt("id"), o.getString("p"), o.optInt("h", 1).takeIf { it in 0..3 } ?: 1)
            }.distinctBy { it.id }
            TextLayout(entries(root.optJSONArray("apps")), folders, widgets)
        }
    } catch (_: Exception) {
        null
    }
}
