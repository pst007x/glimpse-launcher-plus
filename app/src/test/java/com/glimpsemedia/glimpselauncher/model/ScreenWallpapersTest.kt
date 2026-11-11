package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenWallpapersTest {
    private fun pic(screen: Int) = ScreenPicture(screen, "s$screen.jpg", "w$screen.jpg", PicturePlace())

    // ---- which picture each screen shows ----

    @Test fun screenOneAlwaysShowsTheDefault() {
        assertNull(pictureForScreen(1, true, listOf(pic(1), pic(2))))
    }

    @Test fun eachScreenShowsItsOwnNumber() {
        val list = listOf(pic(2), pic(3))
        assertEquals("w2.jpg", pictureForScreen(2, true, list)?.shown)
        assertEquals("w3.jpg", pictureForScreen(3, true, list)?.shown)
    }

    @Test fun aScreenWithoutAPictureShowsTheDefault() {
        assertNull(pictureForScreen(3, true, listOf(pic(2))))
    }

    @Test fun switchOffShowsTheDefaultEverywhereButKeepsTheList() {
        val list = listOf(pic(2))
        assertNull(pictureForScreen(2, false, list))
        assertEquals(1, list.size)
    }

    // ---- Add another screen ----

    @Test fun addStartsAtScreenTwo() {
        assertEquals(2, nextScreenToAdd(emptyList(), 3))
    }

    @Test fun addTakesTheNextFreeNumber() {
        assertEquals(3, nextScreenToAdd(listOf(pic(2)), 4))
        assertEquals(3, nextScreenToAdd(listOf(pic(2), pic(4)), 4))
    }

    @Test fun addStopsAtTheNumberOfScreens() {
        assertNull(nextScreenToAdd(listOf(pic(2), pic(3)), 3))
        assertNull(nextScreenToAdd(emptyList(), 1))
    }

    @Test fun rowsAreTheChosenScreensPlusTheOneBeingAdded() {
        assertEquals(listOf(2, 3), screenRows(listOf(pic(3)), adding = 2))
        assertEquals(listOf(2, 5), screenRows(listOf(pic(5), pic(2)), adding = null))
    }

    // ---- position: the part of the picture that fills the screen ----

    @Test fun aWidePictureIsCentredByDefault() {
        // 4000×1000 on a 1000×2000 screen: fill scale 2, the window is 500×1000, centred.
        assertEquals(listOf(1750, 0, 500, 1000), cropRect(4000, 1000, 1000, 2000, PicturePlace()).toList())
    }

    @Test fun aTallPictureIsCentredByDefault() {
        // 1000×4000 on a 1000×2000 screen: fill scale 1, the window is 1000×2000.
        assertEquals(listOf(0, 1000, 1000, 2000), cropRect(1000, 4000, 1000, 2000, PicturePlace()).toList())
    }

    @Test fun zoomShrinksTheWindowAroundTheCentre() {
        assertEquals(listOf(250, 500, 500, 1000), cropRect(1000, 2000, 1000, 2000, PicturePlace(zoom = 2f)).toList())
    }

    @Test fun theWindowNeverLeavesThePicture() {
        assertEquals(listOf(0, 0, 500, 1000), cropRect(4000, 1000, 1000, 2000, PicturePlace(x = 0f, y = 0f)).toList())
        assertEquals(listOf(3500, 0, 500, 1000), cropRect(4000, 1000, 1000, 2000, PicturePlace(x = 1f)).toList())
    }

    @Test fun clampKeepsZoomAtLeastFillAndAtMostTheLimit() {
        assertEquals(1f, clampPlace(PicturePlace(zoom = 0.3f), 1000, 2000, 1000, 2000).zoom)
        assertEquals(MAX_PICTURE_ZOOM, clampPlace(PicturePlace(zoom = 50f), 1000, 2000, 1000, 2000).zoom)
    }

    @Test fun clampPutsTheCentreWhereTheWindowFits() {
        val p = clampPlace(PicturePlace(x = 0f, y = 0.9f), 4000, 1000, 1000, 2000)
        assertEquals(250f / 4000f, p.x, 1e-4f)
        assertEquals(0.5f, p.y, 1e-4f)
    }

    @Test fun draggingRightShowsMoreOfTheLeft() {
        // Fill scale 2: 200 screen pixels are 100 picture pixels.
        val p = movePlace(PicturePlace(), 200f, 0f, 4000, 1000, 1000, 2000)
        assertEquals((2000f - 100f) / 4000f, p.x, 1e-4f)
    }

    @Test fun draggingStopsAtTheEdge() {
        val p = movePlace(PicturePlace(), 100000f, 0f, 4000, 1000, 1000, 2000)
        assertEquals(0, cropRect(4000, 1000, 1000, 2000, p)[0])
    }

    @Test fun zoomingOutPastFillStops() {
        assertEquals(1f, zoomPlace(PicturePlace(zoom = 1.2f), 0.5f, 1000, 2000, 1000, 2000).zoom)
        assertEquals(2.4f, zoomPlace(PicturePlace(zoom = 1.2f), 2f, 1000, 2000, 1000, 2000).zoom, 1e-4f)
    }

    @Test fun badSizesGiveTheWholePicture() {
        assertEquals(listOf(0, 0, 10, 20), cropRect(10, 20, 0, 0, PicturePlace()).toList())
    }

    // ---- sliding with its screen ----

    @Test fun thePictureSlidesWithItsScreen() {
        assertEquals(0f, slideOffset(2, 2, 0f), 1e-6f)
        assertEquals(-0.25f, slideOffset(2, 2, 0.25f), 1e-6f)
        assertEquals(0.75f, slideOffset(3, 2, 0.25f), 1e-6f)
    }

    // ---- stored as screens.json ----

    @Test fun jsonRoundTrips() {
        val list = listOf(ScreenPicture(2, "a.jpg", "b.jpg", PicturePlace(1.5f, 0.2f, 0.7f)), pic(4))
        assertEquals(list, ScreenPicturesJson.decode(ScreenPicturesJson.encode(list)))
    }

    @Test fun jsonDropsBadEntries() {
        val text = """{"v":1,"screens":[
            {"n":1,"src":"a.jpg","pic":"b.jpg"},
            {"n":2,"src":"../x","pic":"b.jpg"},
            {"n":3,"src":"a.jpg","pic":""},
            {"n":4,"src":"a.jpg","pic":"b.jpg","z":"bad"},
            {"n":5,"src":"c.jpg","pic":"d.jpg"},
            {"n":5,"src":"e.jpg","pic":"f.jpg"}]}"""
        val list = ScreenPicturesJson.decode(text)!!
        assertEquals(listOf(5), list.map { it.screen })
        assertEquals("c.jpg", list[0].source)
    }

    @Test fun jsonFromANewerVersionOrDamagedIsNull() {
        assertNull(ScreenPicturesJson.decode("""{"v":9,"screens":[]}"""))
        assertNull(ScreenPicturesJson.decode("not json"))
    }

    @Test fun filesInUseAreSourceAndShown() {
        assertTrue(screenPictureFiles(listOf(pic(2))) == setOf("s2.jpg", "w2.jpg"))
    }
}
