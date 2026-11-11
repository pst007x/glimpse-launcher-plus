package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import com.glimpsemedia.glimpselauncher.model.shaded

private val PAPER = Color(0xFFFFFFFF)
private val PAPER_EDGE = Color(0xFFB8EEFA)
private val OUTLINE = Color(0xFF1E1E1E)

/**
 * One of the six folder pictures of the folder-icon look ([picture] 1 to 6), drawn in [color]; the darker and lighter
 * parts are worked out from it, so every picture follows the folder colour. The sheets of paper stay white and the
 * outline of picture 2 stays dark.
 */
@Composable
fun FolderPicture(picture: Int, color: Color, modifier: Modifier = Modifier) {
    val argb = color.toArgb()
    val darker = Color(shaded(argb, -22))
    val lighter = Color(shaded(argb, 18))
    Canvas(modifier.fillMaxSize()) {
        val f = Frame(size.width, size.height)
        when (picture) {
            1 -> paperAndSlant(f, darker, lighter)
            2 -> outlined(f, color, darker)
            4 -> plain(f, color)
            5 -> taperedFront(f, color, darker, lighter)
            6 -> slantedFront(f, color, lighter)
            else -> paperAndFlat(f, color, darker)
        }
    }
}

/** Where a folder sits in its square: a little wider than tall, like the pictures it is drawn from. */
private class Frame(val w: Float, val h: Float) {
    val left = w * 0.06f
    val right = w * 0.94f
    val top = h * 0.16f
    val bottom = h * 0.86f
    val r = w * 0.07f
    val tabEnd = left + w * 0.30f
    val tabSlope = w * 0.09f
    val tabDrop = h * 0.09f
}

/** The back of a folder: its tab at the top left, then the full width a little lower. */
private fun back(f: Frame): Path = Path().apply {
    with(f) {
        moveTo(left + r, top)
        lineTo(tabEnd, top)
        lineTo(tabEnd + tabSlope, top + tabDrop)
        lineTo(right - r, top + tabDrop)
        quadraticTo(right, top + tabDrop, right, top + tabDrop + r)
        lineTo(right, bottom - r)
        quadraticTo(right, bottom, right - r, bottom)
        lineTo(left + r, bottom)
        quadraticTo(left, bottom, left, bottom - r)
        lineTo(left, top + r)
        quadraticTo(left, top, left + r, top)
        close()
    }
}

private fun DrawScope.frontBox(f: Frame, frontTop: Float, color: Color) = with(f) {
    drawRoundRect(color, Offset(left, frontTop), Size(right - left, bottom - frontTop), CornerRadius(r, r))
}

private fun DrawScope.paper(f: Frame, paperTop: Float, edge: Boolean) = with(f) {
    val l = left + w * 0.05f
    val pw = right - left - w * 0.10f
    drawRoundRect(PAPER, Offset(l, paperTop), Size(pw, bottom - paperTop - h * 0.1f), CornerRadius(r * 0.5f, r * 0.5f))
    if (edge) drawRect(PAPER_EDGE, Offset(l, paperTop), Size(pw, h * 0.05f))
}

/** 1: a sheet with a light edge shows above a front that steps up on the right. */
private fun DrawScope.paperAndSlant(f: Frame, darker: Color, lighter: Color) = with(f) {
    drawPath(back(f), darker)
    paper(f, top + h * 0.17f, edge = true)
    val low = top + h * 0.34f
    val high = top + h * 0.24f
    val stepAt = left + w * 0.50f
    drawPath(Path().apply {
        moveTo(left, low + r)
        quadraticTo(left, low, left + r, low)
        lineTo(stepAt, low)
        lineTo(stepAt + w * 0.10f, high)
        lineTo(right - r, high)
        quadraticTo(right, high, right, high + r)
        lineTo(right, bottom - r)
        quadraticTo(right, bottom, right - r, bottom)
        lineTo(left + r, bottom)
        quadraticTo(left, bottom, left, bottom - r)
        close()
    }, lighter)
}

/** 2: one shape with a bold dark outline, and a line under the tab across the top. */
private fun DrawScope.outlined(f: Frame, color: Color, darker: Color) = with(f) {
    val shape = back(f)
    val line = h * 0.11f
    drawPath(shape, color)
    // The strip above the line, right of the tab, a shade darker.
    drawRect(darker, Offset(tabEnd + tabSlope, top + tabDrop), Size(right - tabEnd - tabSlope - r * 0.5f, line - tabDrop))
    val stroke = w * 0.04f
    drawPath(shape, OUTLINE, style = Stroke(width = stroke, join = StrokeJoin.Round))
    drawLine(OUTLINE, Offset(tabEnd + tabSlope, top + line), Offset(right - r * 0.6f, top + line), strokeWidth = stroke)
}

