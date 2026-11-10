package com.glimpsemedia.glimpselauncher.model

/** One line of a note: its text, its tick, and an optional colour band (0 is none). */
data class NoteLine(val text: String = "", val ticked: Boolean = false, val color: Int = 0)

/**
 * A note card: a title, its own colour (0 is none), folded or open, its lines (never fewer than one when shown),
 * and the file names of its pictures in the launcher's own `files/notes/` folder.
 */
data class Note(
    val id: String,
    val title: String = "",
    val color: Int = 0,
    val folded: Boolean = false,
    val lines: List<NoteLine> = listOf(NoteLine()),
    val pictures: List<String> = emptyList(),
)

/** Every note, newest first. */
data class Notes(val list: List<Note> = emptyList())

/** The colour bands a line can have: yellow, orange, red, pink, purple, blue, green, grey. */
val NOTE_LINE_COLORS: List<Int> = listOf(
    0xFFFFE082, 0xFFFFCC80, 0xFFEF9A9A, 0xFFF48FB1, 0xFFCE93D8, 0xFF90CAF9, 0xFFA5D6A7, 0xFFCFD8DC,
).map { it.toInt() }

fun Note.displayTitle(): String = title.trim().ifEmpty { "Untitled note" }

/** What a folded card says, such as "3 of 5 ticked · 2 pictures"; blank lines are not counted. */
fun Note.summary(): String {
    val written = lines.filter { it.text.isNotBlank() }
    val parts = buildList {
        if (written.isNotEmpty()) add("${written.count { it.ticked }} of ${written.size} ticked")
        if (pictures.isNotEmpty()) add(if (pictures.size == 1) "1 picture" else "${pictures.size} pictures")
    }
    return parts.joinToString(" · ").ifEmpty { "Empty" }
}

/** The note as plain text for sharing: the title, then each written line as ☐ or ☑ and its text. */
fun Note.shareText(): String =
    (listOf(displayTitle()) + lines.filter { it.text.isNotBlank() }.map { (if (it.ticked) "☑ " else "☐ ") + it.text })
        .joinToString("\n")

fun Notes.added(note: Note): Notes = copy(list = listOf(note) + list)

fun Notes.changed(id: String, f: (Note) -> Note): Notes = copy(list = list.map { if (it.id == id) f(it) else it })

fun Notes.movedUp(id: String): Notes = moved(id, -1)

fun Notes.movedDown(id: String): Notes = moved(id, 1)

private fun Notes.moved(id: String, by: Int): Notes {
    val i = list.indexOfFirst { it.id == id }
    val j = i + by
    if (i < 0 || j !in list.indices) return this
    return copy(list = list.toMutableList().also { val n = it.removeAt(i); it.add(j, n) })
}

fun Notes.deleted(id: String): Notes = copy(list = list.filter { it.id != id })

/**
 * The invisible first character of every line's text field. Phone keyboards don't report a delete pressed at the
 * start of a field, but they do delete this character, which tells the note to join the line to the one above.
 */
const val LINE_MARK = '​'

/** A note after editing one line, and the line that should now have the cursor. */
data class LineEdit(val note: Note, val focus: Int)

/**
 * Applies what the field of line [index] now holds ([raw], normally starting with [LINE_MARK]):
 * no mark means delete at the start (the line joins the one above); a newline splits the line; else the text changes.
 */
fun Note.editLine(index: Int, raw: String): LineEdit {
    val shown = lines.ifEmpty { listOf(NoteLine()) }
    if (index !in shown.indices) return LineEdit(this, 0)
    val line = shown[index]
    if (!raw.startsWith(LINE_MARK)) {
        if (index == 0) return LineEdit(copy(lines = shown), 0)
        val rest = raw.replace(LINE_MARK.toString(), "").replace("\n", "")
        val above = shown[index - 1]
        val joined = shown.toMutableList().apply {
            set(index - 1, above.copy(text = above.text + rest))
            removeAt(index)
        }
        return LineEdit(copy(lines = joined), index - 1)
    }
    val body = raw.drop(1).replace(LINE_MARK.toString(), "")
    val parts = body.split('\n')
    val out = shown.toMutableList()
    out[index] = line.copy(text = parts[0])
    out.addAll(index + 1, parts.drop(1).map { NoteLine(it) })
    return LineEdit(copy(lines = out), index + parts.size - 1)
}
