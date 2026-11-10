package com.glimpsemedia.glimpselauncher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SEARCH
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SETTINGS
import com.glimpsemedia.glimpselauncher.model.SYSTEM_SETTINGS
import com.glimpsemedia.glimpselauncher.model.TextIcon
import com.glimpsemedia.glimpselauncher.model.TextLayout
import com.glimpsemedia.glimpselauncher.model.shaded
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The standard text colour on light themes. */
const val TEXT_DARK_INK = 0xFF101820.toInt()

/** Text mode's text colour: the clock colour (white on dark themes, near-black on light ones when standard), shaded. */
fun textInk(clockColor: Int, clockShade: Int, dark: Boolean): Int =
    shaded(if (clockColor != 0) clockColor else if (dark) 0xFFFFFFFF.toInt() else TEXT_DARK_INK, clockShade)

/** A chosen text colour (darker or lighter by [shade]), or [automatic] when none is chosen (0). */
fun nameInk(color: Int, shade: Int, automatic: Int): Int = if (color == 0) automatic else shaded(color, shade)

/** A text-mode folder icon: its own colour if it has one, else the text colour. */
fun folderInk(folderColor: Int, textInk: Int): Int = if (folderColor != 0) folderColor else textInk

/** Text mode's clock size, apart from the text: Small 75%, Medium as it always was, Large 135%. */
fun textClockScale(clockSize: Int): Float = when (clockSize) { 0 -> 0.75f; 2 -> 1.35f; else -> 1f }

/** Extra small, Small, Medium, Large and Extra large text, in sp; anything else reads as Medium. */

/** The mono Material icon for each folder icon choice. */
val TextIcon.vector: ImageVector
    get() = when (this) {
        TextIcon.Folder -> Icons.Filled.Folder
        TextIcon.Files -> Icons.AutoMirrored.Filled.InsertDriveFile
        TextIcon.Images -> Icons.Filled.Image
        TextIcon.Messages -> Icons.AutoMirrored.Filled.Chat
        TextIcon.Videos -> Icons.Filled.VideoLibrary
        TextIcon.Work -> Icons.Filled.Work
        TextIcon.Film -> Icons.Filled.Movie
        TextIcon.Shopping -> Icons.Filled.ShoppingCart
        TextIcon.Music -> Icons.Filled.MusicNote
        TextIcon.Travel -> Icons.Filled.Flight
        TextIcon.Search -> Icons.Filled.Search
        TextIcon.Health -> Icons.Filled.Favorite
        TextIcon.AI -> Icons.Filled.AutoAwesome
        TextIcon.Money -> Icons.Filled.AccountBalanceWallet
        TextIcon.Security -> Icons.Filled.Lock
        TextIcon.Settings -> Icons.Filled.Settings
        TextIcon.Games -> Icons.Filled.SportsEsports
        TextIcon.Social -> Icons.Filled.People
        TextIcon.News -> Icons.AutoMirrored.Filled.Article
        TextIcon.Tools -> Icons.Filled.Build
        TextIcon.Star -> Icons.Filled.Star
    }

/** What a text-mode entry is called: the app's name, or one of the built-in entries. */
fun textEntryLabel(key: String, state: LauncherState): String? = when (key) {
    GLIMPSE_SEARCH -> "Search"
    GLIMPSE_SETTINGS -> "Glimpse Settings"
    SYSTEM_SETTINGS -> "System settings"
    else -> state.apps[key]?.label
}

fun haloFor(ink: Color) = if (ink.luminance() > 0.45f) Color.Black else Color.White

/**
 * The text-only home screen: an optional clock at the top, then the app list on one side and the folder icons on
 * the other, both filling from the bottom up. Tapping a folder shows its apps in place of the list.
 */
