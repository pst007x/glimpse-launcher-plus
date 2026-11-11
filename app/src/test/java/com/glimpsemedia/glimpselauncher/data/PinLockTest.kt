package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PinLockTest {
    private fun b64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

    @Test fun validPins() {
        assertTrue(Pin.isValid("1234"))
        assertTrue(Pin.isValid("12345678"))
        for (bad in listOf("123", "123456789", "12a4", "", " 1234")) assertFalse(bad, Pin.isValid(bad))
    }

    @Test fun hashMatchesOnlyTheSamePin() {
        val salt = Pin.newSalt()
        val hash = Pin.hash("4321", salt)
        assertTrue(Pin.matches("4321", hash, b64(salt)))
        assertFalse(Pin.matches("4322", hash, b64(salt)))
        assertNotEquals(hash, Pin.hash("4321", Pin.newSalt()))
    }

    @Test fun hashNeverContainsTheDigits() {
        assertFalse(Pin.hash("9876", Pin.newSalt()).contains("9876"))
    }

    @Test fun fiveFailuresBlockForThirtySeconds() {
        var t = 1_000_000L
        val tries = PinTries { t }
        repeat(4) { tries.failed() }
        assertEquals(0L, tries.blockedForMs())
        tries.failed()
        assertEquals(30_000L, tries.blockedForMs())
        t += 30_000
        assertEquals(0L, tries.blockedForMs())
        // the count restarts: four more failures do not block
        repeat(4) { tries.failed() }
        assertEquals(0L, tries.blockedForMs())
    }

    @Test fun successResetsTheCount() {
        val tries = PinTries { 0L }
        repeat(4) { tries.failed() }
        tries.succeeded()
        repeat(4) { tries.failed() }
        assertEquals(0L, tries.blockedForMs())
    }
}

class PinTriesKeptTest {
    private class Memory : PinTriesMemory {
        var failures = 0; var until = 0L
        override fun read() = failures to until
        override fun write(failures: Int, blockedUntil: Long) { this.failures = failures; until = blockedUntil }
    }

    @Test fun wrongTriesAreCountedAfterTheLauncherIsReopened() {
        var t = 1_000L
        val m = Memory()
        val first = PinTries({ t }, m)
        repeat(4) { first.failed() }
        val again = PinTries({ t }, m)   // the launcher closed and opened again
        again.failed()
        org.junit.Assert.assertEquals(PinTries.BLOCK_MS, again.blockedForMs())
    }

    @Test fun thePauseSurvivesAReopen() {
        var t = 1_000L
        val m = Memory()
        val p = PinTries({ t }, m)
        repeat(5) { p.failed() }
        t += 10_000
        org.junit.Assert.assertEquals(20_000L, PinTries({ t }, m).blockedForMs())
    }

    @Test fun aClockSetBackNeverPausesLongerThan30Seconds() {
        var t = 1_000_000L
        val m = Memory()
        val p = PinTries({ t }, m)
        repeat(5) { p.failed() }
        t -= 500_000   // the phone's clock set back
        org.junit.Assert.assertEquals(PinTries.BLOCK_MS, PinTries({ t }, m).blockedForMs())
    }

    @Test fun aRightPinClearsTheSavedCount() {
        val m = Memory()
        val p = PinTries({ 0L }, m)
        repeat(3) { p.failed() }
        p.succeeded()
        org.junit.Assert.assertEquals(0, m.failures)
    }

    @Test fun theCountIsNeverInABackup() {
        val all = mapOf("pinFailures" to PrefValue.I(3), "pinBlockedUntil" to PrefValue.L(9L), "darkTheme" to PrefValue.B(true))
        org.junit.Assert.assertEquals(setOf("darkTheme"), Backup.settingsForBackup(all).keys)
    }
}
