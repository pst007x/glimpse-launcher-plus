package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.glimpsemedia.glimpselauncher.model.LINE_MARK
import com.glimpsemedia.glimpselauncher.model.NOTE_LINE_COLORS
import com.glimpsemedia.glimpselauncher.model.NoteLine
import com.glimpsemedia.glimpselauncher.model.editLine
import com.glimpsemedia.glimpselauncher.model.Note
import com.glimpsemedia.glimpselauncher.model.added
import com.glimpsemedia.glimpselauncher.model.changed
import com.glimpsemedia.glimpselauncher.model.deleted
import com.glimpsemedia.glimpselauncher.model.displayTitle
import com.glimpsemedia.glimpselauncher.model.movedDown
import com.glimpsemedia.glimpselauncher.model.movedUp
import com.glimpsemedia.glimpselauncher.model.summary

/** The notes screen is the pager's first page; the home screens follow it. */
const val NOTES_PAGE = 0

/** The home screen shown at a pager page (-1 is the notes screen). */
fun homePageOf(pagerIndex: Int): Int = pagerIndex - 1

/** The pager page that shows a home screen. */
fun pagerIndexOf(homePage: Int): Int = homePage + 1

/** How many home pages follow the notes: text mode has the text screen then the widgets screen; tiles have their pages (and the empty one while unlocked). */
fun homePageCount(textMode: Boolean, tilePages: Int, ghostPage: Boolean): Int = if (textMode) 2 else tilePages + if (ghostPage) 1 else 0

/** What the notes screen asks the activity to do with pictures and sharing. */
sealed interface PictureRequest {
    data class TakePhoto(val noteId: String) : PictureRequest
    data class FromGallery(val noteId: String) : PictureRequest
    data class Share(val noteId: String) : PictureRequest
    /** One picture on its own, from the full-size view. */
    data class SharePicture(val name: String) : PictureRequest
}

