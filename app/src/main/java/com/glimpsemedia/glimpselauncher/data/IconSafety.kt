package com.glimpsemedia.glimpselauncher.data

/**
 * Hands the icon pack a pack's own "Apply" chose (after the person agreed) from [com.glimpsemedia.glimpselauncher.ApplyIconPackActivity]
 * to the home screen, inside Glimpse only: no other app can set it. Taken once; kept only when [take]'s check says it
 * is an installed icon pack.
 */
object PendingIconPack {
    private var pending: String? = null
    @Synchronized fun offer(pkg: String) { pending = pkg }
    @Synchronized fun take(isPack: (String) -> Boolean): String? = pending.also { pending = null }?.takeIf(isPack)
}

/** Runs icon drawing so a broken pack drawable, or a picture too big for memory, gives no icon instead of a crash. */
inline fun <T> safely(block: () -> T): T? = try { block() } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
