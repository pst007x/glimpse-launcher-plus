package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconMinorsTest {
    // Picture names from saved (or restored) settings must be plain file names in the picked folder.
    @Test fun pictureNamesArePlain() {
        assertTrue(isPlainFileName("pic-com.a_.A-1.png"))
        for (bad in listOf("", ".", "..", "../x.png", "a/b.png", "a\\b.png", "/etc/x")) assertFalse(bad, isPlainFileName(bad))
    }

    @Test fun unsafePictureChoiceReadsAsNone() {
        val c = parseIconChoices("""{"a":{"picture":"../../shared_prefs/settings.xml"},"b":{"picture":"pic-b.png"}}""")
        assertEquals(mapOf("b" to IconChoice.Picture("pic-b.png")), c)
    }

    // With Glimpse Colour, an app whose own pick cannot be shown (pack gone, no copy) gets Glimpse Colour, not its plain icon.
    @Test fun glimpseColourWhenThePickCannotShow() {
        assertTrue(glimpseColourApplies(GLIMPSE_COLOUR, IconSource.Own))
        assertFalse(glimpseColourApplies(GLIMPSE_COLOUR, IconSource.Picture("p.png")))
        assertFalse(glimpseColourApplies("website.leifs.delta", IconSource.Own))
    }

    // The whole pack's saved copies are made again when the pack updates, never when it is uninstalled.
    @Test fun packCopiesRefreshOnlyOnUpdate() {
        assertTrue(refreshPackCopies(setOf("p"), "p", installed = true))
        assertFalse(refreshPackCopies(setOf("p"), "p", installed = false))
        assertFalse(refreshPackCopies(setOf("q"), "p", installed = true))
    }

    // A photo's EXIF orientation: degrees to turn, and whether to mirror first.
    @Test fun exifOrientation() {
        assertEquals(0 to false, exifTurn(1)); assertEquals(0 to true, exifTurn(2)); assertEquals(180 to false, exifTurn(3))
        assertEquals(180 to true, exifTurn(4)); assertEquals(90 to true, exifTurn(5)); assertEquals(90 to false, exifTurn(6))
        assertEquals(270 to true, exifTurn(7)); assertEquals(270 to false, exifTurn(8)); assertEquals(0 to false, exifTurn(0))
    }
}