@Composable
fun TextScreen(state: LauncherState, onOpen: (key: String) -> Unit, modifier: Modifier = Modifier) {
    val s = state.settings
    val t = state.textLayout ?: TextLayout()
    val size = textStepSp(s.textStep)
    // Text mode's own text colour (Settings > Home screen > Text mode), separate from the tile screens' clock colour.
    val ink = Color(textInk(s.textModeColor, s.textModeShade, s.darkTheme))
    val halo = haloFor(ink)
    // Folder icons follow the text colour, unless a folder has a colour of its own.
    val lineStyle = TextStyle(color = ink, fontSize = size.sp, lineHeight = (size * 1.5).sp, shadow = Shadow(halo, Offset(2f, 2f), 6f))
    val lineHeight = with(LocalDensity.current) { (size * 1.5).sp.toDp() }
    val open = t.folders.firstOrNull { it.id == state.textFolderOpen }
    // The open folder's entries (its name is drawn above them), or the main list.
    val entries: List<String> = (open?.entries ?: t.apps).filter { textEntryLabel(it, state) != null }
    val right = s.textListRight

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (s.textClock) TextClock(ink, halo, s.textClockAlign, textClockScale(s.textClockSize))
        Row(Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp)) {
            @Composable fun ListColumn(m: Modifier) {
                // An open folder's name sits just above its apps, with half a line of space, and never scrolls away.
                // Tapping the folder icon again closes it. The list is only as wide as its longest name, so swipes on
                // the empty side still reach the screen.
                Column(m.fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = if (right) Alignment.End else Alignment.Start) {
                    if (open != null) {
                        Text(
                            open.name, style = lineStyle.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = if (right) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(lineHeight / 2))
                    }
                    // Each list (the main one, each folder) has its own scroll, so each opens with its first item lowest.
                    key(open?.id) {
                        val list = rememberLazyListState()
                        LazyColumn(
                            Modifier.weight(1f, fill = false),
                            state = list,
                            reverseLayout = true,
                            horizontalAlignment = if (right) Alignment.End else Alignment.Start,
                            // Only a list taller than the space scrolls; otherwise up and down swipes reach the screen.
                            userScrollEnabled = list.canScrollForward || list.canScrollBackward,
                        ) {
                            items(entries, key = { "e-$it" }) { key ->
                                Text(
                                    textEntryLabel(key, state) ?: key, style = lineStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = if (right) TextAlign.End else TextAlign.Start,
                                    modifier = Modifier.clickable { onOpen(key) },
                                )
                            }
                        }
                    }
                }
            }
            @Composable fun FolderColumn(m: Modifier) {
                val list = rememberLazyListState()
                LazyColumn(
                    m.fillMaxHeight(),
                    state = list,
                    reverseLayout = true,
                    horizontalAlignment = if (right) Alignment.Start else Alignment.End,
                    userScrollEnabled = list.canScrollForward || list.canScrollBackward,
                ) {
                    items(t.folders, key = { "f-${it.id}" }) { f ->
                        Box(
                            Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                                .clickable { state.textFolderOpen = if (state.textFolderOpen == f.id) null else f.id }
                                .semantics { contentDescription = f.name },
                            contentAlignment = Alignment.Center,
                        ) {
                            // The halo: a soft copy behind, slightly offset, then the icon itself.
                            val iconInk = Color(folderInk(f.color, ink.toArgb()))
                            Icon(f.icon.vector, contentDescription = null, tint = haloFor(iconInk).copy(alpha = 0.55f), modifier = Modifier.size(lineHeight).offset(1.dp, 1.dp))
                            Icon(f.icon.vector, contentDescription = null, tint = iconInk, modifier = Modifier.size(lineHeight))
                            // The open folder's icon is underlined.
                            if (state.textFolderOpen == f.id) Box(
                                Modifier.align(Alignment.BottomCenter).width(lineHeight).height(2.dp).background(iconInk),
                            )
                        }
                    }
                }
            }
            // The folder icons need one icon's width; the names get the rest of the row.
            val folderWidth = maxOf(48.dp, lineHeight) + 12.dp
            if (right) {
                if (t.folders.isNotEmpty()) FolderColumn(Modifier.width(folderWidth))
                ListColumn(Modifier.weight(1f))
            } else {
                ListColumn(Modifier.weight(1f))
                if (t.folders.isNotEmpty()) FolderColumn(Modifier.width(folderWidth))
            }
        }
    }
}

/** A plain digital clock with the date under it, at the top: left, centred or right. */
@Composable
private fun TextClock(ink: Color, halo: Color, align: Int = 1, scale: Float = 1f) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay((60 - value.second) * 1000L)
        }
    }
    val locale = Locale.getDefault()
    val time = now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
    val shadow = Shadow(halo, Offset(2f, 2f), 6f)
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).semantics(mergeDescendants = true) { contentDescription = "$time, $date" },
        horizontalAlignment = when (align) { 0 -> Alignment.Start; 2 -> Alignment.End; else -> Alignment.CenterHorizontally },
    ) {
        Text(time, style = TextStyle(color = ink, fontSize = (64 * scale).sp, fontWeight = FontWeight.Bold, shadow = shadow))
        Text(date, style = TextStyle(color = ink, fontSize = (20 * scale).sp, fontWeight = FontWeight.Medium, shadow = shadow))
    }
}
