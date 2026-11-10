package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** -1 for a swipe up, 1 for down, 0 for neither: long enough, and much more up-and-down than sideways. */
fun verticalSwipe(dx: Float, dy: Float, minDistancePx: Float): Int = when {
    abs(dy) < minDistancePx || abs(dy) <= 2 * abs(dx) -> 0
    dy > 0 -> 1
    else -> -1
}

/**
 * Follows one gesture. A touch-down taken by a tile or the clock for its tap still counts; a move taken by
 * someone else (a tile being dragged, the pager turning, a widget scrolling) or a second finger does not.
 * [move] returns true once the gesture is clearly up or down: from then on the caller takes the moves,
 * so the tile under the finger does not also open.
 */
class SwipeTracker(private val minDistancePx: Float, private val slopPx: Float) {
    private var x0 = 0f
    private var y0 = 0f
    private var x = 0f
    private var y = 0f
    private var taken = false
    private var claimed = false

    fun down(atX: Float, atY: Float) {
        x0 = atX; y0 = atY; x = atX; y = atY; taken = false; claimed = false
    }

    fun move(atX: Float, atY: Float, consumedByOther: Boolean, pointers: Int): Boolean {
        x = atX; y = atY
        if (pointers > 1 || (consumedByOther && !claimed)) taken = true
        if (taken) return false
        if (!claimed) {
            val dx = x - x0
            val dy = y - y0
            if (abs(dy) > slopPx && abs(dy) > 2 * abs(dx)) claimed = true
        }
        return claimed
    }

    /** 1 down, -1 up, 0 neither. */
    fun up(): Int = if (taken) 0 else verticalSwipe(x - x0, y - y0, minDistancePx)
}

/**
 * Swipe down and swipe up on the home screen. It listens last (the final pass), so a move that anything else
 * has taken is left alone; once a swipe is clear it takes the moves, which cancels the tap underneath.
 */
fun Modifier.homeSwipes(enabled: Boolean, onDown: () -> Unit, onUp: () -> Unit): Modifier = composed {
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    val on by rememberUpdatedState(enabled)
    val minPx = with(LocalDensity.current) { 64.dp.toPx() }
    pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val tracker = SwipeTracker(minPx, slop).apply { down(first.position.x, first.position.y) }
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == first.id } ?: break
                val byOther = change.isConsumed && change.position != change.previousPosition
                if (tracker.move(change.position.x, change.position.y, byOther, event.changes.count { it.pressed }.coerceAtLeast(1)) && on) {
                    event.changes.forEach { it.consume() }
                }
                if (!change.pressed) break
            }
            if (on) when (tracker.up()) {
                1 -> down()
                -1 -> up()
            }
        }
    }
}
