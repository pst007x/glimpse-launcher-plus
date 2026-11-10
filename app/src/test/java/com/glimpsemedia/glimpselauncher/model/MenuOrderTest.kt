package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.textSizeFromStored
import com.glimpsemedia.glimpselauncher.ui.textClockScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MenuOrderTest {
    @Test fun textModeHasThreeSizesAgain() {
        // The old Extra small and Extra large read as Small and Large (kept for reading old settings).
        assertEquals(listOf(1, 1, 2, 3, 3), (0..4).map { textSizeFromStored(it) })
        assertEquals(0, Settings().textStep)
    }

    @Test fun theTextModeClockHasItsOwnSize() {
        assertEquals(1, Settings().textClockSize)
        assertEquals(listOf(0.75f, 1f, 1.35f), (0..2).map { textClockScale(it) })
        assertEquals(1f, textClockScale(9))
    }

    @Test fun theBugLogStartsOff() { assertFalse(Settings().bugLog) }
}
