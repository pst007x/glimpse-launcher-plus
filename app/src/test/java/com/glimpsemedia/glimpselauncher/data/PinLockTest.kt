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
