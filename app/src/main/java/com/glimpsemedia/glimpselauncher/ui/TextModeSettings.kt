package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SEARCH
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SETTINGS
import com.glimpsemedia.glimpselauncher.model.SYSTEM_SETTINGS
import com.glimpsemedia.glimpselauncher.model.TextFolder
import com.glimpsemedia.glimpselauncher.model.TextIcon
import com.glimpsemedia.glimpselauncher.model.appMoved
import com.glimpsemedia.glimpselauncher.model.entryMoved
import com.glimpsemedia.glimpselauncher.model.folderMoved
import com.glimpsemedia.glimpselauncher.model.withApp
import com.glimpsemedia.glimpselauncher.model.withEntry
import com.glimpsemedia.glimpselauncher.model.withFolder
import com.glimpsemedia.glimpselauncher.model.withoutApp
import com.glimpsemedia.glimpselauncher.model.withoutEntry
import com.glimpsemedia.glimpselauncher.model.withoutFolder
import com.glimpsemedia.glimpselauncher.model.onScreenMove
import com.glimpsemedia.glimpselauncher.model.shownInScreenOrder

const val SAME_ORDER_HINT = "Shown here in the same order as on the screen."

/** A folder name as typed, tidied; null when nothing is left. */
fun cleanTextFolderName(text: String): String? = text.trim().take(30).ifEmpty { null }

/**
 * Settings > Home screen > Text mode's list and folders: add, remove and move apps; add, rename, re-icon, fill, move and delete
 * folders. Each change is saved at once.
 */
