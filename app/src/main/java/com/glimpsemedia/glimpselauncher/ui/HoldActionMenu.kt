package com.glimpsemedia.glimpselauncher.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.model.HoldAction
import com.glimpsemedia.glimpselauncher.model.PhoneAction
import com.glimpsemedia.glimpselauncher.model.describe
import com.glimpsemedia.glimpselauncher.model.dialable
import com.glimpsemedia.glimpselauncher.model.webAddress

private enum class HoldStep { Start, App, Web, Phone, Call, Text }

/**
 * Chooses a tile's hold action: an app, a web address or one of the phone's own things, or none.
 * [onSet] receives the new action (null removes it); the menu then closes.
 */
@Composable
fun HoldActionMenu(
    title: String,
    current: HoldAction?,
    apps: List<AppInfo>,
    appLabel: (String) -> String?,
    onSet: (HoldAction?) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(HoldStep.Start) }
    fun save(action: HoldAction?) {
        onSet(action)
        Toast.makeText(context, if (action == null) "Hold action removed" else "Hold action saved", Toast.LENGTH_SHORT).show()
        onClose()
    }
    when (step) {
        HoldStep.Start -> MenuDialog(title = "Hold action: $title", onClose = onClose) {
            Text("Now: " + (current?.describe(appLabel) ?: "nothing"), color = LocalMenu.current.dimText, fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BigButton("Open an app", { step = HoldStep.App })
            BigButton("Open a web address", { step = HoldStep.Web })
            BigButton("A phone action", { step = HoldStep.Phone })
            if (current != null) BigButton("Remove the hold action", { save(null) })
        }
        HoldStep.App -> SearchableAppList(
            "Hold action: choose an app", "Back", apps.sortedBy { it.label.lowercase() }, "No apps.",
            onClose = { step = HoldStep.Start }, onPick = { save(HoldAction.OpenApp(it.key)) }, pickLabel = "Choose",
        )
        HoldStep.Web -> {
            var text by remember { mutableStateOf((current as? HoldAction.Web)?.url ?: "") }
            var refused by remember { mutableStateOf(false) }
            MenuDialog(title = "Web address", onClose = onClose) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(500); refused = false }, singleLine = true,
                    placeholder = { Text("example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 20.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                if (refused) Text("That is not a web address", color = LocalAccent.current, fontSize = 18.sp)
                BigButton("Save", { webAddress(text)?.let { save(HoldAction.Web(it)) } ?: run { refused = true } }, primary = true)
                BigButton("Back", { step = HoldStep.Start })
            }
        }
        HoldStep.Phone -> MenuDialog(title = "A phone action", onClose = onClose) {
            BigButton(PhoneAction.Torch.title, { save(HoldAction.Phone(PhoneAction.Torch)) })
            BigButton("Call a number", { step = HoldStep.Call })
            BigButton("Text a number", { step = HoldStep.Text })
            for (p in PhoneAction.entries) if (p != PhoneAction.Torch) BigButton(p.title, { save(HoldAction.Phone(p)) })
            BigButton("Open the notes screen", { save(HoldAction.Notes) })
            BigButton("Back", { step = HoldStep.Start })
        }
        HoldStep.Call, HoldStep.Text -> {
            val calling = step == HoldStep.Call
            var text by remember(step) {
                mutableStateOf((current as? HoldAction.Call)?.number?.takeIf { calling } ?: (current as? HoldAction.Text)?.number?.takeIf { !calling } ?: "")
            }
            var refused by remember(step) { mutableStateOf(false) }
            MenuDialog(title = if (calling) "Call a number" else "Text a number", onClose = onClose) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(30); refused = false }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 24.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth(),
                )
                if (refused) Text("That is not a phone number", color = LocalAccent.current, fontSize = 18.sp)
                BigButton("Save", {
                    dialable(text)?.let { save(if (calling) HoldAction.Call(it) else HoldAction.Text(it)) } ?: run { refused = true }
                }, primary = true)
                BigButton("Back", { step = HoldStep.Phone })
            }
        }
    }
}
