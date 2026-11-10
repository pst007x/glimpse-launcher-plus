package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.nameStepFromOld
import com.glimpsemedia.glimpselauncher.data.stepFromStored
import com.glimpsemedia.glimpselauncher.data.textStepFromOld
import com.glimpsemedia.glimpselauncher.data.ICON_STEPS
import com.glimpsemedia.glimpselauncher.data.NAME_STEPS
import com.glimpsemedia.glimpselauncher.data.TEXT_STEPS
import org.junit.Assert.assertEquals
import org.junit.Test

class SizesTest {
    @Test fun eachGridHasItsOwnSizes() {
        assertEquals(GridSizes(20f, 80f, 96f), gridSizes(2, 4))
        assertEquals(GridSizes(13f, 56f, 72f), gridSizes(4, 4))
        assertEquals(GridSizes(12f, 50f, 64f), gridSizes(4, 5))
        assertEquals(GridSizes(11f, 46f, 58f), gridSizes(5, 5))
        assertEquals(GridSizes(10.5f, 42f, 54f), gridSizes(5, 6))
    }

    @Test fun iconStepsReachTwiceTheSize() {
        assertEquals(listOf(0.8f, 0.9f, 1f, 1.2f, 1.4f, 1.6f, 1.8f, 2f), (-2..5).map { iconStepScale(it) })
        assertEquals(2f, iconStepScale(9)); assertEquals(0.8f, iconStepScale(-9))
    }

    @Test fun nameStepsReachOneAndAHalf() {
        assertEquals(listOf(0.8f, 0.9f, 1f, 1.15f, 1.3f, 1.5f), (-2..3).map { nameStepScale(it) })
        assertEquals(1.5f, nameStepScale(9))
    }

    @Test fun textModeStepsKeepTheOldSizes() {
        assertEquals(listOf(16, 18, 22, 25, 28, 33), (-2..3).map { textStepSp(it) })
    }

    @Test fun stepLabels() {
        assertEquals(listOf("-2", "-1", "0", "+1", "+2", "+5"), listOf(-2, -1, 0, 1, 2, 5).map { stepLabel(it) })
    }

    @Test fun oldChoicesCarryOver() {
        assertEquals(listOf(-1, 0, 1), (0..2).map { nameStepFromOld(it) })
        assertEquals(listOf(-1, 0, 2), (1..3).map { textStepFromOld(it) })
        assertEquals(listOf(-2, -2, 0, 3, 3), listOf(-7, -2, 0, 3, 7).map { stepFromStored(it, NAME_STEPS) })
        assertEquals(listOf(-2, 5, 5), listOf(-7, 5, 9).map { stepFromStored(it, ICON_STEPS) })
        assertEquals(-2..3, TEXT_STEPS)
    }

    @Test fun allStartAtZero() {
        val s = Settings()
        assertEquals(listOf(0, 0, 0), listOf(s.tileNameStep, s.tileIconStep, s.textStep))
    }

    @Test fun quickUnlockIsOneSecond() {
        assertEquals(3000, unlockHoldMs(quick = false))
        assertEquals(1000, unlockHoldMs(quick = true))
        assertEquals(false, Settings().quickUnlock)
    }
}
