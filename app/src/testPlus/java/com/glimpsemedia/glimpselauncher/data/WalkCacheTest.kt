package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.WalkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class WalkCacheTest {
    private val found = WalkResult(listOf(Entry("a.txt", false, "/s/a.txt", "", "", "/s")), false, emptySet())

    /** Closing Search while a long first walk runs must not throw the walk away: the next opening gets its result. */
    @Test fun closingSearchDoesNotStopTheWalk() = runBlocking {
        val walks = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val cache = WalkCache(CoroutineScope(SupervisorJob() + Dispatchers.Default), now = { 1_000L }) { walks.incrementAndGet(); gate.await(); found }
        val firstOpening = launch(Dispatchers.Default) { cache.current() }
        while (walks.get() == 0) Thread.sleep(5)
        firstOpening.cancelAndJoin()          // the window closed before the walk was done
        assertNull(cache.last())
        gate.complete(Unit)                   // the walk goes on and finishes
        assertEquals(found, cache.current())  // the next opening gets it
        assertEquals(1, walks.get())          // with no second walk
        assertEquals(found, cache.last())
    }

    @Test fun aFreshResultIsKeptForAMinute() = runBlocking {
        var t = 0L
        val walks = AtomicInteger()
        val cache = WalkCache(CoroutineScope(SupervisorJob() + Dispatchers.Default), now = { t }) { walks.incrementAndGet(); found }
        cache.current(); t = 59_000; cache.current()
        assertEquals(1, walks.get())
        t = 61_000; cache.current()
        assertEquals(2, walks.get())
    }

    /** Ten minutes after Search closes the names are dropped, to free the memory; opening it again keeps them. */
    @Test fun namesAreForgottenAWhileAfterSearchCloses() = runBlocking {
        val cache = WalkCache(CoroutineScope(SupervisorJob() + Dispatchers.Default), now = { 0L }, forgetAfterMs = 150) { found }
        cache.current()
        cache.closed(); Thread.sleep(50); cache.opened(); Thread.sleep(250)
        assertEquals(found, cache.last())     // reopened in time: kept
        cache.closed(); Thread.sleep(400)
        assertNull(cache.last())              // closed long enough: forgotten
        assertEquals(10 * 60_000L, FORGET_AFTER_MS)
    }
}
