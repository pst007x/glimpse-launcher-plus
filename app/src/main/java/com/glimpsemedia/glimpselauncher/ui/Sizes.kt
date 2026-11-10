package com.glimpsemedia.glimpselauncher.ui

/** The usual sizes on a grid: the names (sp), the largest icon with names (dp), and without names (dp). */
data class GridSizes(val nameSp: Float, val iconDp: Float, val iconNoNameDp: Float)

/** Each grid's own sizes: the smaller the squares, the smaller the names and icons. */
fun gridSizes(cols: Int, rows: Int): GridSizes = when (cols to rows) {
    4 to 4 -> GridSizes(13f, 56f, 72f)
    4 to 5 -> GridSizes(12f, 50f, 64f)
    5 to 5 -> GridSizes(11f, 46f, 58f)
    5 to 6 -> GridSizes(10.5f, 42f, 54f)
    else -> GridSizes(20f, 80f, 96f)   // 2×4
}

/** The icons' size for a step −2…5: 80% and 90% below, then 20% a step up to twice the size. */
fun iconStepScale(step: Int): Float = when (step.coerceIn(-2, 5)) { -2 -> 0.8f; -1 -> 0.9f; 0 -> 1f; 1 -> 1.2f; 2 -> 1.4f; 3 -> 1.6f; 4 -> 1.8f; else -> 2f }

/** The names' size for a step −2…3: 80% and 90% below, up to one and a half times the size. */
fun nameStepScale(step: Int): Float = when (step.coerceIn(-2, 3)) { -2 -> 0.8f; -1 -> 0.9f; 0 -> 1f; 1 -> 1.15f; 2 -> 1.3f; else -> 1.5f }

/** Text mode's text size for a step −2…3: the old Small (18), Medium (22) and Large (28) are −1, 0 and +2; +3 is 150%. */
fun textStepSp(step: Int): Int = when (step.coerceIn(-2, 3)) { -2 -> 16; -1 -> 18; 1 -> 25; 2 -> 28; 3 -> 33; else -> 22 }

/** A step as shown: "-2", "-1", "0", "+1", "+2". */
fun stepLabel(step: Int): String = if (step > 0) "+$step" else "$step"
