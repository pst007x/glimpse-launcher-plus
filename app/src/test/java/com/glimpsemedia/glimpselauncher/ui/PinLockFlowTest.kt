package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class PinLockFlowTest {
    // Locking abandons any half-done PIN change, so the next person can't set a PIN of their own.
    @Test fun lockingClosesPinSetupButKeepsTheUnlockPad() {
        assertEquals(null, pinStepAfterLock(PinStep.NewFirst(unlockAfter = false)))
        assertEquals(null, pinStepAfterLock(PinStep.NewSecond("1234", unlockAfter = false)))
        assertEquals(null, pinStepAfterLock(PinStep.Current(thenRemove = true)))
        assertEquals(PinStep.Unlock, pinStepAfterLock(PinStep.Unlock))
        assertEquals(PinStep.NoPhoneLock, pinStepAfterLock(PinStep.NoPhoneLock))
        assertEquals(null, pinStepAfterLock(null))
    }

    // Finishing the welcome unlocks only when there is no PIN, or it was already unlocked.
    @Test fun theWelcomeNeverUnlocksPastAPin() {
        assertTrue(welcomeMayUnlock(pinHash = null, unlocked = false))
        assertTrue(welcomeMayUnlock(pinHash = "h", unlocked = true))
        assertFalse(welcomeMayUnlock(pinHash = "h", unlocked = false))
    }

    @Test fun notesOpenRule() {
        assertTrue(notesOpen(needPin = false, unlocked = false, notesUnlocked = false))
        assertFalse(notesOpen(needPin = true, unlocked = false, notesUnlocked = false))
        assertTrue(notesOpen(needPin = true, unlocked = false, notesUnlocked = true))
        assertTrue(notesOpen(needPin = true, unlocked = true, notesUnlocked = false))
    }

    @Test fun lockingKeepsTheNotesPinPads() {
        assertEquals(PinStep.UnlockNotes, pinStepAfterLock(PinStep.UnlockNotes))
        assertNull(pinStepAfterLock(PinStep.UnlockChoice))
    }

    @Test fun whatThePadlockHoldAsksFor() {
        // No PIN: the hold unlocks at once.
        assertNull(pinStepForPadlock(hasPin = false, notesNeedPin = false, notesUnlocked = false))
        // A PIN, notes not locked by it: the PIN, then edit mode.
        assertEquals(PinStep.Unlock, pinStepForPadlock(hasPin = true, notesNeedPin = false, notesUnlocked = false))
        // Notes need the PIN: the PIN, then the choice (from any screen).
        assertEquals(PinStep.UnlockNotes, pinStepForPadlock(hasPin = true, notesNeedPin = true, notesUnlocked = false))
        // Notes already open: nothing to choose, the PIN gives edit mode.
        assertEquals(PinStep.Unlock, pinStepForPadlock(hasPin = true, notesNeedPin = true, notesUnlocked = true))
    }
}
