package com.glimpsemedia.glimpselauncher.model

import kotlin.math.abs

/** How the screens change when you swipe (Settings › Home screen › Screens › Screen change). Saved by its number. */
enum class PageTurn(val label: String) {
    Slide("Slide"),
    Fade("Fade out and in"),
    Zoom("Zoom"),
    Stack("Stack");

    companion object {
        fun of(saved: Int): PageTurn = entries.getOrNull(saved) ?: Slide
    }
}

/**
 * How a screen is drawn mid-change: [shift] in screen widths, added to where the pager puts it (so -offset keeps it
 * still), with [alpha] and [scale].
 */
data class PageLook(val shift: Float, val alpha: Float, val scale: Float)

/**
 * The look of a screen [offset] screen widths from the middle of the view (negative: to the left, the screen being
 * left on a forward swipe; 1 or more: out of view). Slide is the pager's own movement.
 */
fun pageLook(turn: PageTurn, offset: Float): PageLook {
    if (turn == PageTurn.Slide) return PageLook(0f, 1f, 1f)
    val a = abs(offset)
    // Out of view: left where the pager puts it, off the screen. Moved over this one, even invisible, it would take
    // this screen's touches.
    if (a >= 1f) return PageLook(0f, 0f, 1f)
    return when (turn) {
        PageTurn.Fade -> PageLook(0f - offset, 1f - a, 1f)
        PageTurn.Zoom -> PageLook(0f - offset, 1f - a, 1f - 0.15f * a)
        // The screen on the right slides over; the one on the left stays still, settles back and fades under it.
        else -> if (offset >= 0f) PageLook(0f, 1f, 1f) else PageLook(0f - offset, 1f - a, 1f - 0.08f * a)
    }
}
