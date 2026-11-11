package com.glimpsemedia.glimpselauncher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.data.PackInfo
import com.glimpsemedia.glimpselauncher.model.DrawableEntry
import com.glimpsemedia.glimpselauncher.model.IconChoice
import com.glimpsemedia.glimpselauncher.model.matchesSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Asks for the icon picker for an app or built-in tile: its key, its name, and the app (null for a built-in tile). */
data class IconRequest(val key: String, val name: String, val app: AppInfo?)

/** Opens the icon picker; given by the home screen so every menu (tiles, bar, folders, All apps) can use it. */
val LocalChangeIcon = androidx.compose.runtime.staticCompositionLocalOf<(IconRequest) -> Unit> { {} }

/**
 * "Icon for <name>": Original, Choose a picture, the icons the installed packs suggest for this app, then one tab per
 * pack with a search box and its icons (only those on screen are loaded). Touching one saves it and closes.
 */
@Composable
fun IconPicker(request: IconRequest, onClose: () -> Unit) {
    val store = LocalIconStore.current ?: run { onClose(); return }
    val menu = LocalMenu.current
    val scope = rememberCoroutineScope()
    val current = store.choiceFor(request.key)
    val packs by produceState<List<PackInfo>?>(null) { value = withContext(Dispatchers.IO) { store.packs.installed() } }
    var tab by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            store.savePicture(request.key, uri)?.let { store.setChoice(request.key, it); onClose() }
        }
    }
    fun pick(c: IconChoice?) { store.setChoice(request.key, c); onClose() }

    MenuText { Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(menu.background).systemBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Icon for ${request.name}", color = menu.text, fontSize = 22.sp, maxLines = 2, modifier = Modifier.weight(1f).padding(end = 8.dp))
                BigButton("Close", onClose, Modifier.width(120.dp))
            }
            ButtonRow(2, minButtonWidth(listOf("Choose a picture"), 20f).dp) { i, m ->
                if (i == 0) BigButton("Original", { pick(null) }, m, primary = current == null)
                else BigButton("Choose a picture", { pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, m, primary = current is IconChoice.Picture)
            }
            val list = packs
            if (list == null) { Text("…", color = menu.dimText); return@Column }
            if (list.isEmpty()) { CardNote("No icon packs are installed."); return@Column }
            // What each installed pack draws for this app.
            val suggested by produceState<List<Pair<String, String>>>(emptyList(), request.key, list) {
                value = withContext(Dispatchers.IO) {
                    val app = request.app ?: return@withContext emptyList()
                    list.mapNotNull { p -> store.packs.suggestionFor(p.pkg, app.key)?.let { p.pkg to it } }
                }
            }
            if (suggested.isNotEmpty()) {
                CardTitle("Suggested")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((pack, d) in suggested) PackIconCell(pack, d, current == IconChoice.Pack(pack, d), Modifier.size(64.dp)) { pick(IconChoice.Pack(pack, d)) }
                }
            }
            val chosenTab = tab ?: list.first().pkg
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (p in list) BigButton(p.label, { tab = p.pkg; query = "" }, primary = p.pkg == chosenTab, fontSize = 15.sp)
            }
            CardNote("Icons come from the icon pack apps installed on this phone. Install more packs from the Play Store for more choice.")
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) }, singleLine = true, label = { Text("Search this pack") },
                textStyle = TextStyle(color = menu.text, fontSize = 18.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            val entries by produceState<List<DrawableEntry>?>(null, chosenTab) { value = withContext(Dispatchers.IO) { store.packs.drawableList(chosenTab) } }
            val shown = entries?.filter { matchesSearch(it.name, query) }
            if (shown == null) Text("…", color = menu.dimText)
            else LazyVerticalGrid(GridCells.Fixed(5), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                var last: String? = null
                for (e in shown) {
                    if (query.isBlank() && e.category != null && e.category != last) {
                        val heading = e.category
                        item(span = { GridItemSpan(5) }) { Text(heading, color = menu.dimText, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp)) }
                    }
                    last = e.category
                    item(key = e.name) { PackIconCell(chosenTab, e.name, current == IconChoice.Pack(chosenTab, e.name), Modifier.aspectRatio(1f)) { pick(IconChoice.Pack(chosenTab, e.name)) } }
                }
            }
        }
    } }
}

@Composable
private fun PackIconCell(pack: String, drawable: String, picked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val store = LocalIconStore.current
    val image by produceState<ImageBitmap?>(null, pack, drawable) { value = store?.packIcon(pack, drawable) }
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .border(if (picked) 3.dp else 0.dp, if (picked) LocalAccent.current else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp))
            .semantics { selected = picked; contentDescription = drawable.replace('_', ' ') }
            .clickable(role = Role.Button, onClick = onClick).padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) } }
}

/** A built-in tile's chosen icon (or the icon pack's for it), or null: then Glimpse's own drawing is shown. */
@Composable
fun rememberBuiltInIcon(key: String): ImageBitmap? {
    val store = LocalIconStore.current
    val image by produceState<ImageBitmap?>(null, key, store?.version) { value = store?.bitmapForKey(key) }
    return image
}

/** A built-in tile's icon: its chosen icon when it has one, else Glimpse's own drawing. */
@Composable
fun BuiltInIcon(key: String, own: @Composable () -> Unit) {
    val i = rememberBuiltInIcon(key)
    if (i != null) Image(i, contentDescription = null, modifier = Modifier.fillMaxSize()) else own()
}
