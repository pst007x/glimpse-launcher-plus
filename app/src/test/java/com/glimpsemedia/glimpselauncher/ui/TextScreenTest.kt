package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.model.Page
import com.glimpsemedia.glimpselauncher.model.TextLayout
import com.glimpsemedia.glimpselauncher.model.shaded
import com.glimpsemedia.glimpselauncher.model.startingTextLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextScreenTest {
    private val tiles = Layout(listOf(Page()), emptyList())

    @Test fun turningTextModeOnStartsTheLayoutOnceAndLeavesTheTilesAlone() {
        val savedText = mutableListOf<TextLayout>()
        val savedTiles = mutableListOf<Layout>()
        val st = LauncherState(tiles, Settings(), { savedTiles += it }, {}, textLayout = null, saveTextLayout = { savedText += it })
        st.startingTextLayoutFor = { startingTextLayout("d/D", null, null, null) }
        st.change(st.settings.copy(textMode = true))
        assertEquals(listOf("d/D"), st.textLayout!!.apps)
        st.change(st.settings.copy(textMode = false))
        st.change(st.settings.copy(textMode = true))
        assertEquals(1, savedText.size)
        assertEquals(tiles, st.layout)
        assertTrue(savedTiles.isEmpty())
    }

    @Test fun aSavedTextLayoutIsKeptWhenTextModeTurnsOn() {
        val mine = TextLayout(listOf("x/X"))
        val st = LauncherState(tiles, Settings(), {}, {}, textLayout = mine)
        st.startingTextLayoutFor = { startingTextLayout("d/D", null, null, null) }
        st.change(st.settings.copy(textMode = true))
        assertEquals(mine, st.textLayout)
    }

    @Test fun textInkFollowsTheClockColour() {
        assertEquals(0xFFFFFFFF.toInt(), textInk(0, 0, dark = true))
        assertEquals(0xFF101820.toInt(), textInk(0, 0, dark = false))
        assertEquals(0xFFFF0000.toInt(), textInk(0xFFFF0000.toInt(), 0, dark = false))
        assertEquals(shaded(0xFF101820.toInt(), 30), textInk(0, 30, dark = false))
    }

    @Test fun textSizes() {
        assertEquals(listOf(18, 22, 28), listOf(-1, 0, 2).map { textStepSp(it) })
        assertEquals(33, textStepSp(9))   // past the top: the largest, +3
    }

    @Test fun folderNamesAreTidied() {
        assertEquals("Music", cleanTextFolderName("  Music "))
        assertEquals(null, cleanTextFolderName("   "))
        assertEquals(30, cleanTextFolderName("x".repeat(40))!!.length)
    }

    @Test fun allAppsAddsToTheTextListNotTheTilesInTextMode() {
        val savedTiles = mutableListOf<Layout>()
        val st = LauncherState(tiles, Settings(textMode = true), { savedTiles += it }, {}, textLayout = TextLayout(listOf("a/A")))
        assertTrue(st.addToTextList("b/B"))
        assertEquals(false, st.addToTextList("b/B"))
        assertEquals(listOf("a/A", "b/B"), st.textLayout!!.apps)
        assertTrue(savedTiles.isEmpty())
    }

    @Test fun haloIsTheOppositeOfItsOwnInk() {
        assertEquals(androidx.compose.ui.graphics.Color.Black, haloFor(androidx.compose.ui.graphics.Color.White))
        assertEquals(androidx.compose.ui.graphics.Color.White, haloFor(androidx.compose.ui.graphics.Color(TEXT_DARK_INK)))
    }

    @Test fun noStartingLayoutBeforeTheAppsAreKnown() {
        val st = LauncherState(tiles, Settings(), {}, {}, textLayout = null)
        st.change(st.settings.copy(textMode = true))   // e.g. the switch widget starting Glimpse cold
        assertEquals(null, st.textLayout)
        st.startingTextLayoutFor = { startingTextLayout("d/D", null, null, null) }
        st.ensureTextLayout()
        assertEquals(listOf("d/D"), st.textLayout!!.apps)
    }

    @Test fun aChosenTextColourWinsOverAutomatic() {
        assertEquals(0xFFFFFFFF.toInt(), nameInk(0, 0, 0xFFFFFFFF.toInt()))
        assertEquals(0xFF2962FF.toInt(), nameInk(0xFF2962FF.toInt(), 0, 0xFFFFFFFF.toInt()))
        assertEquals(shaded(0xFF2962FF.toInt(), 20), nameInk(0xFF2962FF.toInt(), 20, 0xFFFFFFFF.toInt()))
    }

    @Test fun aFolderFollowsTheTextColourUnlessItHasItsOwn() {
        assertEquals(0xFF2962FF.toInt(), folderInk(0, 0xFF2962FF.toInt()))
        assertEquals(0xFFFF40A0.toInt(), folderInk(0xFFFF40A0.toInt(), 0xFF2962FF.toInt()))
    }
}
