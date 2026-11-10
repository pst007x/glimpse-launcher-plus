package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SavedThemesTest {
    @get:Rule val tmp = TemporaryFolder()

    private val custom = Settings().withTheme(THEME_PRESETS.first { it.name == "Neon" })
        .copy(highlightColor = 0xFFCD2026.toInt(), tileShade = 15, clockShade = -10, barTransparency = 25, iconTransparency = 50)

    @Test fun aLookSavesAndRestoresExactlyTheThemedSettings() {
        val look = custom.look()
        val other = Settings(showClock = false, autoLockMinutes = 30, clockStyle = 2, showTileNames = false)
        val applied = other.withLook(look)
        assertEquals(look, applied.look())
        // Everything that isn't the look is kept.
        assertEquals(false, applied.showClock); assertEquals(30, applied.autoLockMinutes)
        assertEquals(2, applied.clockStyle); assertEquals(false, applied.showTileNames)
        assertEquals(0xFFCD2026.toInt(), applied.highlightColor); assertEquals(15, applied.tileShade); assertEquals(25, applied.barTransparency)
    }

    @Test fun namesCountUpAndSkipTakenOnes() {
        assertEquals("My theme 1", nextThemeName(emptyList()))
        assertEquals("My theme 3", nextThemeName(listOf("My theme 1", "My theme 2")))
        assertEquals("My theme 2", nextThemeName(listOf("My theme 1", "My theme 3")))
        assertEquals("Beach", cleanThemeName("  Beach  "))
        assertNull(cleanThemeName("   "))
        assertEquals(30, cleanThemeName("x".repeat(80))!!.length)
    }

    @Test fun savedAndReadBack() {
        val store = ThemeStore(tmp.root)
        val pic = store.newWallpaperFile().apply { writeText("jpeg") }
        val themes = SavedThemes(
            list = listOf(
                SavedTheme("t1", "Night \"café\" ☕", custom.look(),
                    home = SavedWallpaper.Drawn(1, 2, 3, 4, true), lock = SavedWallpaper.Picture(pic.name)),
                SavedTheme("t2", "Plain", Settings().look(), home = null, lock = null),
            ),
            lastHome = GlimpseWallpaper(SavedWallpaper.Picture(pic.name), 41),
            lastLock = null,
        )
        store.save(themes)
        assertEquals(themes, ThemeStore(tmp.root).load())
    }

    @Test fun anUnreadableFileIsKeptAsideAndItsPicturesStay() {
        val store = ThemeStore(tmp.root)
        val pic = store.newWallpaperFile().apply { writeText("jpeg") }
        File(tmp.root, "themes.json").writeText("{ broken")
        assertEquals(SavedThemes(), store.load())
        assertTrue(File(tmp.root, "themes.json.bad").exists())
        assertEquals(0, store.removeUnusedWallpapersIfSafe(SavedThemes()))
        assertTrue(pic.exists())
    }

    @Test fun unusedWallpaperFilesGo() {
        val store = ThemeStore(tmp.root)
        val keep = store.newWallpaperFile().apply { writeText("a") }
        val last = store.newWallpaperFile().apply { writeText("b") }
        val old = store.newWallpaperFile().apply { writeText("c") }
        val t = SavedThemes(
            list = listOf(SavedTheme("t", "T", Settings().look(), home = SavedWallpaper.Picture(keep.name), lock = null)),
            lastHome = null, lastLock = GlimpseWallpaper(SavedWallpaper.Picture(last.name), 9),
        )
        store.load()
        assertEquals(1, store.removeUnusedWallpapersIfSafe(t))
        assertTrue(keep.exists()); assertTrue(last.exists()); assertFalse(old.exists())
    }

    @Test fun onlyWallpapersStillSetByGlimpseAreSaved() {
        val drawn = SavedWallpaper.Drawn(1, 2, 3, 4, false)
        val t = SavedThemes(lastHome = GlimpseWallpaper(drawn, 10), lastLock = GlimpseWallpaper(drawn, 20))
        // Both still the ones Glimpse set.
        assertEquals(drawn to drawn, t.wallpapersNow(homeId = 10, lockId = 20))
        // The lock screen was changed elsewhere since.
        assertEquals(drawn to null, t.wallpapersNow(homeId = 10, lockId = 99))
        // Neither.
        assertEquals(null to null, t.wallpapersNow(homeId = 5, lockId = 6))
        assertEquals(null to null, SavedThemes().wallpapersNow(homeId = 10, lockId = 20))
    }

    @Test fun picturesWithPathsAreNotTrusted() {
        val text = """{"v":1,"themes":[{"id":"t","n":"T","look":{},"home":{"k":"pic","f":"../x.jpg"}}]}"""
        assertNull(SavedThemesJson.decode(text)!!.list[0].home)
    }

    @Test fun cardColoursAreTheLooksFourColours() {
        assertEquals(custom.currentThemeColours(), SavedTheme("t", "T", custom.look(), null, null).cardColours())
    }
}
