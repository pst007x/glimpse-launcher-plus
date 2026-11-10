package com.glimpsemedia.glimpselauncher.model

import org.json.JSONArray
import org.json.JSONObject

/** The notes as text for `notes.json`, and back. Anything unreadable, or from a newer version, gives null. */
object NotesJson {
    private const val VERSION = 1

    fun encode(n: Notes): String {
        val notes = JSONArray()
        n.list.forEach { note ->
            val lines = JSONArray()
            note.lines.forEach { lines.put(JSONObject().put("x", it.text).put("k", it.ticked).put("c", it.color)) }
            notes.put(
                JSONObject().put("id", note.id).put("t", note.title).put("c", note.color).put("f", note.folded)
                    .put("lines", lines).put("p", JSONArray(note.pictures)),
            )
        }
        return JSONObject().put("v", VERSION).put("notes", notes).toString()
    }

    fun decode(text: String): Notes? = try {
        val root = JSONObject(text)
        if (root.getInt("v") != VERSION) null else {
            val arr = root.getJSONArray("notes")
            Notes((0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val lines = o.optJSONArray("lines") ?: JSONArray()
                val pics = o.optJSONArray("p") ?: JSONArray()
                Note(
                    id = o.getString("id"),
                    title = o.optString("t"),
                    color = o.optInt("c"),
                    folded = o.optBoolean("f"),
                    lines = (0 until lines.length()).map { j ->
                        val l = lines.getJSONObject(j)
                        NoteLine(l.optString("x"), l.optBoolean("k"), l.optInt("c"))
                    },
                    // Only plain file names: a name can never point outside the notes folder.
                    pictures = (0 until pics.length()).map { pics.getString(it) }.filter { isPlainFileName(it) },
                )
            }.let { list ->
                // Two notes with one id would break the list (each card needs its own key): the later ones get new ids.
                val seen = HashSet<String>()
                list.mapIndexed { i, n -> if (seen.add(n.id)) n else n.copy(id = "${n.id}-$i").also { seen.add(it.id) } }
            })
        }
    } catch (_: Exception) {
        null
    }

    fun isPlainFileName(name: String): Boolean = name.isNotEmpty() && '/' !in name && '\\' !in name && name != "." && name != ".."
}
