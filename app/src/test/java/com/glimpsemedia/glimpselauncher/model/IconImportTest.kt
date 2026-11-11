package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.PACK_ACTIONS
import com.glimpsemedia.glimpselauncher.data.PACK_CATEGORIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconImportTest {
    // ---- the signals packs announce themselves with ----

    @Test fun newerLauncherSignalsAreAsked() {
        assertTrue("ch.deletescape.lawnchair.ICONPACK" in PACK_ACTIONS)
        assertTrue("net.oneplus.launcher.icons.ACTION_PICK_ICON" in PACK_ACTIONS)
        assertTrue("com.dlto.atom.launcher.THEME" in PACK_ACTIONS)
        assertTrue("com.fede.launcher.THEME_ICONPACK" in PACK_CATEGORIES)
        assertTrue("com.anddoes.launcher.THEME" in PACK_CATEGORIES)
    }

    // ---- Import: apps found with an appfilter ----

    @Test fun foundAreThePacksNotListedYet() {
        assertEquals(listOf("a.pack", "c.pack"), newlyFound(listOf("c.pack", "b.pack", "a.pack", "a.pack"), setOf("b.pack")))
    }

    @Test fun glimpseColourIsNeverFound() {
        assertEquals(emptyList<String>(), newlyFound(listOf(GLIMPSE_COLOUR), emptySet()))
    }

    @Test fun importedPacksJoinTheListWhileInstalled() {
        val installed = setOf("x.pack", "y.pack")
        assertEquals(listOf("x.pack"), importedStillThere(listOf("x.pack", "gone.pack", "y.pack"), setOf("y.pack")) { it in installed })
    }

    @Test fun importedListRoundTripsAndDropsBadNames() {
        assertEquals(listOf("a.b", "c_d.e2"), decodeImported(encodeImported(listOf("a.b", "c_d.e2"))))
        assertEquals(listOf("ok.pack"), decodeImported("ok.pack\n../x\n\nbad name\nok.pack"))
        assertEquals(emptyList<String>(), decodeImported(null))
    }

    @Test fun importSaysWhatItFound() {
        assertEquals("No more icon packs found", importMessage(emptyList()))
        assertEquals("Found 1 more icon pack: Lines", importMessage(listOf("Lines")))
        assertEquals("Found 2 more icon packs: Lines, Delta", importMessage(listOf("Lines", "Delta")))
    }

    // ---- Apply to every icon ----

    @Test fun applyAllShowsOnlyWithSingleChanges() {
        assertNull(applyAllLabel(0))
        assertEquals("Apply the pack to every icon (1 changed one by one)", applyAllLabel(1))
        assertEquals("Apply the pack to every icon (12 changed one by one)", applyAllLabel(12))
    }

    @Test fun applyAllWarnsFirst() {
        assertEquals(
            "This clears the 12 icons you changed one by one (and any pictures you chose for them), so every icon comes from Delta. This can't be undone.",
            applyAllWarning(12, "Delta"),
        )
        assertEquals(
            "This clears the icon you changed one by one (and any picture you chose for it), so every icon comes from Delta. This can't be undone.",
            applyAllWarning(1, "Delta"),
        )
        assertEquals(
            "This clears the 3 icons you changed one by one (and any pictures you chose for them), so every icon is the app's own. This can't be undone.",
            applyAllWarning(3, null),
        )
    }
}