@Composable
fun TextModeEditor(state: LauncherState) {
    val t = state.textLayout
    val menu = LocalMenu.current
    if (t == null) {
        Text("Turn on text mode to set up its list and folders.", color = menu.dimText, fontSize = 18.sp)
        return
    }
    // Where the app picker adds to: "" for the main list, else a folder's id.
    var pickingFor by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<String?>(null) }
    var naming by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubHeading("Apps")
        Text(SAME_ORDER_HINT, color = menu.dimText, fontSize = 16.sp)
        // Listed as the screen shows them: the last app at the top, the first (lowest) at the bottom.
        val apps = shownInScreenOrder(t.apps)
        apps.forEachIndexed { i, key ->
            EditRow(
                label = textEntryLabel(key, state) ?: key, icon = null,
                canUp = i > 0, canDown = i < apps.size - 1,
                onUp = { state.changeTextLayout(t.appMoved(key, onScreenMove(-1))) },
                onDown = { state.changeTextLayout(t.appMoved(key, onScreenMove(1))) },
                onRemove = { state.changeTextLayout(t.withoutApp(key)) },
            )
        }
        BigButton("Add an app", { pickingFor = "" })

        SubHeading("Folders")
        Text(SAME_ORDER_HINT, color = menu.dimText, fontSize = 16.sp)
        val folders = shownInScreenOrder(t.folders)
        folders.forEachIndexed { i, f ->
            EditRow(
                label = f.name, icon = f.icon.vector,
                canUp = i > 0, canDown = i < folders.size - 1,
                onUp = { state.changeTextLayout(t.folderMoved(f.id, onScreenMove(-1))) },
                onDown = { state.changeTextLayout(t.folderMoved(f.id, onScreenMove(1))) },
                onRemove = null,
                onClick = { editing = f.id },
            )
        }
        BigButton("Add a folder", { naming = true })
    }

    if (naming) {
        NameDialog("Add a folder", "", onSave = { name ->
            val f = TextFolder("tf" + System.currentTimeMillis(), name, TextIcon.Folder, emptyList())
            state.changeTextLayout(t.withFolder(f))
            naming = false
            editing = f.id
        }, onClose = { naming = false })
    }
    editing?.let { id ->
        val f = state.textLayout?.folders?.firstOrNull { it.id == id }
        if (f == null) editing = null else FolderEditor(state, f, onAdd = { pickingFor = id }, onClose = { editing = null })
    }
    pickingFor?.let { target ->
        val now = state.textLayout ?: return@let
        val have = if (target == "") now.apps else now.folders.firstOrNull { it.id == target }?.entries.orEmpty()
        TextEntryPicker(
            state = state, have = have.toSet(),
            onPick = { key ->
                val l = state.textLayout ?: return@TextEntryPicker
                if (target == "") state.changeTextLayout(l.withApp(key))
                else l.folders.firstOrNull { it.id == target }?.let { state.changeTextLayout(l.withFolder(it.withEntry(key))) }
            },
            onClose = { pickingFor = null },
        )
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(text, color = LocalMenu.current.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

/** A small round button; hidden (but keeping its place) when [visible] is false. */
@Composable
private fun SmallButton(icon: ImageVector, description: String, visible: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
            .then(if (visible) Modifier.background(LocalMenu.current.button).clickable(onClickLabel = description, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (visible) Icon(icon, contentDescription = description, tint = LocalMenu.current.text, modifier = Modifier.size(28.dp))
    }
}

/** One app or folder row: its name (and icon), then Up, Down and Remove. */
@Composable
private fun EditRow(
    label: String, icon: ImageVector?, canUp: Boolean, canDown: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onRemove: (() -> Unit)?, onClick: (() -> Unit)? = null,
) {
    val menu = LocalMenu.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Change $label", onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = menu.text, modifier = Modifier.size(30.dp))
        Text(label, color = menu.text, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        SmallButton(Icons.Filled.KeyboardArrowUp, "Move $label up", canUp, onUp)
        SmallButton(Icons.Filled.KeyboardArrowDown, "Move $label down", canDown, onDown)
        if (onRemove != null) SmallButton(Icons.Filled.Close, "Remove $label", true, onRemove)
    }
}

/** A box for a folder's name, then Save. */
@Composable
private fun NameDialog(title: String, start: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(start) }
    MenuDialog(title = title, onClose = onClose) {
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(30) }, singleLine = true, label = { Text("Name") },
            textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Save", { cleanTextFolderName(text)?.let(onSave) }, primary = true)
    }
}

/** A full-screen panel for one folder: its name, its icon, its apps, and Delete. */
@Composable
private fun FolderEditor(state: LauncherState, f: TextFolder, onAdd: () -> Unit, onClose: () -> Unit) {
    val menu = LocalMenu.current
    var name by remember(f.id) { mutableStateOf(f.name) }
    var deleting by remember { mutableStateOf(false) }
    var choosingColour by remember { mutableStateOf(false) }
    fun change(new: TextFolder) { state.textLayout?.let { state.changeTextLayout(it.withFolder(new)) } }
    // Done (or Back) keeps a name that was typed but not saved.
    fun done() {
        cleanTextFolderName(name)?.takeIf { it != f.name }?.let { change(f.copy(name = it)) }
        onClose()
    }
    Dialog(onDismissRequest = ::done, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(f.name, color = menu.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() })
                Text(
                    "Done", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                        .clickable(onClick = ::done).padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(30) }, singleLine = true, label = { Text("Name") },
                    textStyle = TextStyle(color = menu.text, fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                BigButton("Save the name", { cleanTextFolderName(name)?.let { change(f.copy(name = it)) } })

                SubHeading("Icon")
                TextIcon.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { icon ->
                            val chosen = icon == f.icon
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (chosen) LocalAccent.current else menu.button)
                                    .clickable(onClickLabel = "Use the ${icon.name} icon") { change(f.copy(icon = icon)) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val ink = if (chosen) readableOn(LocalAccent.current) else menu.text
                                Icon(icon.vector, contentDescription = null, tint = ink, modifier = Modifier.size(32.dp))
                                Text(icon.name, color = ink, fontSize = 16.sp, textAlign = TextAlign.Center)
                            }
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }

                SubHeading("Icon colour")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(f.icon.vector, contentDescription = null, tint = if (f.color != 0) androidx.compose.ui.graphics.Color(f.color) else menu.text, modifier = Modifier.size(36.dp))
                    Text(if (f.color == 0) "Same as the text" else "Its own colour", color = menu.text, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    BigButton("Change", { choosingColour = true }, Modifier.widthIn(max = 140.dp), primary = true, fontSize = 17.sp)
                }

                SubHeading("Apps in this folder")
                if (f.entries.isEmpty()) Text("No apps yet.", color = menu.dimText, fontSize = 18.sp)
                else Text(SAME_ORDER_HINT, color = menu.dimText, fontSize = 16.sp)
                val entries = shownInScreenOrder(f.entries)
                entries.forEachIndexed { i, key ->
                    EditRow(
                        label = textEntryLabel(key, state) ?: key, icon = null,
                        canUp = i > 0, canDown = i < entries.size - 1,
                        onUp = { change(f.entryMoved(key, onScreenMove(-1))) },
                        onDown = { change(f.entryMoved(key, onScreenMove(1))) },
                        onRemove = { change(f.withoutEntry(key)) },
                    )
                }
                BigButton("Add an app", onAdd)
                BigButton("Delete the folder", { deleting = true })
            }
        }
    }
    if (choosingColour) {
        // 0 ("Same as the text") follows Settings > Home screen > Text mode > Text colour.
        // Its own window, so it shows above the folder editor's window.
        Dialog(onDismissRequest = { choosingColour = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ColourPicker("Icon colour: ${f.name}", f.color, "Same as the text", onPick = { change(f.copy(color = it)) }, onClose = { choosingColour = false })
        }
    }
    if (deleting) {
        MenuDialog(title = "Delete ${f.name}?", onClose = { deleting = false }) {
            Text("The apps stay on the phone.", color = menu.dimText, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BigButton("Delete the folder", {
                deleting = false
                state.textLayout?.let { state.changeTextLayout(it.withoutFolder(f.id)) }
                onClose()
            }, primary = true)
            BigButton("Keep it", { deleting = false })
        }
    }
}

/** The A to Z apps not yet in the list or folder, with Glimpse Settings and System settings first. */
@Composable
private fun TextEntryPicker(state: LauncherState, have: Set<String>, onPick: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SearchableAppList(
            "Add an app", "Done",
            state.apps.values.filter { it.key !in have }.sortedBy { it.label.lowercase() },
            "Every app is already here.", onClose, { onPick(it.key) }, pickLabel = "Add",
            rowEnd = { Icon(Icons.Filled.Add, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(36.dp)) },
            topContent = {
                for (key in listOf(GLIMPSE_SEARCH, GLIMPSE_SETTINGS, SYSTEM_SETTINGS)) {
                    if (key !in have) BigButton("Add ${textEntryLabel(key, state)}", { onPick(key) })
                }
            },
        )
    }
}
