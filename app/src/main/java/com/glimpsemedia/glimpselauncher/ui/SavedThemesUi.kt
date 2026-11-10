package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.SAVED_THEMES_MAX
import com.glimpsemedia.glimpselauncher.data.SavedTheme
import com.glimpsemedia.glimpselauncher.data.cleanThemeName
import com.glimpsemedia.glimpselauncher.data.nextThemeName

/** What the person asked to do with saved themes; the activity does it (it can set wallpapers). */
sealed interface SavedThemeAction {
    data class Save(val name: String) : SavedThemeAction
    data class Apply(val theme: SavedTheme) : SavedThemeAction
    data class Rename(val id: String, val name: String) : SavedThemeAction
    data class Update(val id: String) : SavedThemeAction
    data class Delete(val id: String) : SavedThemeAction
}

/** A box for a theme's name, then Save. */
@Composable
private fun ThemeNameDialog(title: String, start: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(start) }
    MenuDialog(title = title, onClose = onClose) {
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(30) }, singleLine = true,
            textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Save", { cleanThemeName(text)?.let { onSave(it) } }, primary = true)
    }
}

/** "Save current theme" (or a note when there are already [SAVED_THEMES_MAX]). */
@Composable
fun SaveThemeButton(saved: List<SavedTheme>, onAction: (SavedThemeAction) -> Unit) {
    var naming by remember { mutableStateOf(false) }
    if (saved.size >= SAVED_THEMES_MAX) {
        Text("$SAVED_THEMES_MAX themes saved: delete one to save another.", color = LocalMenu.current.dimText, fontSize = 16.sp)
    } else {
        BigButton("Save current theme", { naming = true })
    }
    if (naming) ThemeNameDialog("Save current theme", nextThemeName(saved.map { it.name }), onSave = { onAction(SavedThemeAction.Save(it)); naming = false }, onClose = { naming = false })
}

/** The menu for one saved theme: Rename, Update to the current look, Delete (asks first). */
@Composable
fun SavedThemeMenu(theme: SavedTheme, onAction: (SavedThemeAction) -> Unit, onClose: () -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    when {
        renaming -> ThemeNameDialog("Rename the theme", theme.name, onSave = { onAction(SavedThemeAction.Rename(theme.id, it)); onClose() }, onClose = onClose)
        deleting -> MenuDialog(title = "Delete ${theme.name}?", onClose = onClose) {
            BigButton("Delete the theme", { onAction(SavedThemeAction.Delete(theme.id)); onClose() }, primary = true)
            BigButton("Keep it", onClose)
        }
        else -> MenuDialog(title = theme.name, onClose = onClose) {
            BigButton("Rename", { renaming = true })
            BigButton("Update to the current look", { onAction(SavedThemeAction.Update(theme.id)); onClose() })
            BigButton("Delete", { deleting = true })
        }
    }
}
