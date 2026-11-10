package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderWalkTest {
    private fun dir(id: String, path: String = "") = Entry(id.substringAfterLast('/'), true, id, path)
    private fun file(id: String, path: String = "") = Entry(id.substringAfterLast('/'), false, id, path)

    /** A tree held in a map: folder id → its children. */
    private class MapTree(val m: Map<String, List<Entry>>, val failing: Set<String> = emptySet()) : Tree {
        override fun children(id: String): List<Entry> {
            if (id in failing) throw SecurityException("no grant")
            return m[id].orEmpty()
        }
    }

    @Test fun walkFindsNestedNames() {
        val tree = MapTree(mapOf("D" to listOf(dir("D/Recipes"), file("D/a.txt")), "D/Recipes" to listOf(file("D/Recipes/soup.pdf"))))
        val r = walk(listOf(dir("D")), tree)
        assertEquals(setOf("D", "Recipes", "a.txt", "soup.pdf"), r.entries.map { it.name }.toSet())
        assertFalse(r.cut)
    }

    @Test fun walkSkipsHidden() {
        val tree = MapTree(mapOf("D" to listOf(dir("D/.thumbs"), file("D/.nomedia"), file("D/b.txt")), "D/.thumbs" to listOf(file("D/.thumbs/x.jpg"))))
        assertEquals(listOf("D", "b.txt"), walk(listOf(dir("D")), tree).entries.map { it.name })
    }

    @Test fun walkStopsAtLimit() {
        val tree = MapTree(mapOf("D" to (1..20).map { file("D/f$it") }))
        val r = walk(listOf(dir("D")), tree, maxEntries = 5)
        assertEquals(5, r.entries.size)
        assertTrue(r.cut)
    }

    @Test fun walkStopsAtDepth() {
        // D/1/2/3/...: with depth 2, D's children (depth 1) and theirs (depth 2) are listed, no deeper.
        val m = (0 until 6).associate { i -> (if (i == 0) "D" else "D/$i") to listOf(dir("D/${i + 1}")) }
        val r = walk(listOf(dir("D")), MapTree(m), maxDepth = 2)
        assertEquals(listOf("D", "1", "2"), r.entries.map { it.name })
        assertTrue(r.cut)
    }

    @Test fun walkIgnoresRepeatedIds() {
        val tree = MapTree(mapOf("D" to listOf(dir("D/loop")), "D/loop" to listOf(dir("D"), file("D/loop/x"))))
        val r = walk(listOf(dir("D")), tree)
        assertEquals(listOf("D", "loop", "x"), r.entries.map { it.name })
    }

    @Test fun walkSkipsFailingRoot() {
        val tree = MapTree(mapOf("B" to listOf(file("B/ok.txt"))), failing = setOf("A"))
        val r = walk(listOf(dir("A"), dir("B")), tree)
        assertEquals(setOf("A"), r.failedRoots)
        assertEquals(listOf("B", "ok.txt"), r.entries.map { it.name })
    }
}

class FolderWalkCancelTest {
    @Test fun walkStopsWhenCancelled() {
        var reads = 0
        val tree = object : Tree {
            override fun children(id: String): List<Entry> { reads++; return (1..3).map { Entry("d$id$it", true, "$id/$it", "") } }
        }
        val r = walk(listOf(Entry("D", true, "D", "")), tree, cancelled = { reads >= 3 })
        assertEquals(3, reads)
        assertTrue(r.cut)
    }
}

class FolderWalkTidyTest {
    private fun dir(id: String) = Entry(id.substringAfterLast('/'), true, id, "")
    private fun file(id: String) = Entry(id.substringAfterLast('/'), false, id, "")
    private class MapTree(val m: Map<String, List<Entry>>) : Tree { override fun children(id: String) = m[id].orEmpty() }

    @Test fun depthLimitStillReadsTheOtherFolders() {
        // D holds Deep (too deep below it) and Wide, whose file is at depth 2: with maxDepth 2 Wide's file is still found.
        val tree = MapTree(mapOf(
            "D" to listOf(dir("D/Deep"), dir("D/Wide")),
            "D/Deep" to listOf(dir("D/Deep/More")),
            "D/Wide" to listOf(file("D/Wide/w.txt")),
        ))
        val r = walk(listOf(dir("D")), tree, maxDepth = 2)
        assertTrue(r.entries.any { it.name == "w.txt" })
        assertTrue(r.cut)
    }

    @Test fun nestedPickedFoldersListedOnce() {
        val tree = MapTree(mapOf("D" to listOf(dir("D/R")), "D/R" to listOf(file("D/R/soup.txt"))))
        val seen = HashSet<String>()
        val a = walk(listOf(dir("D")), tree, seen = seen)
        val b = walk(listOf(dir("D/R")), tree, seen = seen)
        assertEquals(1, (a.entries + b.entries).count { it.name == "soup.txt" })
        assertEquals(1, (a.entries + b.entries).count { it.id == "D/R" })
    }
}
