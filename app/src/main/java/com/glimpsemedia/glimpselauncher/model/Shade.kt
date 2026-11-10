package com.glimpsemedia.glimpselauncher.model

import kotlin.math.floor

/** The brightness slider runs from 50% darker to 50% lighter, in steps of 5; 0 is the colour as picked. */
const val SHADE_MIN = -50
const val SHADE_MAX = 50
const val SHADE_STEP = 5

/**
 * [argb] made lighter (towards white) or darker (towards black) by [percent], clamped to −50…50.
 * The transparency (alpha) is kept.
 */
fun shaded(argb: Int, percent: Int): Int {
    val p = percent.coerceIn(SHADE_MIN, SHADE_MAX) / 100.0
    if (p == 0.0) return argb
    fun channel(c: Int): Int {
        val v = if (p > 0) c + (255 - c) * p else c * (1 + p)
        return floor(v + 0.5).toInt().coerceIn(0, 255)
    }
    val a = argb ushr 24
    val r = channel((argb shr 16) and 0xFF)
    val g = channel((argb shr 8) and 0xFF)
    val b = channel(argb and 0xFF)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
