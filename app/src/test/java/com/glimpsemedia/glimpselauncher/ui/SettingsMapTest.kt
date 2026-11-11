package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMapTest {
    @Test fun everySectionIsInExactlyOneGroup() {
        val inGroups = SettingsMap.groups.flatMap { it.sections }
        assertEquals(inGroups.size, inGroups.toSet().size)
        assertEquals(SettingsMap.sections.keys, inGroups.toSet())
        assertEquals(18, inGroups.size)
    }

    @Test fun sectionsWithoutBlocksOpenElsewhere() {
        val noBlocks = SettingsMap.sections.values.filter { it.blocks.isEmpty() }.map { it.id }.toSet()
        assertEquals(setOf("help", "about"), noBlocks)
        assertEquals(noBlocks, SettingsMap.sections.values.filter { it.opensElsewhere }.map { it.id }.toSet())
    }

    @Test fun everyHitPointsToARealRow() {
        val hits = SettingsMap.search("e", 1000)
        assertTrue(hits.size > 40)
        for (h in hits) {
            val sec = SettingsMap.sections.getValue(h.section)
            assertTrue(h.toString(), h.row == null || sec.blocks.any { h.row in it.rows })
        }
    }

    @Test fun pinFindsThePinSettings() {
        val labels = SettingsMap.search("pin").map { it.label }
        assertTrue(labels.containsAll(listOf("Set a PIN", "Change the PIN", "Remove the PIN", "Notes need the PIN", "Locking and PIN")))
    }

    @Test fun searchIgnoresCaseAndSpaces() {
        val hits = SettingsMap.search("  CLOCK ")
        assertTrue(SettingsHit("Clock", "clock", null) in hits)
        assertTrue(SettingsHit("Clock size", "clock", "Clock size") in hits)
    }

    @Test fun noMatchIsEmpty() {
        assertEquals(emptyList<SettingsHit>(), SettingsMap.search("zzz"))
        assertEquals(emptyList<SettingsHit>(), SettingsMap.search("   "))
    }

    @Test fun atMostTwelve() {
        assertEquals(12, SettingsMap.search("the").size)
    }

    @Test fun hiddenRowsAreStillSearchable() {
        assertEquals(SettingsHit("Remove the PIN", "lock", "Remove the PIN"), SettingsMap.search("Remove the PIN").single())
    }

    @Test fun rowNamesAreUnique() {
        // The pages draw each setting by its name alone, so no two may share one.
        val rows = SettingsMap.sections.values.flatMap { sec -> sec.blocks.flatMap { it.rows } }
        assertEquals(rows.groupBy { it }.filterValues { it.size > 1 }.keys, emptySet<String>())
    }

    @Test fun aPageAndASettingWithTheSameNameAreListedOnce() {
        // "Themes" is both a page and the setting on it: the search lists it once.
        assertEquals(1, SettingsMap.search("themes").count { it.label == "Themes" })
        val all = SettingsMap.search("e", 1000)
        assertEquals(all.map { it.label to it.section }.distinct().size, all.size)
    }

    @Test fun coffeeIsACardOnTheFirstPageNotAPage() {
        // Buy me a coffee sits on the first Settings page itself, under the version line.
        assertEquals(listOf("help", "about", "bugs"), SettingsMap.groups.last().sections)
        assertEquals(emptyList<SettingsHit>(), SettingsMap.search("coffee"))
    }

    @Test fun tilesAndShapeAreSearchable() {
        assertTrue(SettingsHit("Tiles behind the icons", "screens", "Tiles behind the icons") in SettingsMap.search("tiles"))
        val shapes = SettingsMap.search("shape").filter { it.section == "tiles" }.map { it.label }
        assertTrue(shapes.containsAll(listOf("Tile shape", "Icon background shape", "Empty place shape")))
    }

    @Test fun shapesSitTogetherAtTheTopOfTilesAndIcons() {
        val first = SettingsMap.sections.getValue("tiles").blocks.first()
        assertEquals("Shapes", first.heading)
        assertEquals(listOf("Shapes preview", "Tile shape", "Icon background shape", "Empty place shape"), first.rows)
        assertTrue(SettingsMap.search("icon background").any { it.label == "Icon background colour" })
        assertTrue(SettingsMap.search("tile colour").any { it.label == "Tile colour" })
    }

    @Test fun groupOfFindsTheGroup() {
        assertEquals("Home screen", SettingsMap.groupOf("clock").title)
    }

    @Test fun sizeSlidersAreSearchable() {
        assertEquals(listOf("Name size on the tiles", "Icon size on the tiles"),
            SettingsMap.sections.getValue("tiles").blocks.first { it.heading == "Sizes" }.rows)
        assertEquals(true, SettingsMap.search("icon size").any { it.label == "Icon size on the tiles" })
    }

    @Test fun searchFindsSearchRows() {
        for (row in listOf("Search icon", "Hold All apps to search", "Add folder location(s)", "Add search engine(s)")) {
            assertTrue(row, SettingsMap.search(row).any { it.row == row && it.section == "search" })
        }
        assertTrue(SettingsMap.search("search").any { it.row == null && it.section == "search" })
    }

    @Test fun searchPageInHomeGroupAfterBar() {
        assertEquals(listOf("screens", "bar", "search", "clock", "text"), SettingsMap.groupOf("search").sections)
    }

    @Test fun allFilesRowOnlyInPlus() {
        assertEquals(listOf("All files and folders", "Add folder location(s)", "Add search engine(s)"), searchSectionRows(plus = true).first { it.heading == "What is searched" }.rows)
        assertEquals(listOf("Add folder location(s)", "Add search engine(s)"), searchSectionRows(plus = false).first { it.heading == "What is searched" }.rows)
    }

    @Test fun iconPackIsFound() {
        assertTrue(SettingsMap.search("icon pack").any { it.label == "Icon pack" })
        assertEquals(listOf("Icon pack"), SettingsMap.sections.getValue("tiles").blocks.first { it.heading == "Icon pack" }.rows)
    }

    @Test fun screenChangeSitsWithTheWallpapers() {
        assertEquals(listOf("Change the wallpaper", "A different wallpaper for each screen", "Screen change", "Full screen: hide the top bar"),
            SettingsMap.sections.getValue("screens").blocks.first { it.heading == "Screen" }.rows)
        assertTrue(SettingsMap.search("screen change").any { it.label == "Screen change" })
    }
}
