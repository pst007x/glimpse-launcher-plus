package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderBadgeSpotTest {
    // The folder's front, as fractions of the square: the lowest front top of the six pictures (picture 6's slant)
    // down to the folder's bottom, and the folder's left and right edges.
    private val frontTop = 0.50f
    private val bottom = 0.86f
    private val left = 0.06f
    private val right = 0.94f

    @Test fun theBadgeIsCentredAcrossTheFolder() = assertEquals(0.5f, FOLDER_BADGE_SPOT.x, 0.0001f)

    @Test fun theMonoBadgeSitsWhollyOnTheFront() = assertOnFront(FOLDER_BADGE_SPOT.diameter)

    @Test fun theColourBadgeSitsWhollyOnTheFront() = assertOnFront(FOLDER_BADGE_SPOT.colourDiameter)

    @Test fun theBadgeIsStillEasyToSee() = assertTrue(FOLDER_BADGE_SPOT.diameter >= 0.30f)

    private fun assertOnFront(d: Float) {
        val r = d / 2
        assertTrue("top ${FOLDER_BADGE_SPOT.y - r}", FOLDER_BADGE_SPOT.y - r >= frontTop - 0.0001f)
        assertTrue("bottom ${FOLDER_BADGE_SPOT.y + r}", FOLDER_BADGE_SPOT.y + r <= bottom - 0.01f)
        assertTrue(FOLDER_BADGE_SPOT.x - r >= left && FOLDER_BADGE_SPOT.x + r <= right)
    }
}
