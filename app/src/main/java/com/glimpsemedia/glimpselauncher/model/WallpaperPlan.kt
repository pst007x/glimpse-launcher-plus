package com.glimpsemedia.glimpselauncher.model

/** A soft round glow of [color], strongest ([alpha]) at its centre and gone at [radius]. */
data class Glow(val cx: Float, val cy: Float, val radius: Float, val color: Int, val alpha: Float)

/**
 * What a theme wallpaper is drawn from: a blend from [from] at the top left (0, 0) to [to] at ([endX], [endY]),
 * and two glows over it. Always portrait.
 */
data class WallpaperPlan(
    val width: Int,
    val height: Int,
    val from: Int,
    val to: Int,
    val endX: Float,
    val endY: Float,
    val glows: List<Glow>,
)

/**
 * The wallpaper for four theme colours at a screen size: colour 1 blending into colour 2 (mostly downwards), a glow
 * of colour 3 low on the left and of colour 4 high on the right, stronger on dark themes. A landscape size is turned
 * upright, and a missing size becomes 1080 × 2340.
 */
fun wallpaperPlan(width: Int, height: Int, c1: Int, c2: Int, c3: Int, c4: Int, dark: Boolean): WallpaperPlan {
    var w = width
    var h = height
    if (w <= 0 || h <= 0) { w = 1080; h = 2340 }
    if (w > h) { val t = w; w = h; h = t }
    // The mix is t = 0.35·x/w + 0.65·y/h; the blend's end is the nearest point where t = 1.
    val gx = 0.35 / w
    val gy = 0.65 / h
    val n = gx * gx + gy * gy
    val alpha = if (dark) 0.47f else 0.37f
    val radius = 0.58f * w
    return WallpaperPlan(
        width = w, height = h, from = c1, to = c2,
        endX = (gx / n).toFloat(), endY = (gy / n).toFloat(),
        glows = listOf(
            Glow(0.25f * w, 0.80f * h, radius, c3, alpha),
            Glow(0.90f * w, 0.14f * h, radius, c4, alpha),
        ),
    )
}
