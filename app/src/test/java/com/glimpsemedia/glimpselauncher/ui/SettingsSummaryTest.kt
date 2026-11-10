package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.data.SavedTheme
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.THEME_PRESETS
import com.glimpsemedia.glimpselauncher.data.look
import com.glimpsemedia.glimpselauncher.data.withTheme
import com.glimpsemedia.glimpselauncher.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class SettingsSummaryTest {
    private fun facts(s: Settings, themeName: String? = null, columns: Int = 2, rows: Int = 4) =
        SummaryFacts(s, columns = columns, themeName = themeName, isDefaultHome = true, versionName = "1.0.5", zone = ZoneId.of("Europe/London"), rows = rows)

    @Test fun homeGroup() {
        assertEquals("2×4 grid · Clock on · Text mode off", groupSummary("home", facts(Settings())))
    }

    @Test fun clockOffAndTextOn() {
        assertEquals("Off in icon view", sectionSummary("clock", facts(Settings(showClock = false))))
        assertEquals("On · text size +2", sectionSummary("text", facts(Settings(textMode = true, textStep = 2))))
    }

    @Test fun lockAndPin() {
        assertEquals("PIN off · lock after 10 min", sectionSummary("lock", facts(Settings(autoLockMinutes = 10))))
        assertEquals("PIN on · lock after: Off", sectionSummary("lock", facts(Settings(pinHash = "x", autoLockMinutes = 0))))
    }

    @Test fun emergency() {
        assertEquals("Not set", sectionSummary("emergency", facts(Settings())))
        assertEquals("Mum", sectionSummary("emergency", facts(Settings(emergencyEnabled = true, emergencyContact = Contact("Mum", "0123456")))))
        assertEquals("0123456", sectionSummary("emergency", facts(Settings(emergencyEnabled = true, emergencyContact = Contact("", "0123456")))))
        assertEquals("Not set", sectionSummary("emergency", facts(Settings(emergencyEnabled = true, emergencyContact = Contact("Mum", "12")))))
    }

    @Test fun backupDates() {
        assertEquals("No backup yet", groupSummary("setup", facts(Settings())))
        assertEquals("Last backup 8 Nov 2026", sectionSummary("backup", facts(Settings(lastBackupMillis = 1_794_168_000_000L))))
    }

    @Test fun customColours() {
        assertEquals("Custom tiles · names 0, icons 0", sectionSummary("tiles", facts(Settings(tileColor = 0xFF112233.toInt()))))
        assertEquals("Standard tiles · names +1, icons -2", sectionSummary("tiles", facts(Settings(tileNameStep = 1, tileIconStep = -2))))
        assertEquals("Dark menus · Custom highlight", sectionSummary("menus", facts(Settings(highlightColor = 0xFFAA0000.toInt()))))
    }

    @Test fun themeNameOf() {
        val preset = Settings().withTheme(THEME_PRESETS[0])
        assertEquals(THEME_PRESETS[0].name, themeNameOf(preset, emptyList()))
        val mine = Settings(tileColor = 0xFF123456.toInt())
        val saved = SavedTheme("t1", "Mine", mine.look(), null, null)
        assertEquals("Mine", themeNameOf(mine, listOf(saved)))
        assertNull(themeNameOf(mine, emptyList()))
        assertEquals("Custom · Dark menus", groupSummary("colours", facts(mine, themeName = null)))
    }

    @Test fun tilesOffAndTightGrids() {
        assertEquals("5×6 grid · Free layout off · No tiles", sectionSummary("screens", facts(Settings(showTiles = false), columns = 5, rows = 6)))
        assertEquals("No tiles · names 0, icons 0", sectionSummary("tiles", facts(Settings(showTiles = false))))
        assertEquals(true, groupSummary("home", facts(Settings(showTiles = false), columns = 4, rows = 5)).startsWith("4×5 grid"))
        assertEquals("4×4 grid · Free layout off", sectionSummary("screens", facts(Settings(), columns = 4, rows = 4)))
    }

    @Test fun searchSummary() {
        assertEquals("Apps, menus and help", sectionSummary("search", facts(Settings())))
        assertEquals("Apps, menus and help · folders · web", sectionSummary("search", facts(Settings(searchFolders = true, searchEngines = true))))
        // All files and folders replaces the picked folders, in Plus; the Play edition ignores it.
        val allFiles = sectionSummary("search", facts(Settings(allFilesSearch = true, searchFolders = true)))
        assertEquals(if (com.glimpsemedia.glimpselauncher.BuildConfig.PLUS) "Apps, menus and help · all files" else "Apps, menus and help · folders", allFiles)
        val notEnabled = sectionSummary("search", facts(Settings(allFilesSearch = true)).copy(allFilesPermitted = false))
        assertEquals(if (com.glimpsemedia.glimpselauncher.BuildConfig.PLUS) "Apps, menus and help · all files (not enabled)" else "Apps, menus and help", notEnabled)
    }
}
