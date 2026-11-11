package com.glimpsemedia.glimpselauncher.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.glimpsemedia.glimpselauncher.data.PackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Settings › Tiles and icons › Icon pack: the chosen pack (or None) and a Choose button opening the list. */
@Composable
fun IconPackRow(label: String, chosen: String, onChoose: (String) -> Unit) {
    val packs = LocalIconStore.current?.packs
    var open by remember { mutableStateOf(false) }
    val installed by produceState<List<PackInfo>?>(null, open) { value = withContext(Dispatchers.IO) { packs?.installed().orEmpty() } }
    val store = LocalIconStore.current
    val savedCopies by produceState(false, chosen, installed) { value = withContext(Dispatchers.IO) { store?.usingSavedCopies() == true } }
    val name = when {
        chosen.isEmpty() -> "None"
        else -> installed?.firstOrNull { it.pkg == chosen }?.label
            ?: if (installed == null) "…" else if (savedCopies) "$chosen (not installed, using saved icons)" else "$chosen (not installed)"
    }
    CardTitle(label)
    NameAndButton(start = {
        Text(name, color = LocalMenu.current.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
    }) { BigButton("Choose", { open = true }) }
    if (open) IconPackChooser(chosen, installed.orEmpty(), onPick = { onChoose(it); open = false }, onClose = { open = false })
}

/** The list of packs to choose from: None first, then every installed pack with its icon, A to Z. */
@Composable
fun IconPackChooser(chosen: String, installed: List<PackInfo>, onPick: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val packs = LocalIconStore.current?.packs
    MenuDialog("Icon pack", onClose) {
        PackLine("None", null, chosen.isEmpty()) { onPick("") }
        for (p in installed) {
            val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, p.pkg) {
                value = withContext(Dispatchers.IO) { packs?.appIcon(p.pkg)?.toBitmap(96, 96)?.asImageBitmap() }
            }
            PackLine(p.label, icon, chosen == p.pkg) { onPick(p.pkg) }
        }
        if (com.glimpsemedia.glimpselauncher.model.showFindPacks(installed.map { it.pkg })) {
            CardNote("No other icon packs are installed. Icon packs are free or paid apps from the Play Store.")
            BigButton("Find icon packs", {
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { context.startActivity(market) } catch (_: Exception) { try { context.startActivity(web) } catch (_: Exception) { } }
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PackLine(name: String, icon: androidx.compose.ui.graphics.ImageBitmap?, picked: Boolean, onClick: () -> Unit) {
    val menu = LocalMenu.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
            .semantics { selected = picked }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(40.dp)) else Column(Modifier.size(40.dp)) { }
        Text(name, color = if (picked) LocalAccent.current else menu.text, fontSize = 19.sp, modifier = Modifier.weight(1f))
        if (picked) Text("✓", color = LocalAccent.current, fontSize = 22.sp)
    }
}
