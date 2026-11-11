package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTurnTest {
    private val e = 0.0001f

    @Test fun slideLeavesThePagerAlone() {
        for (o in listOf(-1f, -0.5f, 0f, 0.3f, 1f)) assertEquals(PageLook(0f, 1f, 1f), pageLook(PageTurn.Slide, o))
    }

    @Test fun fadeKeepsBothScreensInPlaceAndCrossFades() {
        val leaving = pageLook(PageTurn.Fade, -0.25f)
        val coming = pageLook(PageTurn.Fade, 0.75f)
        assertEquals(0.25f, leaving.shift, e)   // undoes the pager's slide: the screen stays still
        assertEquals(-0.75f, coming.shift, e)
        assertEquals(0.75f, leaving.alpha, e)
        assertEquals(0.25f, coming.alpha, e)
        assertEquals(1f, leaving.scale, e)
    }

    @Test fun zoomShrinksTheLeavingScreenAndGrowsTheNextOne() {
        val still = pageLook(PageTurn.Zoom, 0f)
        assertEquals(PageLook(0f, 1f, 1f), still)
        val leaving = pageLook(PageTurn.Zoom, -0.5f)
        val coming = pageLook(PageTurn.Zoom, 0.5f)
        assertEquals(0.5f, leaving.shift, e)
        assertEquals(-0.5f, coming.shift, e)
        assertEquals(0.5f, leaving.alpha, e)
        assertTrue(leaving.scale < 1f && coming.scale < 1f)
        assertTrue(pageLook(PageTurn.Zoom, 0.9f).scale < coming.scale)   // the next one grows as it arrives
    }

    @Test fun stackSlidesTheNextScreenOverTheOneThatStays() {
        val coming = pageLook(PageTurn.Stack, 0.6f)    // the screen to the right slides in as usual
        assertEquals(0f, coming.shift, e)
        assertEquals(1f, coming.alpha, e)
        val staying = pageLook(PageTurn.Stack, -0.4f)  // the screen to the left stays still and dims
        assertEquals(0.4f, staying.shift, e)
        assertTrue(staying.alpha < 1f && staying.alpha > 0f)
        assertTrue(staying.scale < 1f)
    }

    @Test fun screensOutOfViewAreHidden() {
        for (t in listOf(PageTurn.Fade, PageTurn.Zoom, PageTurn.Stack)) {
            assertEquals(0f, pageLook(t, -1.5f).alpha, e)
            assertEquals(0f, pageLook(t, 2f).alpha, e)
            // Left off the screen where the pager puts them: an invisible screen over this one would catch its touches.
            for (o in listOf(-2f, -1f, 1f, 2f)) assertEquals(0f, pageLook(t, o).shift, e)
        }
    }

    @Test fun savedNumbersReadBackAndUnknownOnesSlide() {
        for (t in PageTurn.entries) assertEquals(t, PageTurn.of(t.ordinal))
        assertEquals(PageTurn.Slide, PageTurn.of(9))
        assertEquals(PageTurn.Slide, PageTurn.of(-1))
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
