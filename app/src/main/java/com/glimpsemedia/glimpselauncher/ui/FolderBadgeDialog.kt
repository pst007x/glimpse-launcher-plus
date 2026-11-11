package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.model.TextIcon

/**
 * Choosing a folder's badge: No badge first, then text mode's 21 mono icons with their names, three to a row (so
 * every name fits); the current one is ringed in the highlight colour. Touching one saves it and closes.
 */
@Composable
fun FolderBadgeDialog(
    name: String, current: TextIcon?, onPick: (TextIcon?) -> Unit, onClose: () -> Unit,
    /** The colour badge chosen now, and choosing one (Glimpse Colour drawings for the same subjects). */
    currentColour: TextIcon? = null, onPickColour: (TextIcon) -> Unit = {},
) {
    MenuDialog("Badge for $name", onClose) {
        BigButton("No badge", { onPick(null); onClose() }, Modifier.fillMaxWidth(), primary = current == null && currentColour == null)
        val menu = LocalMenu.current
        val accent = LocalAccent.current
        for (row in TextIcon.entries.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (icon in row) {
                    val picked = icon == current
                    Column(
                        Modifier.weight(1f).heightIn(min = 72.dp).clip(RoundedCornerShape(12.dp))
                            .border(if (picked) 3.dp else 1.dp, if (picked) accent else menu.dimText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .semantics { selected = picked }
                            .clickable(role = Role.Button, onClickLabel = icon.name) { onPick(icon); onClose() }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(icon.vector, contentDescription = null, tint = menu.text, modifier = Modifier.size(30.dp))
                        Text(icon.name, color = menu.text, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
        CardTitle("Colour")
        val context = androidx.compose.ui.platform.LocalContext.current
        for (row in TextIcon.entries.filter { com.glimpsemedia.glimpselauncher.model.GC_BADGES.containsKey(it.name) }.chunked(3)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (icon in row) {
                    val picked = icon == currentColour
                    @Suppress("DiscouragedApi")
                    val id = context.resources.getIdentifier("gc_" + com.glimpsemedia.glimpselauncher.model.GC_BADGES.getValue(icon.name), "drawable", context.packageName)
                    Column(
                        Modifier.weight(1f).heightIn(min = 72.dp).clip(RoundedCornerShape(12.dp))
                            .border(if (picked) 3.dp else 1.dp, if (picked) accent else menu.dimText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .semantics { selected = picked }
                            .clickable(role = Role.Button, onClickLabel = "Colour " + icon.name) { onPickColour(icon); onClose() }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (id != 0) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(id), contentDescription = null, modifier = Modifier.size(34.dp))
                        Text(icon.name, color = menu.text, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
