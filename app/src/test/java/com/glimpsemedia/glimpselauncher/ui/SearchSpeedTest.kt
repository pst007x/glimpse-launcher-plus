package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.WalkResult
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plus can hand Search up to 200,000 names; one typed letter must not take long to rank (it runs as you type). */
class SearchSpeedTest {
    private val words = listOf("Holiday", "notes", "Invoice", "photo", "Recipe", "music", "Report", "draft", "Scan", "backup")
    private val big = WalkResult((0 until 200_000).map { i ->
        val n = "${words[i % 10]} ${words[(i / 10) % 10]} $i" + if (i % 3 == 0) ".txt" else ""
        Entry(n, i % 3 != 0, "/s/$i", "Download", "", "/s")
    }, false, emptySet())
    private val input = SearchInput(emptyList(), big, emptyList(), emptyList(), menu = { emptyList() })

    /** Measured against the plain ranking at the same moment, so a busy computer does not make it fail. */
    @Test fun oneLetterOver200kNamesIsQuick() {
        val letters = listOf("o", "e", "a", "r", "s")
        fun best(run: (String) -> Unit) = letters.minOf { q -> val t = System.nanoTime(); run(q); System.nanoTime() - t }
        val plainRun: (String) -> Unit = { q ->
            com.glimpsemedia.glimpselauncher.model.rankByName(big.entries.filter { !it.isFolder }, q) { it.name }
            com.glimpsemedia.glimpselauncher.model.rankByName(big.entries.filter { it.isFolder }, q) { it.name }
        }
        val quickRun: (String) -> Unit = { q -> buildResults(input, q, emptyMap()) }
        repeat(3) { plainRun("o"); quickRun("o") }   // warm up the JVM
        val plain = best(plainRun); val quick = best(quickRun)
        println("one letter over 200,000 names: plain ${plain / 1_000_000} ms, quick ${quick / 1_000_000} ms")
        assertTrue("quick ${quick / 1_000_000} ms is not well under plain ${plain / 1_000_000} ms", quick * 2 < plain)
    }
}

/** The quick path gives exactly what the plain ranking gives: the same first results, in order, and the same total. */
class SearchSameResultsTest {
    @Test fun quickRankingMatchesThePlainOne() {
        val names = listOf("Notes", "my notes", "Holiday notes.txt", "notebook", "NOTES 2", "Recipes", "keynote", "a", "Note")
        val entries = names.mapIndexed { i, n -> Entry(n, false, "/s/$i", "", "", "/s") }
        val input = SearchInput(emptyList(), WalkResult(entries, false, emptySet()), emptyList(), emptyList(), menu = { emptyList() })
        for (q in listOf("note", "NOTE", " no ", "s", "zzz")) {
            val plain = com.glimpsemedia.glimpselauncher.model.rankByName(entries, q) { it.name }
            val got = buildResults(input, q, emptyMap()).firstOrNull { it.heading == com.glimpsemedia.glimpselauncher.model.SearchHeading.Files }
            org.junit.Assert.assertEquals(q, plain.take(10), got?.shown?.map { (it as SearchHit.File).entry } ?: emptyList<Entry>())
            org.junit.Assert.assertEquals(q, plain.size, got?.total ?: 0)
            val all = buildResults(input, q, mapOf(com.glimpsemedia.glimpselauncher.model.SearchHeading.Files to 1000)).firstOrNull { it.heading == com.glimpsemedia.glimpselauncher.model.SearchHeading.Files }
            org.junit.Assert.assertEquals(q, plain, all?.shown?.map { (it as SearchHit.File).entry } ?: emptyList<Entry>())
        }
    }
}

/** The ready names keep only places in the list, not their own copies of every entry (200,000 in Plus). */
class NameIndexKeepsNoCopiesTest {
    @Test fun entriesAreReadFromTheListWhenShown() {
        var reads = 0
        val backing = (0 until 1000).map { Entry("name $it", it % 2 == 0, "/s/$it", "", "", "/s") }
        val list = object : java.util.AbstractList<Entry>() {
            override val size get() = backing.size
            override fun get(index: Int): Entry { reads++; return backing[index] }
        }
        val index = NameIndex(list)
        reads = 0
        index.files[3]; index.folders[4]
        org.junit.Assert.assertEquals(2, reads)
        org.junit.Assert.assertEquals(500, index.files.size)
        org.junit.Assert.assertEquals("name 7", index.files[3].name)
    }
}
