package com.glimpsemedia.glimpselauncher.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glimpsemedia.glimpselauncher.data.ScreenWallpaperStore
import com.glimpsemedia.glimpselauncher.model.PicturePlace
import com.glimpsemedia.glimpselauncher.model.ScreenPicture
import com.glimpsemedia.glimpselauncher.model.clampPlace
import com.glimpsemedia.glimpselauncher.model.cropRect
import com.glimpsemedia.glimpselauncher.model.movePlace
import com.glimpsemedia.glimpselauncher.model.nextScreenToAdd
import com.glimpsemedia.glimpselauncher.model.pictureForScreen
import com.glimpsemedia.glimpselauncher.model.screenRows
import com.glimpsemedia.glimpselauncher.model.slideOffset
import com.glimpsemedia.glimpselauncher.model.zoomPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** "Position the picture": [picture] is placed on a [sw]×[sh] screen, starting at [start]. */
class PositionRequest(
    val picture: Bitmap,
    val sw: Int,
    val sh: Int,
    val start: PicturePlace,
    val onUse: (PicturePlace) -> Unit,
    val onCancel: () -> Unit,
)

/**
 * Screen 2, 3, …'s own wallpapers: the list, choosing and placing pictures, and the pictures ready to draw. The
 * activity gives it the picture picker, the screen size and where "Position the picture" is shown.
 */
class ScreenWallpaperControl(
    val store: ScreenWallpaperStore,
    private val scope: CoroutineScope,
    private val resolver: ContentResolver,
    private val pick: (onPicked: (Uri?) -> Unit) -> Unit,
    private val screenSize: () -> Pair<Int, Int>,
    private val showPositioner: (PositionRequest?) -> Unit,
    private val canSave: () -> Boolean,
    private val note: (String) -> Unit,
) {
    var pictures by mutableStateOf(store.load())
        private set
    /** The screen whose picture is being chosen for a new row. */
    var adding by mutableStateOf<Int?>(null)
        private set
    private val drawn = mutableStateMapOf<String, ImageBitmap>()
    /** How many screens there were when the pictures were last loaded. */
    private var loadedFor = 1

    /** A shown picture ready to draw, or null while it loads (or if it could not be read). */
    fun bitmap(name: String): ImageBitmap? = drawn[name]

    /** Loads the pictures of the [screenCount] screens there are and forgets the rest (screens set up ahead wait). */
    suspend fun loadPictures(screenCount: Int) {
        loadedFor = screenCount
        val names = com.glimpsemedia.glimpselauncher.model.picturesToLoad(pictures, screenCount).map { it.shown }.toSet()
        drawn.keys.filter { it !in names }.forEach { drawn.remove(it) }
        for (n in names) if (n !in drawn) withContext(Dispatchers.IO) { store.loadShown(n) }?.let { drawn[n] = it.asImageBitmap() }
    }

    /** Change (or, for a new row, choose): pick a picture, place it, and use it for [screen]. */
    fun change(screen: Int) {
        if (pictures.none { it.screen == screen }) adding = screen
        pick { uri ->
            if (uri == null) { adding = null; return@pick }
            scope.launch {
                val (sw, sh) = screenSize()
                val src = withContext(Dispatchers.IO) { store.importSource(resolver, uri, minOf(3072, 2 * maxOf(sw, sh))) }
                val bitmap = src?.let { withContext(Dispatchers.IO) { store.loadSource(it) } }
                if (src == null || bitmap == null) { adding = null; note("That picture could not be used"); return@launch }
                showPositioner(PositionRequest(bitmap, sw, sh, PicturePlace(),
                    onUse = { place -> finish(screen, src, place) },
                    onCancel = { adding = null; tidy() }))
            }
        }
    }

    /** Position: place [screen]'s original picture again. */
    fun position(screen: Int) {
        val entry = pictures.firstOrNull { it.screen == screen } ?: return
        scope.launch {
            val (sw, sh) = screenSize()
            val bitmap = withContext(Dispatchers.IO) { store.loadSource(entry.source) }
            if (bitmap == null) { note("That picture could not be used"); return@launch }
            showPositioner(PositionRequest(bitmap, sw, sh, entry.place, onUse = { place -> finish(screen, entry.source, place) }, onCancel = {}))
        }
    }

    fun remove(screen: Int) {
        update(pictures.filter { it.screen != screen })
    }

    private fun finish(screen: Int, source: String, place: PicturePlace) {
        scope.launch {
            val (sw, sh) = screenSize()
            val shown = withContext(Dispatchers.IO) { store.render(source, sw, sh, place) }
            adding = null
            if (shown == null) { note("That picture could not be used"); tidy(); return@launch }
            update((pictures.filter { it.screen != screen } + ScreenPicture(screen, source, shown, place)).sortedBy { it.screen })
            loadPictures(loadedFor)
        }
    }

    private fun update(list: List<ScreenPicture>) {
        if (!canSave()) return
        pictures = list
        scope.launch(Dispatchers.IO) { synchronized(store) { store.save(list); store.removeUnusedIfSafe(list) } }
    }

    private fun tidy() {
        val list = pictures
        scope.launch(Dispatchers.IO) { synchronized(store) { store.removeUnusedIfSafe(list) } }
    }
}

