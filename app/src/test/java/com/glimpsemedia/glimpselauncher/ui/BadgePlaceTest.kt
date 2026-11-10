package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BadgePlaceTest {
    @Test fun smallIconsGetTheSmallNumber() {
        assertEquals(true, smallBadgeFor(48.dp))
        assertEquals(false, smallBadgeFor(96.dp))
    }
}
