package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    private fun note(vararg l: Pair<String, Boolean>, pics: Int = 0) =
        Note("n", "T", lines = l.map { NoteLine(it.first, it.second) }, pictures = List(pics) { "p$it.jpg" })

    @Test fun summaryTexts() {
        assertEquals("3 of 5 ticked · 2 pictures", note("a" to true, "b" to true, "c" to true, "d" to false, "e" to false, pics = 2).summary())
        assertEquals("0 of 1 ticked · 1 picture", note("a" to false, pics = 1).summary())
        assertEquals("1 picture", note("" to false, pics = 1).summary())
        assertEquals("Empty", note("" to false).summary())
        assertEquals("Untitled note", Note("n", "  ").displayTitle())
    }

    @Test fun shareText() {
        assertEquals("Shopping\n☑ milk\n☐ eggs 🥚", Note("n", "Shopping", lines = listOf(NoteLine("milk", true), NoteLine("eggs 🥚"), NoteLine(""))).shareText())
        assertEquals("Untitled note", Note("n", "").shareText())
    }

    @Test fun newestFirstAndMoving() {
        val n = Notes().added(Note("a")).added(Note("b")).added(Note("c"))
        assertEquals(listOf("c", "b", "a"), n.list.map { it.id })
        assertEquals(listOf("b", "c", "a"), n.movedDown("c").list.map { it.id })
        assertEquals(listOf("c", "a", "b"), n.movedUp("a").list.map { it.id })
        assertEquals(n, n.movedUp("c"))
        assertEquals(n, n.movedDown("a"))
        assertEquals(listOf("c", "a"), n.deleted("b").list.map { it.id })
    }

    @Test fun enterSplitsTheLine() {
        val n = Note("n", lines = listOf(NoteLine("milkeggs", ticked = true, color = 5)))
        val e = n.editLine(0, "${LINE_MARK}milk\neggs")
        assertEquals(listOf(NoteLine("milk", true, 5), NoteLine("eggs")), e.note.lines)
        assertEquals(1, e.focus)
    }

    @Test fun deleteAtTheStartJoinsTheLineAbove() {
        val n = Note("n", lines = listOf(NoteLine("milk"), NoteLine("")))
        val e = n.editLine(1, "")
        assertEquals(listOf(NoteLine("milk")), e.note.lines)
        assertEquals(0, e.focus)
        val first = n.editLine(0, "milk")
        assertEquals(n.lines, first.note.lines)
    }

    @Test fun plainTypingChangesOnlyTheText() {
        val n = Note("n", lines = listOf(NoteLine("a", true, 3)))
        assertEquals(NoteLine("ab", true, 3), n.editLine(0, "${LINE_MARK}ab").note.lines[0])
    }

    @Test fun eightLineColours() { assertEquals(8, NOTE_LINE_COLORS.toSet().size) }

    @Test fun manyLinesAndALongOne() {
        val long = "x".repeat(5000)
        val n = Note("n", "Big", lines = List(200) { NoteLine(if (it == 0) long else "l$it", it % 2 == 0) })
        assertEquals("100 of 200 ticked", n.summary())
        assertEquals(201, n.shareText().lines().size)
    }
}
