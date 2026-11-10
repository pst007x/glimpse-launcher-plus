package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** A big, easy-to-hit button for the set-up menus. */
@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, fontSize: TextUnit = if (LocalInCard.current) 17.sp else 20.sp) {
    // Inside a Settings card the buttons are a little lower, to match the smaller text there.
    val inCard = LocalInCard.current
    Text(
        text = text,
        color = if (primary) readableOn(LocalAccent.current) else LocalMenu.current.text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (inCard) 48.dp else 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) LocalAccent.current else LocalMenu.current.button)
            .clickable(onClick = onClick)
            .padding(horizontal = if (fontSize < 20.sp) 6.dp else 16.dp, vertical = if (inCard) 11.dp else 14.dp),
    )
}

/** A centred panel over the screen with a title and big buttons. */
@Composable
fun MenuDialog(title: String, onClose: () -> Unit, dismissible: Boolean = true, content: @Composable () -> Unit) = MenuText {
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(LocalMenu.current.dialog).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = LocalMenu.current.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
            content()
            if (dismissible) BigButton("Close", onClose)
        }
    }
}

/** Text field colours that follow the light or dark menus. */
@Composable
fun menuFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalMenu.current.text,
    unfocusedTextColor = LocalMenu.current.text,
    cursorColor = LocalAccent.current,
    focusedBorderColor = LocalAccent.current,
    unfocusedBorderColor = LocalMenu.current.dimText,
    focusedLabelColor = LocalMenu.current.text,
    unfocusedLabelColor = LocalMenu.current.dimText,
)
