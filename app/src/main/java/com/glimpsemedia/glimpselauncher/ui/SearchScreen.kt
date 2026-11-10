package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.SearchHeading
import com.glimpsemedia.glimpselauncher.model.engineHost
import kotlinx.coroutines.delay

/** A folder no app would open, shown inside the search window: the folder and what is in it. */
data class Browsing(val folder: Entry, val children: List<Entry>)

/** The hint under the search box, naming what is searched. */
fun searchHint(folders: Boolean, web: Boolean): String {
    val what = listOf("apps", "settings", "help") + listOfNotNull("files".takeIf { folders }, "the web".takeIf { web })
    return "Type to search " + what.dropLast(1).joinToString(", ") + " and " + what.last()
}

/**
 * The search window: a box ready to type in, and the results under their headings as the words change.
 * [browsing] replaces the results with a folder's contents until [onBrowseBack].
 */
@Composable
fun SearchScreen(
    input: SearchInput,
    foldersOn: Boolean,
    appOf: (String) -> AppInfo?,
    onOpen: (SearchHit) -> Unit,
    onClose: () -> Unit,
    browsing: Browsing? = null,
    onBrowseBack: () -> Unit = {},
) = MenuText {
    val menu = LocalMenu.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(emptyMap<SearchHeading, Int>()) }
    LaunchedEffect(Unit) {
        delay(80)
        try { focus.requestFocus(); keyboard?.show() } catch (_: Exception) { }
    }
    // Read outside composition, so scrolling does not redraw the window.
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }.collect { if (it) { keyboard?.hide(); focusManager.clearFocus() } }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
    // Built again only when the words, the expanded headings or what is searched change, away from the screen's own
    // thread (Plus can search 200,000 names), so typing never waits; the last results stay until the new ones are ready.
    val sections by androidx.compose.runtime.produceState(emptyList(), input, query, more) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { buildResults(input, query, more) }
    }

    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Search", color = menu.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                "Close", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(role = Role.Button, onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(60); more = emptyMap() },
            singleLine = true,
            placeholder = { Text("Search", fontSize = 20.sp) },
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
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            if (browsing != null) {
                item { ResultRow(Icons.AutoMirrored.Filled.ArrowBack, "Back to the results", null, onBrowseBack) }
                item { Heading(browsing.folder.name) }
                if (browsing.children.isEmpty()) item { Text("This folder is empty.", color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }
                items(browsing.children) { e ->
                    ResultRow(if (e.isFolder) Icons.Outlined.Folder else Icons.Outlined.Description, e.name, null) {
                        onOpen(if (e.isFolder) SearchHit.Folder(e) else SearchHit.File(e))
                    }
                }
                return@LazyColumn
            }
            if (query.isBlank()) {
                item { Text(searchHint(foldersOn, input.engines.isNotEmpty()), color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }
            } else if (sections.isEmpty()) {
                item { Text("Nothing found for “${query.trim()}”.", color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(8.dp)) }
            }
            for (sec in sections) {
                item(key = "h:" + sec.heading.name) { Heading(sec.heading.title) }
                items(sec.shown) { hit -> HitRow(hit, query.trim(), appOf) { onOpen(hit) } }
                if (sec.shown.size < sec.total) {
                    item(key = "more:" + sec.heading.name) {
                        Text(
                            "Load more (${sec.total - sec.shown.size} left)", color = LocalAccent.current, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { more = more + (sec.heading to (more[sec.heading] ?: 0) + 1) }.padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
                if (sec.heading == SearchHeading.Folders || (sec.heading == SearchHeading.Files && sections.none { it.heading == SearchHeading.Folders })) {
                    if (input.files?.cut == true) item { Text("Some files were not searched", color = menu.dimText, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) = Text(
    text, color = LocalMenu.current.dimText, fontSize = 17.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
)

@Composable
private fun HitRow(hit: SearchHit, query: String, appOf: (String) -> AppInfo?, onClick: () -> Unit) = when (hit) {
    is SearchHit.App -> {
        val app = appOf(hit.app.key)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (app != null) AppIcon(app, 40.dp)
            Text(hit.app.label, color = LocalMenu.current.text, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    is SearchHit.File -> ResultRow(Icons.Outlined.Description, hit.entry.name, hit.entry.path, onClick)
    is SearchHit.Folder -> ResultRow(Icons.Outlined.Folder, hit.entry.name, hit.entry.path.ifEmpty { null }, onClick)
    is SearchHit.Menu -> ResultRow(Icons.Outlined.Settings, hit.hit.label, "Settings › " + SettingsMap.sections.getValue(hit.hit.section).title, onClick)
    is SearchHit.Help -> ResultRow(Icons.AutoMirrored.Outlined.HelpOutline, hit.title, "Help", onClick)
    is SearchHit.Online -> ResultRow(Icons.Outlined.Public, "Search ${hit.engine.name} for “$query”", engineHost(hit.engine), onClick)
}

@Composable
private fun ResultRow(icon: ImageVector, title: String, sub: String?, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
) {
    Icon(icon, contentDescription = null, tint = LocalMenu.current.text, modifier = Modifier.size(32.dp))
    Column(Modifier.weight(1f)) {
        Text(title, color = LocalMenu.current.text, fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, color = LocalMenu.current.dimText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
