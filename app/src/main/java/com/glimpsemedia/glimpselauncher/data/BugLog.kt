package com.glimpsemedia.glimpselauncher.data

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What each bug log entry says about the app and the phone: nothing personal. */
data class BugLogHeader(val version: String, val phone: String, val android: String)

/**
 * The optional bug log (Settings > Help and support > Report bugs): crashes and errors, with when, where in the launcher, and what went
 * wrong. It is kept only on the phone, never holds app names, notes or numbers, stays small (the oldest entries go
 * first), and is only sent when the person emails it themselves.
 */
class BugLog(private val file: File, private val header: BugLogHeader, private val maxBytes: Int = 200_000) {
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Synchronized
    fun record(where: String, t: Throwable?, at: Long = System.currentTimeMillis()) {
        val time = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(stamp)
        val entry = buildString {
            append("=== ").append(time).append(" · Glimpse Launcher ").append(header.version)
                .append(" · ").append(header.phone).append(" · Android ").append(header.android).append('\n')
            append(where).append('\n')
            if (t != null) {
                // The error and where it happened in the code: at most 25 lines, so one entry never fills the log.
                append(t.toString()).append('\n')
                t.stackTrace.take(25).forEach { append("  at ").append(it).append('\n') }
                t.cause?.let { append("Caused by: ").append(it).append('\n') }
            }
            append('\n')
        }
        file.parentFile?.mkdirs()
        file.appendText(entry)
        if (file.length() > maxBytes) trim()
    }

    /** Keeps the newest entries in about three quarters of the room. */
    private fun trim() {
        val text = file.readText()
        var cut = text.length - (maxBytes * 3 / 4)
        if (cut <= 0) return
        val next = text.indexOf("\n=== ", cut)
        cut = if (next < 0) text.length else next + 1
        file.writeText(text.substring(cut))
    }

    fun text(): String = if (file.isFile) file.readText() else ""

    fun hasEntries(): Boolean = file.isFile && file.length() > 0

    @Synchronized
    fun clear() { file.delete() }
}
