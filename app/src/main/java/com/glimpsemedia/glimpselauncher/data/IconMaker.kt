package com.glimpsemedia.glimpselauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable

/** Size of every finished icon. */
const val ICON_PX = 192

/** Drawing finished 192 × 192 icons: a pack's own drawing, or an app's icon fitted to a pack's rules. */
object IconMaker {
    fun draw(d: Drawable): Bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888).also { b ->
        d.setBounds(0, 0, ICON_PX, ICON_PX); d.draw(Canvas(b))
    }

    /**
     * An app's own icon in a pack's style: the back, then the icon at [scale] in the middle with the mask cut out of
     * it (the mask's drawn parts remove the icon there), then the front on top.
     */
    fun fitted(own: Drawable, back: Drawable?, mask: Drawable?, front: Drawable?, scale: Float): Bitmap {
        val out = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        back?.let { it.setBounds(0, 0, ICON_PX, ICON_PX); it.draw(c) }
        val layer = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        val size = (ICON_PX * scale).toInt().coerceIn(1, ICON_PX)
        val off = (ICON_PX - size) / 2
        own.setBounds(off, off, off + size, off + size); own.draw(lc)
        mask?.let {
            val m = draw(it)
            lc.drawBitmap(m, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) })
        }
        c.drawBitmap(layer, 0f, 0f, null)
        front?.let { it.setBounds(0, 0, ICON_PX, ICON_PX); it.draw(c) }
        return out
    }

    /**
     * An app shown with its own logo in Glimpse Colour, made to match the built-in drawings (WithIcons Dock's tiles): a
     * rounded tile with a soft shadow and a gradient from a lighter to a darker step of [colour], a faint shine at the
     * top, and on it the app's one-colour logo (Android 13+ themed-icon layer) in white; without one, the app's full
     * icon sits on the tile.
     */
    fun glimpse(own: Drawable, colour: Int): Bitmap {
        val out = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = ICON_PX / 24f                          // the drawings' 24-unit grid
        val l = 2.5f * s; val t = 2f * s; val r = 21.5f * s; val b = 22f * s
        val radius = 5.2f * s
        fun step(to: Int, k: Float) = android.graphics.Color.rgb(
            (android.graphics.Color.red(colour) * (1 - k) + android.graphics.Color.red(to) * k).toInt(),
            (android.graphics.Color.green(colour) * (1 - k) + android.graphics.Color.green(to) * k).toInt(),
            (android.graphics.Color.blue(colour) * (1 - k) + android.graphics.Color.blue(to) * k).toInt(),
        )
        val dark = step(android.graphics.Color.BLACK, 0.32f)
        // The shadow under the tile, half a unit lower.
        c.drawRoundRect(l, t + 0.5f * s, r, b + 0.5f * s, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; alpha = 72 })
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(0f, t, 0f, b, step(android.graphics.Color.WHITE, 0.18f), dark, android.graphics.Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(l, t, r, b, radius, radius, tile)
        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(0f, t, 0f, t + 8f * s, android.graphics.Color.argb(80, 255, 255, 255), android.graphics.Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(l, t, r, b, radius, radius, shine)
        val mono = if (android.os.Build.VERSION.SDK_INT >= 33 && own is android.graphics.drawable.AdaptiveIconDrawable) own.monochrome else null
        if (mono != null) {
            // An adaptive layer is 108 units with the logo inside the middle 72: shown at about 58 % of the icon, like the drawings' symbols.
            val layer = (ICON_PX * 0.58f * 108f / 72f).toInt()
            val off = (ICON_PX - layer) / 2
            val m = mono.mutate()
            m.setTint(android.graphics.Color.WHITE)
            m.setBounds(off, off, off + layer, off + layer)
            m.draw(c)
        } else {
            val size = (ICON_PX * 0.56f).toInt()
            val off = (ICON_PX - size) / 2
            own.setBounds(off, off, off + size, off + size)
            own.draw(c)
        }
        return out
    }
}
