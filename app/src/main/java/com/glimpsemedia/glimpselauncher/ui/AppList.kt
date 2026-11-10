package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.model.groupedByInitial
import com.glimpsemedia.glimpselauncher.model.relatedApps
import com.glimpsemedia.glimpselauncher.model.searchApps

/**
 * A full-screen list of apps A to Z under a letter heading each (A, B, C...), with a search box above it: the
 * list narrows with every letter typed, to the apps whose name begins with what has been typed. After those come
 * the related apps: typing "internet" lists the browsers, "social" the social apps, and so on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchableAppList(
    title: String,
    closeLabel: String,
    apps: List<AppInfo>,
    emptyText: String,
    onClose: () -> Unit,
    onPick: (AppInfo) -> Unit,
    rowEnd: (@Composable () -> Unit)? = null,
    pickLabel: String = "Open",
    /** Holding a row: used by All apps, when unlocked, to add the app to the screen. */
    onHold: ((AppInfo) -> Unit)? = null,
    hint: String? = null,
    /** Shown instead of the [title] heading, such as the All apps Filter button. */
    titleContent: (@Composable () -> Unit)? = null,
    /** Shown above the A to Z list, such as text mode's built-in entries. */
    topContent: (@Composable () -> Unit)? = null,
    /** All apps: the search box is ready to type in, keyboard up, as soon as the list opens; scrolling puts it away. */
    searchOnOpen: Boolean = false,
) = MenuText {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    if (searchOnOpen) {
        LaunchedEffect(Unit) {
            // After the panel has started to appear, so the field can take focus.
            delay(80)
            try { focus.requestFocus(); keyboard?.show() } catch (_: Exception) { }
        }
        // Scrolling the list puts the keyboard away so the whole list can be seen.
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) { keyboard?.hide(); focusManager.clearFocus() }
        }
        // Closing the list (an app opened, Done, Home) never leaves the keyboard behind.
        DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
    }
    val menu = LocalMenu.current
    var query by remember { mutableStateOf("") }
    val shown = searchApps(apps, query) { it.label }
    val groups = groupedByInitial(shown) { it.label }
    val related = relatedApps(apps, query, shown, { it.label }, { it.topics })
    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (titleContent != null) Box(Modifier.weight(1f)) { titleContent() }
            else Text(title, color = menu.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                closeLabel, color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        if (hint != null) {
            Text(hint, color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            singleLine = true,
            placeholder = { Text("Search apps", fontSize = 20.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = menu.dimText, modifier = Modifier.size(30.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Clear the search", tint = menu.text,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable { query = "" }.padding(10.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = TextStyle(fontSize = 24.sp),
            colors = menuFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(focus),
        )
        if (topContent != null) Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { topContent() }
        if (shown.isEmpty() && related.isEmpty()) {
            Text(
                if (apps.isEmpty() || query.isBlank()) emptyText else "No app begins with “${query.trim()}”.",
                color = menu.text, fontSize = 20.sp, modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((letter, group) in groups) {
                stickyHeader(key = "header-$letter") { ListHeading(letter, big = true) }
                items(group, key = { it.key }) { app -> AppRow(app, pickLabel, onPick, rowEnd, onHold) }
            }
            for ((topic, group) in related) {
                stickyHeader(key = "related-${topic.name}") { ListHeading("Related: ${topic.title}", big = false) }
                items(group, key = { "${topic.name}-${it.key}" }) { app -> AppRow(app, pickLabel, onPick, rowEnd, onHold) }
            }
        }
    }
}

@Composable
private fun ListHeading(text: String, big: Boolean) {
    val menu = LocalMenu.current
    Text(
        text, color = LocalAccent.current, fontSize = if (big) 28.sp else 22.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().background(menu.background).padding(start = 8.dp, top = 6.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(app: AppInfo, pickLabel: String, onPick: (AppInfo) -> Unit, rowEnd: (@Composable () -> Unit)?, onHold: ((AppInfo) -> Unit)? = null) {
    val menu = LocalMenu.current
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(menu.surface)
            .combinedClickable(
                onClickLabel = "$pickLabel ${app.label}",
                onClick = { onPick(app) },
                onLongClickLabel = if (onHold != null) "Add ${app.label} to the screen" else null,
                onLongClick = if (onHold != null) ({ haptic.performHapticFeedback(HapticFeedbackType.LongPress); onHold(app) }) else null,
            ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 56.dp)
        Text(
            app.label, color = menu.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
        rowEnd?.invoke()
    }
}
