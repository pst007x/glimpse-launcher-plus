package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.model.EngineCheck
import com.glimpsemedia.glimpselauncher.model.FolderLocation
import com.glimpsemedia.glimpselauncher.model.SearchEngine
import com.glimpsemedia.glimpselauncher.model.checkEngine
import com.glimpsemedia.glimpselauncher.model.engineHost
import com.glimpsemedia.glimpselauncher.model.moved

/** A small button in a list row, like the bar list's Hold action. */
@Composable
private fun RowButton(text: String, onClick: () -> Unit) = Text(
    text, color = LocalMenu.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
    modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(LocalMenu.current.surface)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 13.dp),
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ListRow(title: String, sub: String, buttons: @Composable () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalMenu.current.background).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(title, color = LocalMenu.current.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(sub, color = LocalMenu.current.dimText, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    // The buttons wrap onto a second line on a narrow phone or with large text.
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
}

/** Add folder location(s): the picked folders, each with Remove, and Add a folder. */
@Composable
fun SearchFolderList(folders: List<FolderLocation>, available: (FolderLocation) -> Boolean, onAdd: () -> Unit, onRemove: (FolderLocation) -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (folders.isEmpty()) CardNote("No folders yet. Search looks only inside the folders you add, and only at the names of files and folders.")
        for (f in folders) {
            ListRow(f.name, if (available(f)) "Searched" else "Not available — pick it again or remove it") {
                RowButton("Remove") { onRemove(f) }
            }
        }
        BigButton("Add a folder", onAdd)
    }

/** Add search engine(s): the user's engines in order, with Move up, Move down, Edit and Remove, and Add a search engine. */
@Composable
fun SearchEngineList(engines: List<SearchEngine>, onChange: (List<SearchEngine>) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // The engine being edited (its place), -1 for a new one, null when the dialog is closed.
    var editing by remember { mutableStateOf<Int?>(null) }
    if (engines.isEmpty()) CardNote("No search engines yet. None come with Glimpse: add the ones you want, with %s in the address where your search words go.")
    engines.forEachIndexed { i, e ->
        ListRow(e.name, engineHost(e)) {
            if (i > 0) RowButton("Move up") { onChange(engines.moved(i, -1)) }
            if (i < engines.size - 1) RowButton("Move down") { onChange(engines.moved(i, 1)) }
            RowButton("Edit") { editing = i }
            RowButton("Remove") { onChange(engines.filterIndexed { j, _ -> j != i }) }
        }
    }
    BigButton("Add a search engine", { editing = -1 })
    editing?.let { at ->
        EngineDialog(engines.getOrNull(at), onClose = { editing = null }) { e ->
            onChange(if (at in engines.indices) engines.mapIndexed { j, old -> if (j == at) e else old } else engines + e)
            editing = null
        }
    }
}

@Composable
private fun EngineDialog(current: SearchEngine?, onClose: () -> Unit, onSave: (SearchEngine) -> Unit) {
    var name by remember { mutableStateOf(current?.name ?: "") }
    var address by remember { mutableStateOf(current?.url ?: "") }
    var errors by remember { mutableStateOf<EngineCheck.Bad?>(null) }
    MenuDialog(title = if (current == null) "Add a search engine" else "Edit the search engine", onClose = onClose) {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40); errors = null }, singleLine = true, label = { Text("Name") },
            textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 20.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        errors?.nameError?.let { Text(it, color = LocalAccent.current, fontSize = 17.sp) }
        OutlinedTextField(
            value = address, onValueChange = { address = it.take(500); errors = null }, singleLine = true, label = { Text("Web address") },
            placeholder = { Text("https://example.com/search?q=%s") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 20.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        errors?.addressError?.let { Text(it, color = LocalAccent.current, fontSize = 17.sp) }
        Text("For example: https://example.com/search?q=%s", color = LocalMenu.current.dimText, fontSize = 16.sp)
        BigButton("Save", {
            when (val c = checkEngine(name, address)) {
                is EngineCheck.Ok -> onSave(c.engine)
                is EngineCheck.Bad -> errors = c
            }
        }, primary = true)
        BigButton("Cancel", onClose)
    }
}