/** 3: a darker back, a white sheet, and a flat front (the look Glimpse first had, with a sheet in it). */
private fun DrawScope.paperAndFlat(f: Frame, color: Color, darker: Color) = with(f) {
    drawPath(back(f), darker)
    paper(f, top + h * 0.16f, edge = false)
    frontBox(f, top + h * 0.26f, color)
}

/** 4: one plain shape. */
private fun DrawScope.plain(f: Frame, color: Color) {
    drawPath(back(f), color)
}

/** 5: a darker back and a front that narrows towards the bottom, with a lighter top edge. */
private fun DrawScope.taperedFront(f: Frame, color: Color, darker: Color, lighter: Color) = with(f) {
    drawPath(back(f), darker)
    val frontTop = top + h * 0.17f
    fun front(t: Float) = Path().apply {
        val inset = w * 0.035f
        moveTo(left + r, t)
        lineTo(right - r, t)
        quadraticTo(right, t, right, t + r)
        lineTo(right - inset, bottom - r)
        quadraticTo(right - inset, bottom, right - inset - r, bottom)
        lineTo(left + inset + r, bottom)
        quadraticTo(left + inset, bottom, left + inset, bottom - r)
        lineTo(left, t + r)
        quadraticTo(left, t, left + r, t)
        close()
    }
    drawPath(front(frontTop - h * 0.03f), lighter)
    drawPath(front(frontTop), color)
}

/** 6: a lighter back and a front whose top left corner is cut on a slant. */
private fun DrawScope.slantedFront(f: Frame, color: Color, lighter: Color) = with(f) {
    drawPath(back(f), lighter)
    val low = top + h * 0.38f
    val high = top + h * 0.24f
    drawPath(Path().apply {
        moveTo(left, low)
        lineTo(left + w * 0.13f, high)
        lineTo(right - r, high)
        quadraticTo(right, high, right, high + r)
        lineTo(right, bottom - r)
        quadraticTo(right, bottom, right - r, bottom)
        lineTo(left + r, bottom)
        quadraticTo(left, bottom, left, bottom - r)
        close()
    }, color)
}

/**
 * Settings' Folder picture row: the six pictures in two rows of three, in the folder colour, the chosen one ringed in
 * the highlight colour. [enabled] false dims them (the folder-icon look is off) and they cannot be chosen.
 */
@Composable
fun FolderPictureChoices(chosen: Int, color: Color, enabled: Boolean, onPick: (Int) -> Unit) {
    val accent = LocalAccent.current
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        for (row in (1..com.glimpsemedia.glimpselauncher.data.FOLDER_PICTURES).chunked(3)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                for (n in row) {
                    val picked = n == chosen
                    androidx.compose.foundation.layout.Box(
                        Modifier.weight(1f).heightIn(min = 72.dp).aspectRatio(1.2f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LocalMenu.current.surface)
                            .border(if (picked) 3.dp else 1.dp, if (picked) accent else LocalMenu.current.dimText.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .then(if (enabled) Modifier.selectable(selected = picked, role = Role.RadioButton) { onPick(n) } else Modifier)
                            .semantics { contentDescription = "Folder picture $n" }
                            .padding(10.dp),
                    ) { FolderPicture(n, color) }
                }
            }
        }
    }
}

/** What TalkBack adds after a badged folder's name. */
fun folderBadgeSpoken(icon: com.glimpsemedia.glimpselauncher.model.TextIcon): String = "folder, ${icon.name}"

/**
 * A folder's badge over its picture: a circle at the bottom right, 38 % of the picture's width, in a darker shade of
 * the folder's colour, with the mono icon inside in white or near-black, whichever reads on it.
 */
@Composable
fun FolderBadge(icon: com.glimpsemedia.glimpselauncher.model.TextIcon, folderColor: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        val d = maxWidth * 0.38f
        val circle = Color(shaded(folderColor.toArgb(), -45))
        androidx.compose.foundation.layout.Box(
            Modifier.align(androidx.compose.ui.Alignment.BottomEnd).size(d).clip(androidx.compose.foundation.shape.CircleShape).background(circle),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            androidx.compose.material3.Icon(icon.vector, contentDescription = null, tint = readableOn(circle), modifier = Modifier.size(d * 0.6f))
        }
    }
}

/** A folder's colour badge: the subject's Glimpse Colour tile at the bottom right, the same size as a mono badge. */
@Composable
fun ColourFolderBadge(subject: com.glimpsemedia.glimpselauncher.model.TextIcon, modifier: Modifier = Modifier) {
    val name = com.glimpsemedia.glimpselauncher.model.GC_BADGES[subject.name] ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    @Suppress("DiscouragedApi")
    val id = remember(name) { context.resources.getIdentifier("gc_$name", "drawable", context.packageName) }
    if (id == 0) return
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(id), contentDescription = null,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).size(maxWidth * 0.42f),
        )
    }
}
