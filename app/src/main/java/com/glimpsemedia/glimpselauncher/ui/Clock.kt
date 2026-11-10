package com.glimpsemedia.glimpselauncher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.Settings
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

const val CLOCK_DIGITAL = 0
const val CLOCK_SIDE_BY_SIDE = 1
const val CLOCK_ANALOGUE = 2

/** The clock at the top of the first screen, in one of three styles, with an optional coloured background. */
@Composable
fun ClockHeader(settings: Settings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay((60 - value.second) * 1000L)
        }
    }
    val back = settings.clockBackground
    val baseBack = (if (settings.clockBackColor == 0) LocalPalette.current.bar else Color(settings.clockBackColor)).copy(alpha = 1f)
    val solidBack = Color(com.glimpsemedia.glimpselauncher.model.shaded(baseBack.toArgb(), settings.clockBackShade))
    val backColor = solidBack.copy(alpha = tileOpacity(settings.clockBackTransparency))
    // The chosen text colour, else white (on the wallpaper) or whatever reads on the background colour.
    val inkSolid = Color(clockInk(settings.clockColor, settings.clockShade, back, solidBack.toArgb())).copy(alpha = 1f)
    val ink = inkSolid.copy(alpha = tileOpacity(settings.clockTransparency))
    // Over a wallpaper a halo keeps it readable; on its own background it is not needed.
    val shadow = if (back) null else Shadow(if (inkSolid.luminance() > 0.45f) Color.Black else Color.White, Offset(2f, 2f), 6f)
    val locale = Locale.getDefault()
    val time = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale))
    val day = now.format(DateTimeFormatter.ofPattern("EEEE", locale))
    val date = now.format(DateTimeFormatter.ofPattern("d MMMM", locale))
    val scale = clockScale(settings.clockSize)
    fun text(size: Int, weight: FontWeight) = TextStyle(color = ink, fontSize = (size * scale).sp, fontWeight = weight, shadow = shadow)

    Column(modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(28.dp)
        // Still too wide for a narrow screen: the whole clock is drawn smaller rather than cut off.
        ShrinkToFit {
        Column(
            Modifier.then(if (back) Modifier.clip(shape).background(backColor) else Modifier)
                .padding(horizontal = if (back) (28 * scale).dp else 0.dp, vertical = if (back) (8 * scale).dp else 0.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$time, $day $date"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (settings.clockStyle) {
                CLOCK_SIDE_BY_SIDE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((20 * scale).dp)) {
                    Text(time, style = text(64, FontWeight.Bold))
                    if (settings.showDate) {
                        Column {
                            Text(day, style = text(26, FontWeight.SemiBold))
                            Text(date, style = text(26, FontWeight.Medium))
                        }
                    }
                }
                CLOCK_ANALOGUE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((24 * scale).dp)) {
                    AnalogueFace(now, ink, Modifier.size((128 * scale).dp))
                    if (settings.showDate) {
                        Column {
                            Text(day, style = text(30, FontWeight.Bold))
                            Text(date, style = text(28, FontWeight.Medium))
                        }
                    }
                }
                else -> {
                    Text(time, style = text(64, FontWeight.Bold))
                    if (settings.showDate) Text("$day $date", style = text(24, FontWeight.Medium))
                }
            }
        }
        }
    }
}

/** How big the clock is drawn for the Clock size setting: small, medium or large (the size it always had). */
fun clockScale(size: Int): Float = when (size) {
    com.glimpsemedia.glimpselauncher.data.CLOCK_SMALL -> 0.7f
    com.glimpsemedia.glimpselauncher.data.CLOCK_MEDIUM -> 0.85f
    else -> 1f
}

/** Lays [content] out at its natural width and, when that is wider than the room there is, draws it smaller to fit. */
@Composable
private fun ShrinkToFit(content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, maxWidth = androidx.compose.ui.unit.Constraints.Infinity))
        val shrink = if (constraints.hasBoundedWidth && placeable.width > constraints.maxWidth) constraints.maxWidth.toFloat() / placeable.width else 1f
        val w = (placeable.width * shrink).toInt()
        val h = (placeable.height * shrink).toInt()
        layout(w, h) {
            placeable.placeWithLayer((w - placeable.width) / 2, (h - placeable.height) / 2) { scaleX = shrink; scaleY = shrink }
        }
    }
}

/** A plain round clock face: bold marks at 12, 3, 6 and 9, small ones between, and clear hands. */
@Composable
private fun AnalogueFace(now: LocalDateTime, ink: Color, modifier: Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f - 4.dp.toPx()
        drawCircle(ink, radius = r, center = c, style = Stroke(width = 4.dp.toPx()))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val long = i % 3 == 0
            val from = r - (if (long) 16.dp.toPx() else 9.dp.toPx())
            drawLine(
                ink,
                Offset(c.x + cos(a).toFloat() * from, c.y + sin(a).toFloat() * from),
                Offset(c.x + cos(a).toFloat() * (r - 3.dp.toPx()), c.y + sin(a).toFloat() * (r - 3.dp.toPx())),
                strokeWidth = (if (long) 5.dp else 2.5.dp).toPx(), cap = StrokeCap.Round,
            )
        }
        fun hand(degrees: Double, length: Float, width: Float) {
            val a = Math.toRadians(degrees - 90.0)
            drawLine(ink, c, Offset(c.x + cos(a).toFloat() * length, c.y + sin(a).toFloat() * length), strokeWidth = width, cap = StrokeCap.Round)
        }
        hand((now.hour % 12 + now.minute / 60.0) * 30.0, r * 0.5f, 8.dp.toPx())
        hand(now.minute * 6.0, r * 0.78f, 5.dp.toPx())
        drawCircle(ink, radius = 6.dp.toPx(), center = c)
    }
}

/**
 * The clock's text colour: the chosen one, else whatever reads on its [panel] (when [onPanel]), else white —
 * then made darker or lighter by [clockShade], so the slider works on the standard colour too.
 */
fun clockInk(clockColor: Int, clockShade: Int, onPanel: Boolean, panel: Int): Int {
    val base = when {
        clockColor != 0 -> clockColor
        onPanel -> readableOn(Color(panel)).toArgb()
        else -> 0xFFFFFFFF.toInt()
    }
    return com.glimpsemedia.glimpselauncher.model.shaded(base, clockShade)
}
