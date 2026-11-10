package com.glimpsemedia.glimpselauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.glimpsemedia.glimpselauncher.model.WallpaperPlan

/** Paints a theme wallpaper: the blend, then the two glows. */
fun drawThemeWallpaper(plan: WallpaperPlan): Bitmap {
    val bitmap = Bitmap.createBitmap(plan.width, plan.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    paint.shader = LinearGradient(0f, 0f, plan.endX, plan.endY, plan.from, plan.to, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, plan.width.toFloat(), plan.height.toFloat(), paint)
    for (g in plan.glows) {
        val strong = (g.color and 0x00FFFFFF) or ((g.alpha * 255).toInt().coerceIn(0, 255) shl 24)
        val clear = g.color and 0x00FFFFFF
        paint.shader = RadialGradient(g.cx, g.cy, g.radius, strong, clear, Shader.TileMode.CLAMP)
        canvas.drawCircle(g.cx, g.cy, g.radius, paint)
    }
    paint.shader = null
    return bitmap
}
