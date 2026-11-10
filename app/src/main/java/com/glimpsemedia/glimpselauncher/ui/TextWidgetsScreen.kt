package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.WidgetSupport
import com.glimpsemedia.glimpselauncher.model.Item
import com.glimpsemedia.glimpselauncher.model.TEXT_WIDGET_HEIGHTS_DP

/** The room at the sides of, above and between the widgets, in dp. */
const val WIDGET_GAP_DP = 8

const val NO_WIDGETS_LOCKED = "No widgets yet. Unlock, then hold here to add one."

/**
 * Text mode's widgets screen (swipe left from the text screen): one column of full-width widgets from the top.
 * Unlocked, holding a widget opens its menu and holding the space opens Add widget; locked, widgets just work.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextWidgetsScreen(
    state: LauncherState,
    widgets: WidgetSupport,
    onAddWidget: () -> Unit,
    onWidgetMenu: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = state.textLayout?.widgets.orEmpty()
    val unlocked = state.unlocked
    val toggle = LocalToggleTextMode.current
    val packageName = androidx.compose.ui.platform.LocalContext.current.packageName
    var spaceMenu by remember { mutableStateOf(false) }
    // Locking or Home closes the Add widget menu.
    androidx.compose.runtime.LaunchedEffect(unlocked, state.homePresses) { spaceMenu = false }
    val widthDp = (LocalConfiguration.current.screenWidthDp - 2 * WIDGET_GAP_DP).dp
    val scroll = rememberScrollState()
    // Holding anywhere not covered by a widget offers Add widget (while unlocked).
    Box(modifier.fillMaxSize().then(if (unlocked) Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { spaceMenu = true }) } else Modifier)) {
        Column(Modifier.fillMaxSize()) {
            // While unlocked, Add widget stays at the top, above the widgets, so it can be reached when the screen is full.
            if (unlocked) BigButton(
                "Add widget", onAddWidget,
                Modifier.padding(start = WIDGET_GAP_DP.dp, end = WIDGET_GAP_DP.dp, top = WIDGET_GAP_DP.dp), primary = true,
            )
            Column(
                // Only scrolls when the widgets are taller than the room, so up and down swipes otherwise reach the screen.
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(scroll, enabled = scroll.maxValue > 0)
                    .padding(horizontal = WIDGET_GAP_DP.dp, vertical = WIDGET_GAP_DP.dp),
                verticalArrangement = Arrangement.spacedBy(WIDGET_GAP_DP.dp),
            ) {
                list.forEach { w ->
                    key(w.id) {
                        val h = TEXT_WIDGET_HEIGHTS_DP[w.height.coerceIn(0, 3)].dp
                        Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(20.dp))) {
                            WidgetBox(widgets, Item.Widget(w.id, w.provider, 1, 1), widthDp, h, unlocked)
                            // Unlocked, the widget only shows: a hold opens its menu, and a tap does nothing, except on the
                            // Text mode switch, which works locked or unlocked.
                            val isSwitch = isTextModeSwitch(w.provider, packageName)
                            if (unlocked) Box(Modifier.matchParentSize().pointerInput(w.id, isSwitch) {
                                detectTapGestures(onTap = { if (isSwitch) toggle() }, onLongPress = { onWidgetMenu(w.id) })
                            })
                        }
                    }
                }
                if (list.isEmpty() && !unlocked) Text(
                    NO_WIDGETS_LOCKED, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    style = TextStyle(color = Color.White, fontSize = 20.sp, shadow = Shadow(Color.Black, Offset(2f, 2f), 6f)),
                )
            }
        }
    }
    if (spaceMenu && unlocked) {
        MenuDialog(title = "Widgets", onClose = { spaceMenu = false }) {
            BigButton("Add widget", { spaceMenu = false; onAddWidget() }, primary = true)
        }
    }
}