val LocalScreenWallpapers = androidx.compose.runtime.staticCompositionLocalOf<ScreenWallpaperControl?> { null }

/**
 * Behind the home screens: each screen's own picture, sliding in and out with its screen over the phone's wallpaper.
 * [pagerIndexOfScreen] gives the pager page of screen number n (1 the home screen).
 */
@Composable
fun ScreenWallpaperLayer(on: Boolean, pagerState: PagerState, screenCount: Int, pagerIndexOfScreen: (Int) -> Int) {
    val control = LocalScreenWallpapers.current ?: return
    // Only the screens there are: a screen set up ahead gets its picture once it is made.
    val pictures = com.glimpsemedia.glimpselauncher.model.picturesToLoad(control.pictures, screenCount)
    LaunchedEffect(control.pictures, on, screenCount) { if (on) control.loadPictures(screenCount) }
    if (!on || pictures.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        for (p in pictures) {
            val shown = pictureForScreen(p.screen, on, pictures) ?: continue
            val bitmap = control.bitmap(shown.shown) ?: continue
            Image(
                bitmap, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val offset = slideOffset(pagerIndexOfScreen(p.screen), pagerState.currentPage, pagerState.currentPageOffsetFraction)
                    alpha = if (abs(offset) < 1f) 1f else 0f
                    translationX = offset * size.width
                },
            )
        }
    }
}

/** Settings: the screens with their own picture (Change, Position, Remove), then "Add another screen". */
@Composable
fun ScreenWallpaperRows(screenCount: Int) {
    val control = LocalScreenWallpapers.current ?: return
    val menu = LocalMenu.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (n in screenRows(control.pictures, control.adding)) {
            val has = control.pictures.any { it.screen == n }
            Text(com.glimpsemedia.glimpselauncher.model.screenRowLabel(n, screenCount), color = menu.text, fontSize = 18.sp)
            val labels = if (has) listOf("Change", "Position", "Remove") else listOf("Choosing a picture…")
            ButtonRow(labels.size, minButtonWidth(labels, 17f).dp) { i, m ->
                when {
                    !has -> BigButton(labels[0], { control.change(n) }, m)
                    i == 0 -> BigButton("Change", { control.change(n) }, m)
                    i == 1 -> BigButton("Position", { control.position(n) }, m)
                    else -> BigButton("Remove", { control.remove(n) }, m)
                }
            }
        }
        val next = nextScreenToAdd(control.pictures)
        if (control.adding == null && next != null) {
            BigButton("+ Add another screen", { control.change(next) }, Modifier.fillMaxWidth(), primary = control.pictures.isEmpty())
            if (next > screenCount) CardNote("A screen you have not made yet can have its picture now: it shows once the screen is made (drag an icon to the empty screen after the last one).")
        }
    }
}

/** "Position the picture": the picture fills the screen as it will look; drag to move, pinch to zoom. */
@Composable
fun PicturePositioner(request: PositionRequest, onClose: () -> Unit) {
    val image = remember(request) { request.picture.asImageBitmap() }
    var place by remember(request) { mutableStateOf(clampPlace(request.start, request.picture.width, request.picture.height, request.sw, request.sh)) }
    val pw = request.picture.width
    val ph = request.picture.height
    fun close(use: Boolean) { onClose(); if (use) request.onUse(place) else request.onCancel() }
    MenuText {
        Dialog(onDismissRequest = { close(false) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            // Over the top bar and the camera cut-out too, so the picture is seen on the whole screen.
            val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            androidx.compose.runtime.SideEffect {
                window?.let {
                    if (android.os.Build.VERSION.SDK_INT >= 30) it.attributes = it.attributes.apply {
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                    it.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                }
            }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                // Drawn and moved at this view's own shape (never stretched); the same place is cut at the screen's size.
                Canvas(
                    Modifier.fillMaxSize().pointerInput(request) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val vw = size.width.coerceAtLeast(1); val vh = size.height.coerceAtLeast(1)
                            var p = place
                            if (zoom != 1f) p = zoomPlace(p, zoom, pw, ph, vw, vh)
                            place = movePlace(p, pan.x, pan.y, pw, ph, vw, vh)
                        }
                    },
                ) {
                    val r = cropRect(pw, ph, size.width.toInt(), size.height.toInt(), place)
                    drawImage(image, srcOffset = IntOffset(r[0], r[1]), srcSize = IntSize(r[2], r[3]), dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
                Text(
                    "Drag to move, pinch to zoom", color = Color.White, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
                // The gesture bar is not counted inside a dialog: the buttons keep clear of it themselves.
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
                    ButtonRow(3, minButtonWidth(listOf("Use this"), 17f).dp) { i, m ->
                        when (i) {
                            0 -> BigButton("Cancel", { close(false) }, m, fontSize = 17.sp)
                            1 -> BigButton("Centre", { place = clampPlace(PicturePlace(place.zoom), pw, ph, request.sw, request.sh) }, m, fontSize = 17.sp)
                            else -> BigButton("Use this", { close(true) }, m, primary = true, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }
}
