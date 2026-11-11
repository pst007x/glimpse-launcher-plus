package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How many buttons at least [minDp] wide, [gapDp] apart, fit side by side in [widthDp]: at least one, at most [count]. */
fun buttonsPerRow(widthDp: Float, count: Int, minDp: Float, gapDp: Float): Int =
    ((widthDp + gapDp) / (minDp + gapDp)).toInt().coerceIn(1, maxOf(1, count))

/** [count] buttons in rows of at most [perRow], as even as can be, the fuller rows first (5 in threes: 3 + 2). */
fun balancedRows(count: Int, perRow: Int): List<Int> {
    if (count <= 0) return emptyList()
    val rows = (count + perRow - 1) / perRow
    return List(rows) { r -> count / rows + if (r < count % rows) 1 else 0 }
}

/**
 * A rough width (dp, at the phone's usual text size) a button needs for the longest of [labels] at [fontSp] on one
 * line, with its side padding; never under the 48 dp a finger needs.
 */
fun minButtonWidth(labels: List<String>, fontSp: Float): Float =
    maxOf(48f, (labels.maxOfOrNull { it.length } ?: 0) * fontSp * 0.58f + 32f)

/**
 * Buttons side by side that share the width, flowing onto more rows when they would not fit (a narrow screen, or
 * larger menu text): each needs [minWidth] at the phone's usual text size, more as the text grows. [button] draws
 * button [index] with the modifier it must use.
 */
@Composable
fun ButtonRow(count: Int, minWidth: Dp, modifier: Modifier = Modifier, gap: Dp = 8.dp, button: @Composable RowScope.(index: Int, modifier: Modifier) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val perRow = buttonsPerRow(maxWidth.value, count, minWidth.value * fontScale, gap.value)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            var start = 0
            for (n in balancedRows(count, perRow)) {
                val first = start
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (i in first until first + n) button(i, Modifier.weight(1f))
                }
                start += n
            }
        }
    }
}

/**
 * Whether a name with a button beside it puts the button underneath instead: when the row, measured in the menu's
 * text size, has under 330 dp, the name would be squeezed (a small phone, or large menu text).
 */
fun buttonBelow(widthDp: Float, fontScale: Float): Boolean = widthDp / fontScale < 330f

/**
 * A name (and anything before it) with a button (or buttons) at the end: side by side when there is room, else the
 * button goes underneath at full width (see [buttonBelow]). [start] should give its main text `Modifier.weight(1f)`.
 */
@Composable
fun NameAndButton(modifier: Modifier = Modifier, start: @Composable RowScope.() -> Unit, end: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (buttonBelow(maxWidth.value, fontScale)) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { start() }
            end()
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            start()
            // Big buttons fill the width they are given: beside the name they get only what their text needs.
            Box(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)) { end() }
        }
    }
}
