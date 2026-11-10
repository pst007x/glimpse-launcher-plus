package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.WalkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long after Search closes the whole-phone names are dropped, to give their memory back. */
const val FORGET_AFTER_MS = 10 * 60_000L

/**
 * The whole-phone walk and its result. The walk runs in [scope] (the activity's), not in the search window's: a big
 * phone can take a while, and closing Search before it is done must not throw the work away. Someone waiting is only
 * waiting; a second caller shares the walk already running. A result is kept for a minute, and forgotten
 * [forgetAfterMs] after Search closes (opening it again in time keeps it).
 */
class WalkCache(
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val forgetAfterMs: Long = FORGET_AFTER_MS,
    private val walk: suspend () -> WalkResult,
) {
    private val lock = Any()
    private var cached: WalkResult? = null
    private var cachedAt = 0L
    private var running: Deferred<WalkResult>? = null
    private var forgetting: Job? = null

    /** Search opened: the names are wanted again, so they are not forgotten. */
    fun opened() = synchronized(lock) { forgetting?.cancel(); forgetting = null }

    /** Search closed: the names are dropped after a while unless it opens again. */
    fun closed() = synchronized(lock) {
        forgetting?.cancel()
        forgetting = scope.launch { delay(forgetAfterMs); synchronized(lock) { cached = null; forgetting = null } }
    }

    /** The last result, at once, or null before the first walk is done. */
    fun last(): WalkResult? = synchronized(lock) { cached }

    /** The kept result when under a minute old; else the walk already running, or a new one. */
    suspend fun current(): WalkResult {
        val job = synchronized(lock) {
            cached?.let { if (now() - cachedAt < MAX_AGE_MS) return it }
            running ?: scope.async {
                val r = walk()
                synchronized(lock) { cached = r; cachedAt = now(); running = null }
                r
            }.also { running = it }
        }
        return job.await()
    }

    private companion object { const val MAX_AGE_MS = 60_000L }
}
