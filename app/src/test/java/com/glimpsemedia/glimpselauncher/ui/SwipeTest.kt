package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeTest {
    @Test fun swipeDirection() {
        assertEquals(1, verticalSwipe(0f, 200f, 100f))
        assertEquals(-1, verticalSwipe(0f, -200f, 100f))
        assertEquals("too diagonal", 0, verticalSwipe(150f, 200f, 100f))
        assertEquals("too short", 0, verticalSwipe(0f, 50f, 100f))
    }

    // A tile or the clock takes the touch-down for its tap; a swipe that starts there must still count.
    @Test fun aSwipeStartingOnATileCounts() {
        val t = SwipeTracker(minDistancePx = 100f, slopPx = 10f)
        t.down(500f, 500f)
        assertTrue("claimed once past the slop", t.move(502f, 540f, consumedByOther = false, pointers = 1))
        t.move(505f, 800f, consumedByOther = false, pointers = 1)
        assertEquals(1, t.up())
    }

    @Test fun aMoveTakenByAnotherHandlerIsNotASwipe() {
        val t = SwipeTracker(minDistancePx = 100f, slopPx = 10f)
        t.down(500f, 500f)
        assertFalse(t.move(500f, 520f, consumedByOther = true, pointers = 1))
        t.move(500f, 800f, consumedByOther = false, pointers = 1)
        assertEquals(0, t.up())
    }

    @Test fun sidewaysAndTwoFingersAreNotSwipes() {
        val side = SwipeTracker(100f, 10f).apply { down(0f, 0f) }
        assertFalse(side.move(200f, 20f, consumedByOther = false, pointers = 1))
        assertEquals(0, side.up())
        val two = SwipeTracker(100f, 10f).apply { down(0f, 0f) }
        two.move(0f, 300f, consumedByOther = false, pointers = 2)
        assertEquals(0, two.up())
    }
}
