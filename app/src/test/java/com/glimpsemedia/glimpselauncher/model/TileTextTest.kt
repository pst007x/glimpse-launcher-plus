package com.glimpsemedia.glimpselauncher.model

import androidx.compose.ui.graphics.Color
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.ui.tileNameInk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileTextTest {
    private val app = Item.App("a/A")
    private val folder = Item.Folder("f1", "Games", listOf("b/B"))
    private val l = Layout(listOf(Page(listOf(app, folder))), emptyList())
    private val yellow = 0xFFFFC400.toInt()
    private val blue = 0xFF2962FF.toInt()

    @Test fun aTileAndAFolderKeepTheirOwnTextColour() {
        val c = l.withTextColor(app, yellow).withTextShade(app, 20).withTextColor(folder, blue)
        assertEquals(yellow, c.textColors[app.colorKey]); assertEquals(20, c.textShades[app.colorKey]); assertEquals(blue, c.textColors[folder.colorKey])
        assertEquals(c, LayoutJson.decode(LayoutJson.encode(c)))
        val back = c.withTextColor(app, 0)
        assertNull(back.textColors[app.colorKey]); assertNull(back.textShades[app.colorKey])   // Automatic clears the shade too
    }

    @Test fun aRemovedTileTakesItsTextColourWithIt() {
        val c = l.withTextColor(app, yellow).withTextShade(app, 10)
        val gone = c.copy(pages = listOf(Page(listOf(folder)))).withoutStrayColors()
        assertNull(gone.textColors[app.colorKey]); assertNull(gone.textShades[app.colorKey])
    }

    @Test fun olderLayoutsHaveNoTextColours() {
        val old = """{"v":1,"pages":[[]],"dock":[],"cols":2}"""
        assertEquals(emptyMap<String, Int>(), LayoutJson.decode(old)!!.textColors)
    }

    @Test fun theTilesOwnTextColourWinsThenTheSettingThenAutomatic() {
        val white = Color.White
        assertEquals(Color(yellow), tileNameInk(own = yellow, all = blue, automatic = white))
        assertEquals(Color(blue), tileNameInk(own = 0, all = blue, automatic = white))
        assertEquals(white, tileNameInk(own = 0, all = 0, automatic = white))
    }

    @Test fun nameSizesOnTheTiles() {
        // Now steps of 10% (SizesTest); the old Small, Medium and Large read as −1, 0 and +1.
        assertEquals(0, Settings().tileNameStep)
    }

}