/** The notes: newest first under a big New note button. Each card has its own colour and folds to a summary. */
@Composable
fun NotesScreen(
    state: LauncherState,
    onPictureAction: (PictureRequest) -> Unit,
    onDeletePictures: (List<String>) -> Unit,
    pictureFile: (String) -> java.io.File,
    open: Boolean,
    /** Goes up whenever the notes leave the screen or Home is pressed: every menu, viewer and the keyboard close. */
    dismiss: Int = 0,
    modifier: Modifier = Modifier,
) {
    val menu = LocalMenu.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    // Locked: nothing of the notes shows, and the keyboard goes.
    LaunchedEffect(open) { if (!open) focusManager.clearFocus(force = true) }
    LaunchedEffect(dismiss) { if (dismiss > 0) focusManager.clearFocus(force = true) }
    if (!open) {
        Column(modifier.fillMaxSize().background(menu.background).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Notes are locked", color = menu.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Hold the padlock for three seconds.", color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }
    var menuFor by remember(dismiss) { mutableStateOf<String?>(null) }
    var colourFor by remember(dismiss) { mutableStateOf<String?>(null) }
    var deleteFor by remember(dismiss) { mutableStateOf<String?>(null) }
    var focusTitleOf by remember(dismiss) { mutableStateOf<String?>(null) }
    val notes = state.notes
    LazyColumn(
        modifier.fillMaxSize().background(menu.background).imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "head") {
            Text("Notes", color = menu.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (state.notesUnlocked && !state.unlocked) item(key = "lock") {
            // Only the notes are open: a way to lock them again without waiting.
            BigButton("Lock the notes", { state.notesUnlocked = false })
        }
        item(key = "new") {
            BigButton("New note", {
                val note = Note(id = "n" + System.currentTimeMillis())
                state.changeNotes(state.notes.added(note))
                focusTitleOf = note.id
            }, primary = true)
        }
        items(notes.list, key = { it.id }) { note ->
            NoteCard(
                note = note,
                focusTitle = focusTitleOf == note.id,
                onChange = { f -> state.changeNotes(state.notes.changed(note.id, f)) },
                onMenu = { menuFor = note.id },
                onTitleFocused = { focusTitleOf = null },
                pictureFile = pictureFile,
                onPictureAction = onPictureAction,
                onDeletePictures = onDeletePictures,
                dismiss = dismiss,
            )
        }
    }

    val list = notes.list
    menuFor?.let { id ->
        val i = list.indexOfFirst { it.id == id }
        if (i < 0) { menuFor = null; return@let }
        MenuDialog(title = list[i].displayTitle(), onClose = { menuFor = null }) {
            BigButton("Share…", { onPictureAction(PictureRequest.Share(id)); menuFor = null })
            BigButton("Change the colour", { colourFor = id; menuFor = null })
            if (i > 0) BigButton("Move up", { state.changeNotes(state.notes.movedUp(id)); menuFor = null })
            if (i < list.size - 1) BigButton("Move down", { state.changeNotes(state.notes.movedDown(id)); menuFor = null })
            BigButton("Delete the note", { deleteFor = id; menuFor = null })
        }
    }
    deleteFor?.let { id ->
        val note = list.firstOrNull { it.id == id }
        if (note == null) { deleteFor = null; return@let }
        MenuDialog(title = "Delete this note?", onClose = { deleteFor = null }) {
            Text(note.displayTitle(), color = menu.text, fontSize = 20.sp)
            BigButton("Delete the note", {
                state.changeNotes(state.notes.deleted(id))
                onDeletePictures(note.pictures)
                deleteFor = null
            }, primary = true)
            BigButton("Keep it", { deleteFor = null })
        }
    }
    colourFor?.let { id ->
        val note = list.firstOrNull { it.id == id }
        if (note == null) { colourFor = null; return@let }
        Dialog(onDismissRequest = { colourFor = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ColourPicker(
                title = "Colour of the note",
                current = note.color,
                standardName = "None",
                onPick = { argb -> state.changeNotes(state.notes.changed(id) { it.copy(color = argb) }) },
                onClose = { colourFor = null },
            )
        }
    }
}

/** One note: a title row (title, fold arrow, menu), then its lines when open, or its summary when folded. */
@Composable
private fun NoteCard(
    note: Note,
    focusTitle: Boolean,
    onChange: ((Note) -> Note) -> Unit,
    onMenu: () -> Unit,
    onTitleFocused: () -> Unit,
    pictureFile: (String) -> java.io.File,
    onPictureAction: (PictureRequest) -> Unit,
    onDeletePictures: (List<String>) -> Unit,
    dismiss: Int,
) {
    val menu = LocalMenu.current
    val back = if (note.color == 0) menu.surface else Color(note.color).copy(alpha = 1f)
    val ink = if (note.color == 0) menu.text else readableOn(back)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(back).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.folded) {
                // Folded: touching the title opens the note again.
                Column(Modifier.weight(1f).clickable(onClickLabel = "Open the note") { onChange { it.copy(folded = false) } }.padding(vertical = 6.dp)) {
                    Text(note.displayTitle(), color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(note.summary(), color = ink.copy(alpha = 0.75f), fontSize = 17.sp)
                }
            } else {
                val focus = remember { FocusRequester() }
                Box(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    if (note.title.isEmpty()) Text("Title", color = ink.copy(alpha = 0.5f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    BasicTextField(
                        value = note.title,
                        onValueChange = { t -> onChange { it.copy(title = t.replace("\n", " ").take(200)) } },
                        singleLine = true,
                        textStyle = TextStyle(color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(ink),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (focusTitle) LaunchedEffect(Unit) { focus.requestFocus(); onTitleFocused() }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = if (note.folded) "Open the note" else "Fold the note") {
                    onChange { it.copy(folded = !it.folded) }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(if (note.folded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (note.folded) "Open the note" else "Fold the note", tint = ink) }
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = "Menu") { onMenu() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for ${note.displayTitle()}", tint = ink) }
        }
        if (!note.folded) {
            NoteBody(note, ink, onChange, dismiss)
            NotePictures(note, ink, onChange, pictureFile, onPictureAction, onDeletePictures, dismiss)
        }
    }
}

/** The names of the line colours, in the order of [NOTE_LINE_COLORS]. */
private val LINE_COLOUR_NAMES = listOf("Yellow", "Orange", "Red", "Pink", "Purple", "Blue", "Green", "Grey")

/**
 * The open note's lines: a tick box, the text, and a small menu for the colour band. Each field starts with the
 * invisible [LINE_MARK]; Enter splits the line, deleting the mark joins the line to the one above.
 */
@Composable
private fun NoteBody(note: Note, ink: Color, onChange: ((Note) -> Note) -> Unit, dismiss: Int) {
    val lines = note.lines.ifEmpty { listOf(NoteLine()) }
    // One focus handle per line; the list only grows, so a handle stays with its row.
    val focus = remember(note.id) { ArrayList<FocusRequester>() }
    while (focus.size < lines.size) focus.add(FocusRequester())
    // After a split or a join: the line to put the cursor in, and where in it.
    var cursorAt by remember(note.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var colourFor by remember(note.id, dismiss) { mutableStateOf<Int?>(null) }
    lines.forEachIndexed { i, line ->
        val shown = LINE_MARK + line.text
        var field by remember(note.id, i) { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
        val wanted = cursorAt?.takeIf { it.first == i }?.second
        if (field.text != shown) field = TextFieldValue(shown, TextRange((wanted ?: shown.length).coerceIn(1, shown.length)))
        else if (wanted != null && field.selection != TextRange(wanted)) field = field.copy(selection = TextRange(wanted.coerceIn(1, shown.length)))
        val band = if (line.color != 0) Color(line.color).copy(alpha = 1f) else Color.Transparent
        val lineInk = if (line.color != 0) readableOn(band) else ink
        Row(
            Modifier.fillMaxWidth().alpha(if (line.ticked) 0.55f else 1f).clip(RoundedCornerShape(8.dp)).background(band),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clickable(onClickLabel = if (line.ticked) "Untick" else "Tick") {
                    onChange { n -> n.copy(lines = n.lines.ifEmpty { listOf(NoteLine()) }.mapIndexed { j, l -> if (j == i) l.copy(ticked = !l.ticked) else l }) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (line.ticked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = if (line.ticked) "Ticked" else "Not ticked", tint = lineInk, modifier = Modifier.size(28.dp),
                )
            }
            BasicTextField(
                value = field,
                onValueChange = { v ->
                    // The cursor never goes in front of the invisible mark.
                    val safe = if (v.text.startsWith(LINE_MARK) && v.selection.min < 1) v.copy(selection = TextRange(maxOf(1, v.selection.start), maxOf(1, v.selection.end))) else v
                    if (safe.text == field.text) { field = safe; return@BasicTextField }
                    val edit = note.copy(lines = lines).editLine(i, safe.text)
                    when {
                        edit.note.lines.size < lines.size -> {
                            // Joined to the line above: the cursor goes where the two met.
                            cursorAt = edit.focus to (1 + lines[edit.focus].text.length)
                        }
                        edit.note.lines.size > lines.size -> cursorAt = edit.focus to 1
                        else -> { field = safe; cursorAt = null }
                    }
                    onChange { it.copy(lines = edit.note.lines) }
                },
                textStyle = TextStyle(color = lineInk, fontSize = 20.sp),
                cursorBrush = SolidColor(lineInk),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).focusRequester(focus[i]),
            )
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "Line colour") { colourFor = i },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for this line", tint = lineInk.copy(alpha = 0.7f), modifier = Modifier.size(22.dp)) }
        }
    }
    // The cursor request is used once: after that the person moves the cursor freely.
    cursorAt?.let { (i, _) -> LaunchedEffect(cursorAt) { focus.getOrNull(i)?.requestFocus(); cursorAt = null } }
    colourFor?.let { i ->
        MenuDialog(title = "Line colour", onClose = { colourFor = null }) {
            fun pick(argb: Int) {
                onChange { n -> n.copy(lines = n.lines.ifEmpty { listOf(NoteLine()) }.mapIndexed { j, l -> if (j == i) l.copy(color = argb) else l }) }
                colourFor = null
            }
            for (row in NOTE_LINE_COLORS.indices.chunked(4)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (k in row) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color(NOTE_LINE_COLORS[k]))
                                .clickable(onClickLabel = "Use ${LINE_COLOUR_NAMES[k]}") { pick(NOTE_LINE_COLORS[k]) }
                                .semantics { contentDescription = LINE_COLOUR_NAMES[k] },
                        )
                    }
                }
            }
            BigButton("None", { pick(0) })
        }
    }
}

/** The note's pictures as thumbnails, and Add a picture (camera or gallery). Touching one shows it full size. */
@Composable
private fun NotePictures(
    note: Note,
    ink: Color,
    onChange: ((Note) -> Note) -> Unit,
    pictureFile: (String) -> java.io.File,
    onPictureAction: (PictureRequest) -> Unit,
    onDeletePictures: (List<String>) -> Unit,
    dismiss: Int,
) {
    var adding by remember(note.id, dismiss) { mutableStateOf(false) }
    var viewing by remember(note.id, dismiss) { mutableStateOf<String?>(null) }
    if (note.pictures.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            note.pictures.forEachIndexed { k, name ->
                val bitmap = rememberPicture(pictureFile(name), 256)
                Box(
                    Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(ink.copy(alpha = 0.15f))
                        .clickable(onClickLabel = "Show the picture") { viewing = name }
                        .semantics { contentDescription = "Picture ${k + 1} of ${note.pictures.size}" },
                ) {
                    bitmap?.let { androidx.compose.foundation.Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
            }
        }
    }
    Text(
        "Add a picture", color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(ink.copy(alpha = 0.12f))
            .clickable { adding = true }.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    if (adding) MenuDialog(title = "Add a picture", onClose = { adding = false }) {
        BigButton("Take a photo", { onPictureAction(PictureRequest.TakePhoto(note.id)); adding = false }, primary = true)
        BigButton("Choose from the gallery", { onPictureAction(PictureRequest.FromGallery(note.id)); adding = false })
    }
    viewing?.let { name ->
        var asking by remember(name) { mutableStateOf(false) }
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                val big = rememberPicture(pictureFile(name), 2048)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    big?.let { androidx.compose.foundation.Image(it, contentDescription = "The picture", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                }
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("Delete", { asking = true }, Modifier.weight(1f))
                    BigButton("Share", { onPictureAction(PictureRequest.SharePicture(name)) }, Modifier.weight(1f))
                    BigButton("Close", { viewing = null }, Modifier.weight(1f), primary = true)
                }
            }
        }
        if (asking) MenuDialog(title = "Delete this picture?", onClose = { asking = false }) {
            BigButton("Delete the picture", {
                onChange { it.copy(pictures = it.pictures - name) }
                onDeletePictures(listOf(name))
                asking = false
                viewing = null
            }, primary = true)
            BigButton("Keep it", { asking = false })
        }
    }
}

/** A picture file, decoded off the main thread at about [maxSide] pixels and turned the way the camera held it. */
@Composable
private fun rememberPicture(file: java.io.File, maxSide: Int): androidx.compose.ui.graphics.ImageBitmap? {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file.path, maxSide) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { decodePicture(file, maxSide) }
    }
    return bitmap
}

private fun decodePicture(file: java.io.File, maxSide: Int): androidx.compose.ui.graphics.ImageBitmap? = try {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val raw = android.graphics.BitmapFactory.decodeFile(file.path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
    val turn = when (android.media.ExifInterface(file.path).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val shown = if (raw != null && turn != 0f) {
        android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, android.graphics.Matrix().apply { postRotate(turn) }, true)
    } else raw
    shown?.asImageBitmap()
} catch (_: Exception) {
    null
}
