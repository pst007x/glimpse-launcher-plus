package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.IconChoice
import com.glimpsemedia.glimpselauncher.model.Tag
import com.glimpsemedia.glimpselauncher.model.applyExtraKey
import com.glimpsemedia.glimpselauncher.model.changedPacks
import com.glimpsemedia.glimpselauncher.model.filesToDropOnChoice
import com.glimpsemedia.glimpselauncher.model.firstDrawableFor
import com.glimpsemedia.glimpselauncher.model.showFindPacks
import com.glimpsemedia.glimpselauncher.model.staleVersionDirs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconReviewFixesTest {
    // 1, 2: a pack chosen with a pack's own Apply is handed over inside Glimpse once, and only if it is a pack.
    @Test fun pendingPackIsTakenOnceAndChecked() {
        PendingIconPack.offer("website.leifs.delta")
        assertEquals("website.leifs.delta", PendingIconPack.take { it == "website.leifs.delta" })
        assertNull(PendingIconPack.take { true })
        PendingIconPack.offer("com.evil.app")
        assertNull(PendingIconPack.take { it == "website.leifs.delta" })
    }

    // 3: only the one known extra of each apply action is read.
    @Test fun applyReadsOnlyItsKnownExtra() {
        assertEquals("org.adw.launcher.theme.NAME", applyExtraKey("org.adw.launcher.SET_THEME"))
        assertNull(applyExtraKey("android.intent.action.VIEW"))
        assertNull(applyExtraKey(null))
    }

    // 4: changing a pick drops its saved copy (and an old picture).
    @Test fun changingAPickDropsItsCopy() {
        assertEquals(setOf("copy-k.png"), filesToDropOnChoice("copy-k.png", IconChoice.Pack("a", "x"), IconChoice.Pack("b", "x")).toSet())
        assertEquals(setOf("copy-k.png", "old.png"), filesToDropOnChoice("copy-k.png", IconChoice.Picture("old.png"), IconChoice.Pack("b", "x")).toSet())
        assertEquals(emptyList<String>(), filesToDropOnChoice("copy-k.png", IconChoice.Pack("a", "x"), IconChoice.Pack("a", "x")))
    }

    // 5: an app's own update, or a different drawing for it, gives a new cache name.
    @Test fun cacheNameCarriesAppStampAndDrawing() {
        val a = iconCacheName("glimpse:colour", 17, "com.a/.A", null, stamp = 100, variant = "globe")
        assertTrue(a != iconCacheName("glimpse:colour", 17, "com.a/.A", null, stamp = 101, variant = "globe"))
        assertTrue(a != iconCacheName("glimpse:colour", 17, "com.a/.A", null, stamp = 100, variant = "tile"))
    }

    // 6: packs whose version changed since last seen.
    @Test fun changedPacksFound() {
        assertEquals(setOf("p"), changedPacks(mapOf("p" to 1L, "q" to 5L)) { if (it == "p") 2L else 5L })
        assertEquals(emptySet<String>(), changedPacks(emptyMap()) { 1L })
    }

    // 8: the picker's suggestion reads only until the first match.
    @Test fun firstDrawableStopsAtTheMatch() {
        val tags = sequence {
            yield(Tag("item", mapOf("component" to "ComponentInfo{com.a/com.a.Main}", "drawable" to "a")))
            yield(Tag("item", mapOf("component" to "ComponentInfo{com.b/com.b.Main}", "drawable" to "b")))
            error("read past the match")
        }
        assertEquals("b", firstDrawableFor(tags, "com.b/com.b.Main"))
        assertEquals("a", firstDrawableFor(tags.take(1), "com.a/com.a.Other"))   // same package, no exact match
    }

    // 9: older version folders of a pack go.
    @Test fun staleVersionFoldersFound() =
        assertEquals(listOf("p/1", "p/2", "q/7"), staleVersionDirs(mapOf("p" to listOf("1", "2", "3"), "q" to listOf("7")), mapOf("p" to 3L)))

    // 10: drawing an icon can never crash the launcher.
    @Test fun iconMakingIsSafe() {
        assertNull(safely { throw IllegalStateException("bad drawable") })
        assertNull(safely { throw OutOfMemoryError("huge") })
        assertEquals(1, safely { 1 })
    }

    // 11: Find icon packs shows while no pack besides Glimpse Colour is installed.
    @Test fun findPacksWhenOnlyTheBuiltIn() {
        assertTrue(showFindPacks(listOf("glimpse:colour")))
        assertFalse(showFindPacks(listOf("glimpse:colour", "website.leifs.delta")))
    }
}
