package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BugLogTest {
    @get:Rule val tmp = TemporaryFolder()
    private val about = BugLogHeader(version = "1.0.4", phone = "samsung SM-A556B", android = "16 (API 36)")

    @Test fun anErrorIsRecordedWithWhenWhereAndWhat() {
        val log = BugLog(File(tmp.root, "bug-log.txt"), about)
        log.record("Backup", IllegalStateException("disk full"), at = 1_794_168_000_000L)
        val text = log.text()
        assertTrue(text.contains("Glimpse Launcher 1.0.4")); assertTrue(text.contains("samsung SM-A556B")); assertTrue(text.contains("16 (API 36)"))
        assertTrue(text.contains("Backup")); assertTrue(text.contains("IllegalStateException: disk full"))
        assertTrue(text.contains("2026-11-08"))
        assertTrue(text.contains("at com.glimpsemedia") || text.contains("at "))   // where in the code
    }

    @Test fun theLogStaysSmall() {
        val log = BugLog(File(tmp.root, "bug-log.txt"), about, maxBytes = 4_000)
        repeat(200) { log.record("Widget", RuntimeException("problem $it")) }
        assertTrue(File(tmp.root, "bug-log.txt").length() <= 4_000)
        assertTrue(log.text().contains("problem 199"))      // the newest are kept
        assertFalse(log.text().contains("problem 0\n"))
    }

    @Test fun clearingEmptiesIt() {
        val log = BugLog(File(tmp.root, "bug-log.txt"), about)
        log.record("Crash", RuntimeException("x"))
        log.clear()
        assertEquals("", log.text())
        assertFalse(log.hasEntries())
    }
}
