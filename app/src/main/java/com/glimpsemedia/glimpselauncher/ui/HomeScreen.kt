package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.withContext
import com.glimpsemedia.glimpselauncher.model.snapWidgetSize
import com.glimpsemedia.glimpselauncher.model.resizeWidget
import com.glimpsemedia.glimpselauncher.model.removeWidget
import com.glimpsemedia.glimpselauncher.model.addWidget
import com.glimpsemedia.glimpselauncher.model.moveWidget
import com.glimpsemedia.glimpselauncher.model.moveWidgetFitting
import com.glimpsemedia.glimpselauncher.model.widgetSizes
import com.glimpsemedia.glimpselauncher.model.PAGE_ROWS
import com.glimpsemedia.glimpselauncher.data.WidgetSupport
import com.glimpsemedia.glimpselauncher.data.WidgetChoice
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import com.glimpsemedia.glimpselauncher.data.withTheme
import com.glimpsemedia.glimpselauncher.data.isTheme
import com.glimpsemedia.glimpselauncher.data.ThemePreset
import com.glimpsemedia.glimpselauncher.data.THEME_PRESETS
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SEARCH
import com.glimpsemedia.glimpselauncher.model.GLIMPSE_SETTINGS
import com.glimpsemedia.glimpselauncher.model.SYSTEM_SETTINGS
import com.glimpsemedia.glimpselauncher.model.SettingsTap
import com.glimpsemedia.glimpselauncher.model.glimpseSettingsTap
import com.glimpsemedia.glimpselauncher.model.textWidgetHeightFor
import com.glimpsemedia.glimpselauncher.model.widgetMoved
import com.glimpsemedia.glimpselauncher.model.widgetResized
import com.glimpsemedia.glimpselauncher.model.withWidget
import com.glimpsemedia.glimpselauncher.model.withoutWidget
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.model.Item
import com.glimpsemedia.glimpselauncher.model.add
import com.glimpsemedia.glimpselauncher.model.colorKey
import com.glimpsemedia.glimpselauncher.model.wallpaperPlan
import com.glimpsemedia.glimpselauncher.data.drawThemeWallpaper
import com.glimpsemedia.glimpselauncher.data.currentThemeColours
import com.glimpsemedia.glimpselauncher.data.look
import androidx.compose.ui.graphics.toArgb
import com.glimpsemedia.glimpselauncher.model.SHADE_STEP
import com.glimpsemedia.glimpselauncher.model.SHADE_MAX
import com.glimpsemedia.glimpselauncher.model.SHADE_MIN
import com.glimpsemedia.glimpselauncher.model.fileSources
import com.glimpsemedia.glimpselauncher.model.shaded
import com.glimpsemedia.glimpselauncher.model.dockHoldKey
import com.glimpsemedia.glimpselauncher.model.DockAllHold
import com.glimpsemedia.glimpselauncher.model.dockAllAppsHold
import com.glimpsemedia.glimpselauncher.model.HoldAction
import com.glimpsemedia.glimpselauncher.model.holdKey
import com.glimpsemedia.glimpselauncher.model.dropOnto
import com.glimpsemedia.glimpselauncher.model.renameFolder
import com.glimpsemedia.glimpselauncher.model.moveInFolder
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.data.CLOCK_SMALL
import com.glimpsemedia.glimpselauncher.data.CLOCK_MEDIUM
import com.glimpsemedia.glimpselauncher.data.CLOCK_LARGE
import com.glimpsemedia.glimpselauncher.model.takeOutOfFolder
import com.glimpsemedia.glimpselauncher.model.Page
import com.glimpsemedia.glimpselauncher.model.addPage
import com.glimpsemedia.glimpselauncher.data.EMERGENCY_COUNTDOWN_CHOICES
import com.glimpsemedia.glimpselauncher.model.addToDock
import com.glimpsemedia.glimpselauncher.model.FOLDER_MAX
import com.glimpsemedia.glimpselauncher.model.addToFolder
import com.glimpsemedia.glimpselauncher.data.barShown
import com.glimpsemedia.glimpselauncher.model.dockSides
import com.glimpsemedia.glimpselauncher.model.dockHasRoom
import com.glimpsemedia.glimpselauncher.model.canMoveInDock
import com.glimpsemedia.glimpselauncher.model.dockMoved
import com.glimpsemedia.glimpselauncher.model.dockHidden
import com.glimpsemedia.glimpselauncher.model.addToPage
import com.glimpsemedia.glimpselauncher.model.hasAllAppsIcon
import com.glimpsemedia.glimpselauncher.model.hasSearchIcon
import com.glimpsemedia.glimpselauncher.model.hasEmergencyIcon
import com.glimpsemedia.glimpselauncher.model.Contact
import com.glimpsemedia.glimpselauncher.model.moveItem
import com.glimpsemedia.glimpselauncher.data.Badges
import com.glimpsemedia.glimpselauncher.model.placeItem
import com.glimpsemedia.glimpselauncher.model.addToPageAt
import com.glimpsemedia.glimpselauncher.model.addWidgetAt
import com.glimpsemedia.glimpselauncher.model.removeFromDock
import com.glimpsemedia.glimpselauncher.model.removeFromPage
import com.glimpsemedia.glimpselauncher.model.removePageIfEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Where "add apps" puts what is picked. */
private sealed interface PickerTarget {
    data class OnPage(val page: Int, val slot: Int? = null) : PickerTarget
    data object OnBar : PickerTarget
}

/** The item whose menu is open: where it is on the pages. */
private data class ItemRef(val page: Int, val index: Int)

private class DragState {
    var item by mutableStateOf<Item?>(null)
    var fromPage = 0
    var fromIndex = 0
    var pointer by mutableStateOf(Offset.Zero)
    var moved by mutableFloatStateOf(0f)
    /** For a widget: which of its squares was held (so it lands with that square under the finger). */
    var grabCol = 0
    var grabRow = 0
    val active get() = item != null
}

private const val HOLD_MENU_MS = 5000L

/** Squares are remembered by page × this + slot: more than the 16 squares of the largest grid. */
// Each screen's squares are kept this far apart in the bounds list, so screens never overlap.
private val BOUNDS_STRIDE = com.glimpsemedia.glimpselauncher.model.MAX_SLOTS

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherState,
    onLaunch: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onNotifications: () -> Unit,
    onConfirmPhoneLock: (onOk: () -> Unit) -> Unit,
    onWallpaper: () -> Unit,
    onMakeHome: () -> Unit,
    onEmergencyTap: () -> Unit,
    onEmergencySetup: () -> Unit,
    onEmergencyCallNow: () -> Unit,
    onEmergencyCancel: () -> Unit,
    onHoldCallNow: () -> Unit,
    onPictureAction: (PictureRequest) -> Unit,
    onDeletePictures: (List<String>) -> Unit,
    notePicture: (String) -> java.io.File,
    /** Draws and sets a theme wallpaper from four colours (the screen question comes first). */
    onThemeWallpaper: (c1: Int, c2: Int, c3: Int, c4: Int, dark: Boolean) -> Unit,
    /** Saving, applying, renaming, updating and deleting the person's own themes. */
    onSavedTheme: (SavedThemeAction) -> Unit,
    onHoldCallCancel: () -> Unit,
    onEmail: () -> Unit,
    onWebsite: () -> Unit,
    onPrivacy: () -> Unit,
    onCoffee: () -> Unit,
    widgets: WidgetSupport,
    versionName: String,
    /** Runs a tile's hold action (only ever called while locked). */
    onHoldAction: (HoldAction) -> Unit,
    /** Opens the phone's own Settings (text mode's "System settings"). */
    onSystemSettings: () -> Unit = {},
    /** Flips text mode (a tap on a placed Text mode switch while unlocked). */
    onToggleTextMode: () -> Unit = {},
    /** Settings > Your setup > Backup and restore: save a backup file, pick one to restore, and the backup waiting for a yes or no. */
    onBackup: () -> Unit = {},
    onRestorePick: () -> Unit = {},
    restoreOffer: com.glimpsemedia.glimpselauncher.data.BackupContents? = null,
    onRestoreAnswer: (Boolean) -> Unit = {},
    /** A restore is running: a cover that cannot be closed, until the launcher restarts. */
    restoring: Boolean = false,
    /** Settings > Help and support > Report bugs. */
    onSuggest: () -> Unit = {},
    onEmailBugLog: () -> Unit = {},
    onClearBugLog: () -> Unit = {},
    /** Search's saved lists, folder index and openers; null in previews. */
    search: com.glimpsemedia.glimpselauncher.data.SearchSupport? = null,
) {
    val context = LocalContext.current
    val layout = state.layout
    val apps = state.apps
    val unlocked = state.unlocked
    val custom = state.settings.customLayout

    var showAllApps by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    // Search: what it found in the picked folders, a folder shown inside it, and where Settings or Help open from it.
    var searchFiles by remember { mutableStateOf<com.glimpsemedia.glimpselauncher.model.WalkResult?>(null) }
    var browsing by remember { mutableStateOf<Browsing?>(null) }
    var settingsFlash by remember { mutableStateOf<String?>(null) }
    var helpStart by remember { mutableStateOf<String?>(null) }
    var settingsHitAfterUnlock by remember { mutableStateOf<SettingsHit?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    // The Settings pages open on the way in: "g:<group>" and "s:<page>"; empty is the first page. Kept across a turn of the phone.
    var settingsStack by androidx.compose.runtime.saveable.rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.listSaver<List<String>, String>(save = { it }, restore = { it }),
    ) { mutableStateOf(emptyList<String>()) }
    var pinStep by remember { mutableStateOf<PinStep?>(null) }
    // A PIN pad closed without unlocking (Cancel) drops the Settings row Search was waiting to open.
    LaunchedEffect(pinStep) { if (pinStep == null) settingsHitAfterUnlock = null }
    val pinTries = remember { com.glimpsemedia.glimpselauncher.data.PinTries(System::currentTimeMillis) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var addChoiceFor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var widgetSlot by remember { mutableStateOf<Int?>(null) }
    var widgetSizeFor by remember { mutableStateOf<ItemRef?>(null) }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    var itemMenu by remember { mutableStateOf<ItemRef?>(null) }
    var emptyMenuPage by remember { mutableStateOf<Int?>(null) }
    var holding by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<ItemRef?>(null) }
    var tileColorFor by remember { mutableStateOf<ItemRef?>(null) }
    var iconColorFor by remember { mutableStateOf<ItemRef?>(null) }
    var folderColorFor by remember { mutableStateOf<ItemRef?>(null) }
    var folderBadgeFor by remember { mutableStateOf<ItemRef?>(null) }
    var iconFor by remember { mutableStateOf<IconRequest?>(null) }
    var textColorFor by remember { mutableStateOf<ItemRef?>(null) }
    // The folder whose "Add apps" list is open.
    var folderAddFor by remember { mutableStateOf<ItemRef?>(null) }
    // The All apps filter: null shows every app, else the name of a tag. It stays while the launcher runs.
    var appFilter by remember { mutableStateOf<String?>(null) }
    var renameFolderFor by remember { mutableStateOf<ItemRef?>(null) }
    var dwellSlot by remember { mutableStateOf<Int?>(null) }
    // The hold action being chosen: its key and the tile's name.
    var holdMenuFor by remember { mutableStateOf<Pair<String, String>?>(null) }
    // The bottom-bar icon whose menu is open: its app key, or "" for the All apps button.
    var barMenuFor by remember { mutableStateOf<String?>(null) }
    // Text mode's "Glimpse Settings" asked for the PIN: the right PIN opens Settings straight after unlocking.
    var openSettingsAfterUnlock by remember { mutableStateOf(false) }
    // Text mode's widgets screen: the widget whose menu is open, and the one whose size is being chosen.
    var textWidgetMenu by remember { mutableStateOf<Int?>(null) }
    var textWidgetSizing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pinStep) { if (pinStep == null) openSettingsAfterUnlock = false }
    val textMode = state.settings.textMode

    // While unlocked, an empty screen waits after the last one: drag an icon onto it to start a new screen.
    // Text mode has a single home page and nothing to drag.
    val ghostPage = !textMode && unlocked && !layout.pages.last().isEmpty
    // Text mode: the text screen, then the widgets screen.
    val pageCount = homePageCount(textMode, layout.pages.size, ghostPage)
    // Page 0 of the pager is the notes screen; the home screens follow (see homePageOf / pagerIndexOf).
    val pagerState = rememberPagerState(initialPage = pagerIndexOf(0)) { pageCount + 1 }
    val currentHome = homePageOf(pagerState.currentPage)
    val notesOpenNow = notesOpen(state.settings.notesNeedPin, unlocked, state.notesUnlocked)
    // Leaving the notes (a swipe, Home) closes whatever they had open, keyboard included.
    var notesDismiss by remember { mutableStateOf(0) }
    val onNotesPage = pagerState.currentPage == NOTES_PAGE
    LaunchedEffect(onNotesPage) { if (!onNotesPage) notesDismiss++ else state.textFolderOpen = null }
    // Leaving the text screen (to the notes or the widgets screen) closes an open folder.
    LaunchedEffect(currentHome) { if (currentHome != 0) state.textFolderOpen = null }
    val drag = remember { DragState() }
    val bounds = remember { HashMap<Int, Rect>() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val edge = with(LocalDensity.current) { 56.dp.toPx() }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    // A tap on a text-mode line: an app opens, locked or not; Glimpse Settings never gets round the padlock.
    fun onTextEntry(key: String) {
        state.touched()
        when (key) {
            SYSTEM_SETTINGS -> onSystemSettings()
            GLIMPSE_SEARCH -> showSearch = true
            GLIMPSE_SETTINGS -> when (glimpseSettingsTap(unlocked, state.settings.pinHash != null)) {
                SettingsTap.Open -> { settingsStack = emptyList(); showSettings = true }
                SettingsTap.AskPin -> { settingsHitAfterUnlock = null; pinStep = PinStep.Unlock; openSettingsAfterUnlock = true }
                SettingsTap.HoldPadlock -> toast("Hold the padlock to unlock first")
            }
            else -> state.apps[key]?.let(onLaunch)
        }
    }
    fun slotAt(page: Int, p: Offset): Int? = (0 until state.layout.slotsPerPage).firstOrNull { bounds[page * BOUNDS_STRIDE + it]?.contains(p) == true }

    // The Home button always brings the first screen back, with nothing left open on top of it.
    // A hold action asked for the notes screen.
    LaunchedEffect(state.openNotes) {
        if (state.openNotes > 0) pagerState.animateScrollToPage(NOTES_PAGE, animationSpec = pageTurnSpec())
    }
    LaunchedEffect(state.homePresses) {
        if (state.homePresses > 0) {
            notesDismiss++
            state.textFolderOpen = null
            textWidgetMenu = null; textWidgetSizing = null
            showAllApps = false; showSearch = false; browsing = null; showSettings = false; showHelp = false; showAbout = false; picker = null; itemMenu = null
            emptyMenuPage = null; openFolder = null; renameFolderFor = null; tileColorFor = null; iconColorFor = null; folderColorFor = null; folderBadgeFor = null; iconFor = null; textColorFor = null; folderAddFor = null
            pagerState.animateScrollToPage(pagerIndexOf(0), animationSpec = pageTurnSpec())
        }
    }

    // Locking again after a while without touching, and whenever the padlock is tapped.
    LaunchedEffect(unlocked, state.notesUnlocked) {
        while (unlocked || state.notesUnlocked) {
            delay(15_000)
            val minutes = state.settings.autoLockMinutes
            if (minutes > 0 && System.currentTimeMillis() - state.lastTouch >= minutes * 60_000L) { state.unlocked = false; state.notesUnlocked = false }
        }
    }
    // Locking the screens locks the notes too.
    LaunchedEffect(unlocked) { if (!unlocked) { state.notesUnlocked = false; state.textFolderOpen = null } }
    LaunchedEffect(unlocked) { if (!unlocked) { pinStep = pinStepAfterLock(pinStep); showSettings = false; picker = null; itemMenu = null; emptyMenuPage = null; renameFolderFor = null; tileColorFor = null; iconColorFor = null; folderColorFor = null; folderBadgeFor = null; iconFor = null; textColorFor = null; folderAddFor = null; holdMenuFor = null; barMenuFor = null; textWidgetMenu = null; textWidgetSizing = null } }

    // Dragging to the left or right edge for a moment turns the page.
    val edgeDirection = when {
        !drag.active -> 0
        drag.pointer.x < edge -> -1
        drag.pointer.x > screenWidth - edge -> 1
        else -> 0
    }
    LaunchedEffect(edgeDirection) {
        if (edgeDirection != 0) {
            delay(600)
            while (true) {
                // A dragged tile never turns onto the notes screen.
                val next = (pagerState.currentPage + edgeDirection).coerceIn(pagerIndexOf(0), pagerIndexOf(pageCount - 1))
                if (next == pagerState.currentPage) break
                pagerState.animateScrollToPage(next, animationSpec = pageTurnSpec())
                delay(900)
            }
        }
    }

    // Holding a dragged app over another for a moment offers to make a folder; letting go sooner just moves it.
    val hoverNow = if (drag.active) slotAt(currentHome, drag.pointer) else null
    LaunchedEffect(hoverNow, drag.active) {
        dwellSlot = null
        val target = hoverNow?.let { state.layout.pages.getOrNull(currentHome)?.items?.getOrNull(it) }
        val canMake = drag.item is Item.App && (target is Item.App || target is Item.Folder) &&
            !(drag.fromPage == currentHome && drag.fromIndex == hoverNow)
        if (hoverNow != null && canMake) {
            delay(700)
            dwellSlot = hoverNow
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun finishDrag(drop: Boolean) {
        val item = drag.item ?: return
        val from = drag.fromPage to drag.fromIndex
        drag.item = null
        if (!drop) return
        val page = currentHome
        val slot = slotAt(page, drag.pointer)
        val newId = "f" + System.currentTimeMillis()
        // Dropped on the empty screen at the end: it becomes a real screen with the icon on it
        val base = if (page >= state.layout.pages.size) state.layout.addPage() else state.layout
        if (item is Item.Widget) {
            // A widget lands with the square that was held under the finger, if all the squares it needs are free
            if (slot == null) {
                toast("Let go over one of the squares")
            } else {
                val cols = base.columns
                val col = (slot % cols - drag.grabCol).coerceIn(0, cols - 1)
                val row = (slot / cols - drag.grabRow).coerceIn(0, base.rows - 1)
                // It keeps its size where it fits, moves to the nearest free place if not, and shrinks if it must
                val moved = base.moveWidgetFitting(from.first, from.second, page, row * cols + col)
                if (moved == null) toast("There is no room for the widget on that screen") else {
                    val before = (item as Item.Widget)
                    val after = moved.pages[page].items.filterIsInstance<Item.Widget>().firstOrNull { it.id == before.id }
                    if (after != null && (after.cols != before.cols || after.rows != before.rows)) toast("The widget was made smaller to fit")
                    state.change(moved)
                    // Show where it went: screens that emptied have closed, so the numbers may have moved
                    val now = state.layout.pages.indexOfFirst { p -> p.items.any { it is Item.Widget && it.id == before.id } }
                    if (now >= 0) scope.launch { pagerState.animateScrollToPage(pagerIndexOf(now), animationSpec = pageTurnSpec()) }
                }
            }
            dwellSlot = null
            return
        }
        if (state.settings.customLayout) {
            // Free layout: the icon lands exactly where it is let go; an occupied square swaps, or (held) makes a folder.
            if (slot == null) {
                toast("Let go over one of the squares")
            } else {
                val l = base
                val result = (if (dwellSlot == slot) l.dropOnto(from.first, from.second, page, slot, newId, keepGaps = true) else null)
                    ?: l.placeItem(from.first, from.second, page, slot)
                if (result != null) state.change(result)
            }
        } else {
            val toIndex = slot ?: base.slotsPerPage
            val folder = if (slot != null && dwellSlot == slot) {
                base.dropOnto(from.first, from.second, page, slot, newId)
            } else null
            if (folder != null) {
                state.change(folder)
            } else {
                val moved = base.moveItem(from.first, from.second, page, toIndex)
                if (moved == null) toast("That screen is full") else state.change(moved)
            }
        }
        dwellSlot = null
        @Suppress("UNUSED_VARIABLE") val unused = item
    }

    fun addApp(app: AppInfo, target: PickerTarget) {
        // Adding on the empty screen at the end starts a new screen
        val l = if (target is PickerTarget.OnPage && target.page >= state.layout.pages.size) state.layout.addPage() else state.layout
        when (target) {
            is PickerTarget.OnPage -> {
                val onPage = if (custom && target.slot != null) l.addToPageAt(target.page, target.slot, Item.App(app.key)) else l.addToPage(target.page, Item.App(app.key))
                if (onPage != null) state.change(onPage) else {
                    state.change(l.add(Item.App(app.key)))
                    toast("That screen is full, so it went on another one")
                }
            }
            PickerTarget.OnBar -> state.change(l.addToDock(app.key, state.settings.barSlots))
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    fun addWidgetFrom(choice: WidgetChoice) {
        showWidgetPicker = false
        widgets.add(choice.provider) { id ->
            if (id == null) toast("The widget was not added")
            else if (state.settings.textMode) {
                // Text mode's widgets screen: a full-width widget at the height nearest its own.
                val t = state.textLayout ?: com.glimpsemedia.glimpselauncher.model.TextLayout()
                state.changeTextLayout(t.withWidget(com.glimpsemedia.glimpselauncher.model.TextWidget(id, choice.provider.flattenToString(), textWidgetHeightFor(choice.minHeightDp))))
                toast("${choice.label} added")
            } else {
                // The size of one square, to choose the size that suits the widget
                val gridCols = state.layout.columns
                val cellW = (screenWidthDp - 32 - 12 * (gridCols - 1)) / gridCols
                val cellH = ((screenHeightDp - 340) / state.layout.rows - 12).coerceAtLeast(40)
                val (cols, rows) = snapWidgetSize(choice.minWidthDp, choice.minHeightDp, cellW, cellH, gridCols, state.layout.rows)
                val l = state.layout
                val w = Item.Widget(id, choice.provider.flattenToString(), cols, rows)
                val at = currentHome.coerceAtLeast(0).coerceAtMost(l.pages.size)
                val slot = widgetSlot
                state.change(if (custom && slot != null && at < l.pages.size) l.addWidgetAt(w, at, slot) else l.addWidget(w, at))
                widgetSlot = null
                toast("${choice.label} added to the screen")
            }
        }
    }

    val ps = state.settings
    val palette = remember(ps.tileColor, ps.barColor, ps.darkTheme, ps.tileTransparency, ps.barTransparency, ps.iconColor, ps.iconTransparency, ps.tileShade, ps.barShade, ps.iconShade, ps.tileTextColor, ps.tileTextShade) {
        Palette.of(ps.tileColor, ps.barColor, ps.darkTheme, ps.tileTransparency, ps.barTransparency, ps.iconColor, ps.iconTransparency, ps.tileShade, ps.barShade, ps.iconShade)
            // "Text on the tiles and the bar": one chosen colour for every name there; automatic otherwise.
            .let { p -> if (ps.tileTextColor == 0) p else Color(nameInk(ps.tileTextColor, ps.tileTextShade, 0)).let { c -> p.copy(tileText = c, barText = c) } }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAccent provides Color(shaded((if (state.settings.highlightColor == 0) GlimpseColors.Accent else Color(state.settings.highlightColor)).copy(alpha = 1f).toArgb(), state.settings.highlightShade)),
        LocalMenu provides (if (state.settings.darkTheme) DarkMenu else LightMenu),
        LocalToggleTextMode provides onToggleTextMode,
        LocalTileText provides (if (ps.tileTextColor == 0) 0 else nameInk(ps.tileTextColor, ps.tileTextShade, 0)),
        LocalTileNameScale provides nameStepScale(ps.tileNameStep),
        LocalTileIconScale provides iconStepScale(ps.tileIconStep),
        LocalShowTiles provides state.settings.showTiles,
        LocalChangeIcon provides { req: IconRequest -> iconFor = req },
        LocalFolderStyle provides state.settings.folderStyle,
        LocalFolderIcon provides state.settings.folderIcon,
        LocalFolderColor provides shaded(if (state.settings.folderColor == 0) FOLDER_YELLOW else state.settings.folderColor, state.settings.folderShade),
        LocalIconShape provides state.settings.iconShape,
        LocalTileShape provides state.settings.tileShape,
        LocalSlotShape provides state.settings.slotShape,
    ) {
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); state.touched() } }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).topSafePadding(),
                userScrollEnabled = !drag.active,
                beyondViewportPageCount = pageCount,
                // A swipe carries on at the finger's speed and settles softly on the next screen.
                flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                    state = pagerState, snapAnimationSpec = pageSwipeSpec(),
                ),
            ) { pagerIndex ->
                val index = homePageOf(pagerIndex)
                if (pagerIndex == NOTES_PAGE) NotesScreen(state, onPictureAction, onDeletePictures, notePicture, notesOpenNow, notesDismiss, Modifier.fillMaxSize())
                else if (textMode && index == 0) TextScreen(state, ::onTextEntry, Modifier.fillMaxSize().homeSwipes(enabled = true, onDown = onNotifications, onUp = { showAllApps = true }))
                else if (textMode) TextWidgetsScreen(
                    state, widgets,
                    onAddWidget = { widgetSlot = null; showWidgetPicker = true },
                    onWidgetMenu = { textWidgetMenu = it },
                    modifier = Modifier.fillMaxSize().homeSwipes(enabled = true, onDown = onNotifications, onUp = { showAllApps = true }),
                )
                // Swipe down for the notifications, up for All apps; not while a tile is being dragged.
                else Column(Modifier.fillMaxSize().homeSwipes(enabled = !drag.active, onDown = onNotifications, onUp = { showAllApps = true })) {
                    if (index == 0 && state.settings.showClock) {
                        ClockHeader(
                            settings = state.settings,
                            modifier = if (unlocked) Modifier.combinedClickable(onClick = {}, onLongClick = { emptyMenuPage = index }) else Modifier,
                        )
                    }
                    PageGrid(
                        pageIndex = index,
                        columns = layout.columns,
                        rows = layout.rows,
                        folderColors = layout.folderColors,
                        folderBadges = layout.folderBadges,
                        showNames = state.settings.showTileNames,
                        items = (layout.pages.getOrNull(index) ?: Page()).items,
                        // A tile's own colours with its own darker/lighter change applied.
                        tileColors = layout.tileColors.mapValues { (k, v) -> shaded(v, layout.tileShades[k] ?: 0) },
                        textColors = layout.textColors.mapValues { (k, v) -> shaded(v, layout.textShades[k] ?: 0) },
                        tileTransparency = layout.tileTransparency,
                        iconColors = layout.iconColors.mapValues { (k, v) -> shaded(v, layout.iconShades[k] ?: 0) },
                        iconTransparency = layout.iconTransparency,
                        holdMarks = if (state.settings.showHoldMark) layout.holdActions.keys else emptySet(),
                        holdActions = if (unlocked) emptyMap() else layout.holdActions,
                        onHoldAction = { state.touched(); onHoldAction(it) },
                        apps = apps,
                        unlocked = unlocked,
                        drag = drag,
                        bounds = bounds,
                        hoverSlot = if (drag.active && index == currentHome) slotAt(index, drag.pointer) else null,
                        dwellSlot = if (index == currentHome) dwellSlot else null,
                        onOpenFolder = { openFolder = ItemRef(index, it) },
                        onLaunch = onLaunch,
                        onAllApps = { showAllApps = true },
                        onSearch = { showSearch = true },
                        onEmergency = onEmergencyTap,
                        onMenu = { itemMenu = ItemRef(index, it) },
                        onEmptyTap = { slot -> addChoiceFor = index to slot },
                        onEmptyLong = { emptyMenuPage = index },
                        onDragStart = { item, fromIndex, at ->
                            drag.item = item
                            drag.fromPage = index
                            drag.fromIndex = fromIndex
                            drag.pointer = at
                            drag.moved = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragMove = { by -> drag.pointer += by; drag.moved += by.getDistance() },
                        onDragEnd = { drop -> finishDrag(drop) },
                        onHoldMenu = { fromIndex -> itemMenu = ItemRef(index, fromIndex) },
                        widgets = widgets,
                        scope = scope,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // With no bottom bar, the padlock row keeps clear of the phone's gesture bar itself.
            Box(Modifier.fillMaxWidth().then(if (barShown(state.settings)) Modifier else Modifier.navigationBarsPadding()).padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (holding) {
                    Text("Keep holding to unlock…", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(2f, 2f), 6f)))
                } else {
                    if (currentHome >= 0) PageDots(pageCount, currentHome)
                }
                if (unlocked) {
                    Box(
                        Modifier.align(Alignment.CenterStart).size(56.dp).clip(CircleShape).background(LocalMenu.current.surface)
                            .clickable(onClickLabel = "Open settings") { settingsStack = emptyList(); showSettings = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = LocalMenu.current.text, modifier = Modifier.size(30.dp)) }
                }
                Padlock(
                    unlocked = unlocked,
                    // With a PIN set, the hold asks for it; without one, the hold unlocks.
                    onUnlock = {
                        val step = pinStepForPadlock(state.settings.pinHash != null, state.settings.notesNeedPin, state.notesUnlocked)
                        if (step == null) { state.touched(); state.unlocked = true } else pinStep = step
                    },
                    onLock = { state.unlocked = false },
                    onHolding = { holding = it },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    holdMs = unlockHoldMs(state.settings.quickUnlock),
                )
            }
            if (barShown(state.settings)) Dock(
                layout, state.settings.barSlots, apps, onLaunch, onAllApps = { showAllApps = true }, showNames = state.settings.showDockNames,
                holdMarks = if (state.settings.showHoldMark) layout.holdActions.keys else emptySet(),
                // Unlocked, holding does nothing extra: hold actions run only while locked.
                holdActions = if (unlocked) emptyMap() else layout.holdActions,
                onHoldAction = { state.touched(); onHoldAction(it) },
                onMenu = if (unlocked) { key -> barMenuFor = key ?: "" } else null,
                holdToSearch = state.settings.holdAllAppsToSearch,
                onSearch = { state.touched(); showSearch = true },
            )
        }

        // The dragged icon follows the finger.
        drag.item?.let { item ->
            val app = (item as? Item.App)?.let { apps[it.key] }
            Box(
                Modifier.offset { IntOffset((drag.pointer.x - 60.dp.toPx()).toInt(), (drag.pointer.y - 60.dp.toPx()).toInt()) }
                    .size(120.dp).clip(RoundedCornerShape(28.dp)).background(GlimpseColors.Tile.copy(alpha = 0.95f))
                    .border(3.dp, LocalAccent.current, RoundedCornerShape(28.dp)).padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    app != null -> AppIcon(app, null)
                    item is Item.AllApps -> BuiltInIcon("glimpse:allapps") { AllAppsGlyph() }
                    item is Item.Search -> BuiltInIcon("glimpse:search") { SearchGlyph() }
                    item is Item.Emergency -> BuiltInIcon("glimpse:emergency") { EmergencyGlyph() }
                    item is Item.Widget -> Text("Widget", color = LocalMenu.current.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    else -> FolderGlyph()
                }
            }
        }

        Overlay(showAllApps) {
            AllAppsList(
                apps.values.sortedBy { it.label.lowercase() }, onLaunch = { showAllApps = false; onLaunch(it) }, onClose = { showAllApps = false },
                layout = layout,
                onChange = { state.change(it) },
                filter = appFilter?.takeIf { name -> layout.tags.any { it.name == name } },
                onFilter = { appFilter = it },
                unlocked = unlocked,
                onAppInfo = onAppInfo,
                onUninstall = onUninstall,
                // Unlocked: holding an app's name adds it to the screen being shown (or another with room)
                onAdd = if (unlocked && textMode) ({ app ->
                    // Text mode keeps the tile screens untouched: the app joins the text list instead.
                    toast(if (state.addToTextList(app.key)) "${app.label} added to the list" else "${app.label} is already in the list")
                }) else if (unlocked) ({ app ->
                    if (app.key in state.layout.screenKeys()) toast("${app.label} is already on a screen")
                    else {
                        addApp(app, PickerTarget.OnPage(currentHome.coerceAtLeast(0).coerceAtMost(state.layout.pages.size)))
                        toast("${app.label} added to the screen")
                    }
                }) else null,
            )
        }
        Overlay(showSearch) {
            val sup = search
            val all = sup?.allFiles
            // Whole phone (Plus, with Android's permission) or the picked folders; asked again each time the window opens.
            val permitted = remember(showSearch, all?.permissionVersion) { all?.hasPermission() == true }
            val sources = fileSources(state.settings.allFilesSearch && all?.available == true, permitted, state.settings.searchFolders)
            val locations = remember(showSearch, sources.folders) { if (sources.folders && sup != null) sup.store.folders() else emptyList() }
            val engines = remember(showSearch) { if (state.settings.searchEngines && sup != null) sup.store.engines() else emptyList() }
            // The whole-phone names (Plus) are dropped a while after the window closes, and kept if it opens again.
            LaunchedEffect(showSearch) { if (showSearch) all?.opened() else all?.closed() }
            LaunchedEffect(showSearch, locations, sources) {
                // Closing the window (still drawn while it slides away) starts no walk.
                if (!showSearch) return@LaunchedEffect
                if (sup == null || !sources.any) { searchFiles = null; return@LaunchedEffect }
                if (sources.phone && all != null) { searchFiles = all.last(); searchFiles = all.current(); return@LaunchedEffect }
                val readable = locations.filter { sup.isAvailable(it) }
                searchFiles = sup.index.last(readable)
                searchFiles = sup.index.current(readable)
            }
            val appNames = remember(apps) { apps.values.map { AppName(it.key, it.label) } }
            val filesOn = sources.any
            val input = remember(appNames, searchFiles, filesOn, engines) {
                SearchInput(apps = appNames, files = if (filesOn) searchFiles else null, help = helpTopicList, engines = engines)
            }
            fun locationOf(e: com.glimpsemedia.glimpselauncher.model.Entry) = locations.firstOrNull { sup?.rootIdOf(it) == e.root }
            SearchScreen(
                input = input,
                foldersOn = sources.any,
                appOf = { apps[it] },
                browsing = browsing,
                onBrowseBack = { browsing = null },
                onClose = { showSearch = false; browsing = null },
                onOpen = { hit ->
                    state.touched()
                    when (hit) {
                        is SearchHit.App -> apps[hit.app.key]?.let { showSearch = false; browsing = null; onLaunch(it) }
                        is SearchHit.File -> if (all != null && all.owns(hit.entry)) {
                            if (all.open(hit.entry)) { showSearch = false; browsing = null } else toast("No app on this phone opens this kind of file")
                        } else {
                            val loc = locationOf(hit.entry)
                            if (sup != null && loc != null && sup.openDocument(sup.index.uriOf(loc, hit.entry), hit.entry.mime)) { showSearch = false; browsing = null }
                            else toast("No app on this phone opens this kind of file")
                        }
                        is SearchHit.Folder -> if (all != null && all.owns(hit.entry)) {
                            if (all.openFolder(hit.entry)) { showSearch = false; browsing = null }
                            // No app takes the folder: its contents are shown here instead.
                            else scope.launch { all.childrenOf(hit.entry)?.let { browsing = Browsing(hit.entry, it) } ?: toast("This folder cannot be opened") }
                        } else {
                            val loc = locationOf(hit.entry)
                            when {
                                sup == null || loc == null -> toast("This folder cannot be opened")
                                sup.openFolder(sup.index.uriOf(loc, hit.entry)) -> { showSearch = false; browsing = null }
                                // No app takes the folder: its contents are shown here instead.
                                else -> scope.launch { sup.index.childrenOf(loc, hit.entry)?.let { browsing = Browsing(hit.entry, it) } ?: toast("This folder cannot be opened") }
                            }
                        }
                        // Help and About are open to anyone, locked or not.
                        is SearchHit.Menu -> if (opensWithoutUnlock(hit.hit.section)) {
                            showSearch = false; browsing = null
                            if (hit.hit.section == "help") showHelp = true else showAbout = true
                        } else when (glimpseSettingsTap(unlocked, state.settings.pinHash != null)) {
                            SettingsTap.Open -> {
                                showSearch = false; browsing = null
                                settingsStack = settingsStackFor(hit.hit); settingsFlash = hit.hit.row; showSettings = true
                            }
                            SettingsTap.AskPin -> { showSearch = false; browsing = null; settingsHitAfterUnlock = hit.hit; pinStep = PinStep.Unlock; openSettingsAfterUnlock = true }
                            SettingsTap.HoldPadlock -> toast("Hold the padlock to unlock first")
                        }
                        is SearchHit.Help -> { showSearch = false; browsing = null; helpStart = hit.title; showHelp = true }
                        is SearchHit.Online -> { if (sup?.openWeb(hit.url) == true) { showSearch = false; browsing = null } else toast("No browser on this phone") }
                    }
                },
            )
        }
        Overlay(showSettings) {
            SettingsScreen(
                state = state,
                stack = settingsStack,
                onStack = { settingsStack = it },
                versionName = versionName,
                onClose = { showSettings = false },
                settingsFlash = settingsFlash,
                onFlashTaken = { settingsFlash = null },
                search = search,
                onAddApps = { picker = PickerTarget.OnPage(currentHome.coerceAtLeast(0)) },
                onAddToBar = { picker = PickerTarget.OnBar },
                onWallpaper = onWallpaper,
                onMakeHome = onMakeHome,
                onEmergencySetup = onEmergencySetup,
                onHelp = { showHelp = true },
                onAbout = { showAbout = true },
                onCoffee = onCoffee,
                onAddWidgets = { widgetSlot = null; showWidgetPicker = true },
                widgets = widgets,
                onPinStep = { pinStep = it },
                onThemeWallpaper = onThemeWallpaper,
                onHoldAction = { key, title -> holdMenuFor = key to title },
                onSavedTheme = onSavedTheme,
                onBackup = onBackup,
                onRestorePick = onRestorePick,
                onReportBug = onEmail,
                onSuggest = onSuggest,
                onWebsite = onWebsite,
                onEmailBugLog = onEmailBugLog,
                onClearBugLog = onClearBugLog,
            )
        }
        if (restoring) {
            MenuDialog(title = "Restoring…", onClose = {}, dismissible = false) {
                Text("Glimpse Launcher will restart in a moment.", color = LocalMenu.current.text, fontSize = 18.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        restoreOffer?.let { c ->
            val m = c.manifest
            val date = java.time.Instant.ofEpochMilli(m.created).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", java.util.Locale.getDefault()))
            MenuDialog(title = "Restore the backup from $date?", onClose = { onRestoreAnswer(false) }) {
                Text(
                    listOfNotNull(
                        "Version ${m.version}",
                        if (m.notes == 1) "1 note" else "${m.notes} notes",
                        if (m.themes == 1) "1 saved theme" else "${m.themes} saved themes",
                        if (m.pin) "PIN included" else null,
                    ).joinToString(" · "),
                    color = LocalMenu.current.dimText, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Text("Your current setup will be replaced, and Glimpse Launcher will restart. Widgets will need adding again.",
                    color = LocalMenu.current.text, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                BigButton("Restore", { onRestoreAnswer(true) }, primary = true)
                BigButton("Keep my current setup", { onRestoreAnswer(false) })
            }
        }
        PinSteps(
            step = pinStep,
            onStep = { pinStep = it },
            pinHash = state.settings.pinHash,
            pinSalt = state.settings.pinSalt,
            tries = pinTries,
            // Removing the PIN also turns off "Notes need the PIN".
            onSave = { hash, salt -> state.change(state.settings.copy(pinHash = hash, pinSalt = salt, notesNeedPin = hash != null && state.settings.notesNeedPin)) },
            onUnlocked = {
                state.touched(); state.unlocked = true
                if (openSettingsAfterUnlock) {
                    openSettingsAfterUnlock = false
                    val hit = settingsHitAfterUnlock; settingsHitAfterUnlock = null
                    settingsStack = if (hit == null) emptyList() else settingsStackFor(hit)
                    settingsFlash = hit?.row
                    showSettings = true
                }
            },
            onConfirmPhoneLock = onConfirmPhoneLock,
            toast = ::toast,
            onNotesUnlocked = { state.touched(); state.notesUnlocked = true },
        )
        barMenuFor?.let { key ->
            val appKey = key.ifEmpty { null }
            val name = appKey?.let { apps[it]?.label ?: it } ?: "All apps"
            MenuDialog(title = name, onClose = { barMenuFor = null }) {
                BigButton("Hold action", { holdMenuFor = dockHoldKey(appKey) to name; barMenuFor = null }, primary = true)
                BigButton("Change the icon", {
                    iconFor = appKey?.let { IconRequest(it, name, apps[it]) } ?: IconRequest("glimpse:allapps", "All apps", null); barMenuFor = null
                })
                if (appKey != null) {
                    val slots = state.settings.barSlots
                    if (layout.canMoveInDock(appKey, -1, slots)) BigButton("Move left", { state.change(layout.dockMoved(appKey, -1, slots)); barMenuFor = null })
                    if (layout.canMoveInDock(appKey, +1, slots)) BigButton("Move right", { state.change(layout.dockMoved(appKey, +1, slots)); barMenuFor = null })
                    BigButton("Remove from the bottom bar", { state.change(layout.removeFromDock(appKey)); barMenuFor = null })
                }
            }
        }
        holdMenuFor?.let { (key, title) ->
            // With Hold All apps to search on, the bar's All apps hold belongs to Search; its own action waits.
            if (key == dockHoldKey(null) && state.settings.holdAllAppsToSearch) MenuDialog(title = title, onClose = { holdMenuFor = null }) {
                Text("Holding opens Search (Settings › Search)", color = LocalMenu.current.text, fontSize = 19.sp)
                BigButton("Open Search settings", {
                    holdMenuFor = null; settingsStack = settingsStackFor(SettingsHit("Search", "search", null)); showSettings = true
                }, primary = true)
                BigButton("Close", { holdMenuFor = null })
            } else HoldActionMenu(
                title = title,
                current = layout.holdActions[key],
                apps = apps.values.toList(),
                appLabel = { apps[it]?.label },
                onSet = { state.change(state.layout.withHoldAction(key, it)) },
                onClose = { holdMenuFor = null },
            )
        }
        Overlay(showHelp) { HelpScreen(onClose = { showHelp = false; helpStart = null }, startAt = helpStart) }
        Overlay(showAbout) { AboutScreen(versionName, onEmail, onWebsite, onPrivacy, onClose = { showAbout = false }) }
        EmergencyOverlay(state.emergency, state.settings.emergencyContact, onEmergencyCallNow, onEmergencyCancel)
        HoldCallOverlay(state.holdCall, onHoldCallNow, onHoldCallCancel)
        OverlayFor(picker) { target ->
            val unplaced = apps.values.filter { it.key !in (if (target is PickerTarget.OnBar) layout.dock.toSet() else layout.screenKeys()) }.sortedBy { it.label.lowercase() }
            AppPicker(
                title = if (target is PickerTarget.OnBar) "Add to the bottom bar" else "Add apps to this screen",
                apps = unplaced,
                onPick = { addApp(it, target) },
                onClose = { picker = null },
            )
        }
        OverlayFor(openFolder) { ref ->
            val folder = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index) as? Item.Folder
            if (folder == null) openFolder = null else {
                FolderPanel(
                    folder = folder,
                    apps = apps,
                    unlocked = unlocked,
                    onLaunch = { openFolder = null; onLaunch(it) },
                    onTakeOut = { key -> state.change(layout.takeOutOfFolder(ref.page, ref.index, key, custom)) },
                    onMove = { key, to -> state.change(state.layout.moveInFolder(ref.page, ref.index, key, to)) },
                    onAppInfo = onAppInfo,
                    onRename = { renameFolderFor = ref },
                    onAddApps = { folderAddFor = ref },
                    onClose = { openFolder = null },
                    showNames = state.settings.showTileNames,
                )
            }
        }
        OverlayFor(folderAddFor) { ref ->
            val folder = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index) as? Item.Folder
            if (folder == null) folderAddFor = null else {
                AppPicker(
                    title = "Add to ${folder.name}",
                    apps = apps.values.filter { it.key !in folder.apps }.sortedBy { it.label.lowercase() },
                    onPick = { app ->
                        val added = state.layout.addToFolder(ref.page, ref.index, app.key)
                        if (added == null) { toast("A folder holds up to $FOLDER_MAX apps"); folderAddFor = null }
                        else {
                            state.change(added)
                            // Full now: the list closes by itself.
                            if (((added.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index) as? Item.Folder)?.apps?.size ?: 0) >= FOLDER_MAX) {
                                toast("A folder holds up to $FOLDER_MAX apps"); folderAddFor = null
                            }
                        }
                    },
                    onClose = { folderAddFor = null },
                )
            }
        }
        renameFolderFor?.let { ref ->
            val folder = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index) as? Item.Folder
            if (folder == null) renameFolderFor = null else {
                var text by remember(folder.id) { mutableStateOf(folder.name) }
                MenuDialog(title = "Name of the folder", onClose = { renameFolderFor = null }) {
                    OutlinedTextField(value = text, onValueChange = { text = it.take(24) }, singleLine = true,
                        textStyle = TextStyle(color = LocalMenu.current.text, fontSize = 24.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
                    BigButton("Save the name", { state.change(layout.renameFolder(ref.page, ref.index, text)); renameFolderFor = null }, primary = true)
                }
            }
        }
        if (!state.settings.welcomeShown && state.apps.isNotEmpty()) {
            WelcomeScreen(
                state = state,
                onMakeHome = onMakeHome,
                // Finishing leaves the screen unlocked, so apps can be added at once; with a PIN set and the padlock
                // closed (the welcome shown again, then the screen locked), the PIN is asked for instead.
                onDone = {
                    state.change(state.settings.copy(welcomeShown = true))
                    if (welcomeMayUnlock(state.settings.pinHash, state.unlocked)) { state.unlocked = true; state.touched() } else pinStep = PinStep.Unlock
                },
                onThemeWallpaper = onThemeWallpaper,
                onSavedTheme = onSavedTheme,
                onAddApps = {
                    state.change(state.settings.copy(welcomeShown = true))
                    if (welcomeMayUnlock(state.settings.pinHash, state.unlocked)) {
                        state.unlocked = true; state.touched()
                        picker = PickerTarget.OnPage(0)
                    } else pinStep = PinStep.Unlock
                },
            )
        }
        tileColorFor?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item == null) tileColorFor = null else {
                ColourPicker(
                    title = "Colour of this tile",
                    current = layout.tileColors[item.colorKey] ?: 0,
                    standardName = "Same colour as the other tiles",
                    onPick = { argb -> state.change(layout.withTileColor(item, argb)) },
                    onClose = { tileColorFor = null },
                    extra = {
                        // Darker or lighter applies to the tile's own colour, so it shows once one is chosen.
                        layout.tileColors[item.colorKey]?.let { own ->
                            val shade = layout.tileShades[item.colorKey] ?: 0
                            ShadeRow(shade, Color(shaded(own, shade))) { state.change(state.layout.withTileShade(item, it)) }
                        }
                        TransparencyRow("Transparency of this tile", layout.tileTransparency[item.colorKey], "Same transparency as the other tiles") {
                            state.change(layout.withTileTransparency(item, it))
                        }
                    },
                )
            }
        }
        iconColorFor?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item == null) iconColorFor = null else {
                ColourPicker(
                    title = "Square behind this icon",
                    current = layout.iconColors[item.colorKey] ?: 0,
                    standardName = "Same as the other icons",
                    onPick = { argb -> state.change(layout.withIconColor(item, argb)) },
                    onClose = { iconColorFor = null },
                    extra = {
                        layout.iconColors[item.colorKey]?.let { own ->
                            val shade = layout.iconShades[item.colorKey] ?: 0
                            ShadeRow(shade, Color(shaded(own, shade))) { state.change(state.layout.withIconShade(item, it)) }
                        }
                        TransparencyRow("Transparency of this square (100% hides it)", layout.iconTransparency[item.colorKey], "Same as the other icons") {
                            state.change(layout.withIconTransparency(item, it))
                        }
                    },
                )
            }
        }
        folderColorFor?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item == null) folderColorFor = null else {
                ColourPicker(
                    title = "Colour of this folder",
                    current = layout.folderColors[item.colorKey] ?: 0,
                    standardName = "Same as the other folders",
                    onPick = { argb -> state.change(layout.withFolderColor(item, argb)) },
                    onClose = { folderColorFor = null },
                )
            }
        }
        iconFor?.let { req -> IconPicker(req) { iconFor = null } }
        folderBadgeFor?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item !is Item.Folder) folderBadgeFor = null else {
                FolderBadgeDialog(
                    item.name, layout.badgeOf(item), onPick = { state.change(layout.withFolderBadge(item, it)) }, onClose = { folderBadgeFor = null },
                    currentColour = layout.colourBadgeOf(item), onPickColour = { state.change(layout.withColourBadge(item, it)) },
                )
            }
        }
        textColorFor?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item == null) textColorFor = null else {
                ColourPicker(
                    title = "Colour of this name",
                    current = layout.textColors[item.colorKey] ?: 0,
                    standardName = "Automatic",
                    onPick = { argb -> state.change(layout.withTextColor(item, argb)) },
                    onClose = { textColorFor = null },
                    extra = {
                        layout.textColors[item.colorKey]?.let { own ->
                            val shade = layout.textShades[item.colorKey] ?: 0
                            ShadeRow(shade, Color(shaded(own, shade))) { state.change(state.layout.withTextShade(item, it)) }
                        }
                    },
                )
            }
        }
        addChoiceFor?.let { (page, slot) ->
            MenuDialog(title = "Add to this screen", onClose = { addChoiceFor = null }) {
                BigButton("Add an app", { picker = PickerTarget.OnPage(page, slot); addChoiceFor = null }, primary = true)
                BigButton("Add a widget", { widgetSlot = slot; showWidgetPicker = true; addChoiceFor = null })
            }
        }
        widgetSizeFor?.let { ref ->
            val widget = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index) as? Item.Widget
            if (widget == null) widgetSizeFor = null else {
                MenuDialog(title = "Size of the widget", onClose = { widgetSizeFor = null }) {
                    Text("Squares across × squares down", color = LocalMenu.current.dimText, fontSize = 18.sp)
                    for ((c, r) in widgetSizes(layout.columns, layout.rows)) {
                        BigButton("$c × $r", {
                            val resized = layout.resizeWidget(ref.page, ref.index, c, r)
                            if (resized == null) toast("There is no room for that size on this screen") else { state.change(resized); widgetSizeFor = null }
                        }, primary = widget.cols == c && widget.rows == r)
                    }
                }
            }
        }
        Overlay(showWidgetPicker) {
            val gridCols = layout.columns
            val cellW = (screenWidthDp - 32 - 12 * (gridCols - 1)) / gridCols
            val cellH = ((screenHeightDp - 340) / layout.rows - 12).coerceAtLeast(40)
            WidgetPicker(
                widgets.choices(), onPick = ::addWidgetFrom, onClose = { showWidgetPicker = false },
                suggested = { snapWidgetSize(it.minWidthDp, it.minHeightDp, cellW, cellH, gridCols, layout.rows) },
            )
        }
        textWidgetMenu?.let { id ->
            val t = state.textLayout
            val i = t?.widgets?.indexOfFirst { it.id == id } ?: -1
            if (t == null || i < 0) textWidgetMenu = null else {
                MenuDialog(title = widgets.info(id)?.loadLabel(context.packageManager) ?: "Widget", onClose = { textWidgetMenu = null }) {
                    BigButton("Change the size", { textWidgetSizing = id; textWidgetMenu = null })
                    if (i > 0) BigButton("Move up", { state.changeTextLayout(t.widgetMoved(id, -1)); textWidgetMenu = null })
                    if (i < t.widgets.size - 1) BigButton("Move down", { state.changeTextLayout(t.widgetMoved(id, 1)); textWidgetMenu = null })
                    BigButton("Remove the widget", { state.changeTextLayout(t.withoutWidget(id)); widgets.delete(id); textWidgetMenu = null }, primary = true)
                }
            }
        }
        textWidgetSizing?.let { id ->
            val w = state.textLayout?.widgets?.firstOrNull { it.id == id }
            if (w == null) textWidgetSizing = null else {
                MenuDialog(title = "Change the size", onClose = { textWidgetSizing = null }) {
                    listOf("Small", "Medium", "Large", "Extra large").forEachIndexed { h, label ->
                        BigButton(label, { state.textLayout?.let { state.changeTextLayout(it.widgetResized(id, h)) }; textWidgetSizing = null }, primary = w.height == h)
                    }
                }
            }
        }
        itemMenu?.let { ref ->
            val item = layout.pages.getOrNull(ref.page)?.items?.getOrNull(ref.index)
            if (item is Item.Widget) {
                MenuDialog(title = "Widget", onClose = { itemMenu = null }) {
                    BigButton("Change the size", { widgetSizeFor = ref; itemMenu = null })
                    BigButton("Remove the widget", {
                        state.change(layout.removeWidget(ref.page, ref.index)); widgets.delete(item.id); itemMenu = null
                    }, primary = true)
                }
            } else if (item == null) itemMenu = null else {
                val app = (item as? Item.App)?.let { apps[it.key] }
                MenuDialog(title = app?.label ?: if (item is Item.Folder) item.name else if (item is Item.Emergency) "Emergency" else if (item is Item.Search) "Search" else "All apps", onClose = { itemMenu = null }) {
                    if (ref.page > 0 && !layout.pages[ref.page - 1].isFull) {
                        BigButton("Move to the previous screen", { state.change(layout.moveItem(ref.page, ref.index, ref.page - 1, 99)!!); itemMenu = null })
                    }
                    if (ref.page < layout.pages.size - 1 && !layout.pages[ref.page + 1].isFull) {
                        BigButton("Move to the next screen", { state.change(layout.moveItem(ref.page, ref.index, ref.page + 1, 99)!!); itemMenu = null })
                    }
                    if (item is Item.Folder) {
                        BigButton("Open the folder", { openFolder = ref; itemMenu = null })
                        BigButton("Change the folder's name", { renameFolderFor = ref; itemMenu = null })
                        BigButton("Add apps to this folder", { folderAddFor = ref; itemMenu = null })
                    }
                    // Without tiles there is no card to colour.
                    when {
                        app != null -> IconRequest(app.key, app.label, app)
                        item is Item.AllApps -> IconRequest("glimpse:allapps", "All apps", null)
                        item is Item.Search -> IconRequest("glimpse:search", "Search", null)
                        item is Item.Emergency -> IconRequest("glimpse:emergency", "Emergency", null)
                        else -> null
                    }?.let { req -> BigButton("Change the icon", { iconFor = req; itemMenu = null }) }
                    if (state.settings.showTiles) BigButton("Change this tile's colour", { tileColorFor = ref; itemMenu = null })
                    BigButton("Change the square behind the icon", { iconColorFor = ref; itemMenu = null })
                    // With the folder-icon look, a folder can have its own colour.
                    if (item is Item.Folder && state.settings.folderStyle == 1) BigButton("Change the folder colour", { folderColorFor = ref; itemMenu = null })
                    com.glimpsemedia.glimpselauncher.model.badgeMenuLine(item, state.settings.folderStyle, layout.anyBadgeOf(item))?.let { line ->
                        BigButton(line, { folderBadgeFor = ref; itemMenu = null })
                    }
                    BigButton("Change the colour of the name", { textColorFor = ref; itemMenu = null })
                    item.holdKey?.let { key ->
                        val name = app?.label ?: (item as? Item.Folder)?.name ?: if (item is Item.Search) "Search" else "All apps"
                        BigButton("Hold action", { holdMenuFor = key to name; itemMenu = null })
                    }
                    BigButton(
                        if (item is Item.Folder) "Remove the folder (the apps stay on the phone)" else "Remove from the screen",
                        { state.change(layout.removeFromPage(ref.page, ref.index, custom)); itemMenu = null },
                        primary = true,
                    )
                    if (app != null) BigButton("App info (to uninstall)", { onAppInfo(app); itemMenu = null })
                }
            }
        }
        emptyMenuPage?.let { page ->
            MenuDialog(title = "This screen", onClose = { emptyMenuPage = null }) {
                BigButton("Add apps", { picker = PickerTarget.OnPage(page); emptyMenuPage = null }, primary = true)
                BigButton("Add a widget", { widgetSlot = null; showWidgetPicker = true; emptyMenuPage = null })
                if (!layout.hasAllAppsIcon) {
                    BigButton("Add the All apps icon", {
                        state.change(layout.addToPage(page, Item.AllApps) ?: layout.add(Item.AllApps)); emptyMenuPage = null
                    })
                }
                if (!layout.hasSearchIcon) {
                    BigButton("Add the Search icon", {
                        state.change(layout.addToPage(page, Item.Search) ?: layout.add(Item.Search)); emptyMenuPage = null
                    })
                }
                if (!layout.hasEmergencyIcon) {
                    BigButton("Add the Emergency icon", {
                        state.change(layout.addToPage(page, Item.Emergency) ?: layout.add(Item.Emergency)); emptyMenuPage = null
                    })
                }
                if (layout.pages.getOrNull(page)?.isEmpty == true && layout.pages.size > 1) {
                    BigButton("Remove this empty screen", { state.change(layout.removePageIfEmpty(page)); emptyMenuPage = null })
                }
                BigButton("Change the wallpaper", { onWallpaper(); emptyMenuPage = null })
            }
        }
    }

    // Back does nothing on the home screen; it closes whatever is open.
    BackHandler(enabled = true) {
        when {
            showAbout -> showAbout = false
            showHelp -> showHelp = false
            showWidgetPicker -> showWidgetPicker = false
            addChoiceFor != null -> addChoiceFor = null
            widgetSizeFor != null -> widgetSizeFor = null
            tileColorFor != null -> tileColorFor = null
            iconColorFor != null -> iconColorFor = null
            folderColorFor != null -> folderColorFor = null
            textColorFor != null -> textColorFor = null
            folderAddFor != null -> folderAddFor = null
            renameFolderFor != null -> renameFolderFor = null
            openFolder != null -> openFolder = null
            itemMenu != null -> itemMenu = null
            emptyMenuPage != null -> emptyMenuPage = null
            picker != null -> picker = null
            holdMenuFor != null -> holdMenuFor = null
            // Back goes back one Settings page; on the first page it closes Settings.
            showSettings && settingsStack.isNotEmpty() -> settingsStack = settingsStack.dropLast(1)
            showSettings -> showSettings = false
            showAllApps -> showAllApps = false
            browsing != null -> browsing = null
            showSearch -> showSearch = false
            state.textFolderOpen != null -> state.textFolderOpen = null
        }
    }
    }
}

/** Up to eight large tiles, two across and four down. When unlocked, free slots show a plus and tiles can be dragged. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageGrid(
    pageIndex: Int,
    columns: Int,
    rows: Int = PAGE_ROWS,
    /** Each folder's own colour for the folder-icon look. */
    folderColors: Map<String, Int> = emptyMap(),
    folderBadges: Map<String, String> = emptyMap(),
    showNames: Boolean,
    items: List<Item?>,
    tileColors: Map<String, Int>,
    /** Each tile's own name colour, darker or lighter already applied. */
    textColors: Map<String, Int> = emptyMap(),
    tileTransparency: Map<String, Int>,
    iconColors: Map<String, Int>,
    iconTransparency: Map<String, Int>,
    apps: Map<String, AppInfo>,
    /** The hold keys of tiles that show the lightning mark. */
    holdMarks: Set<String>,
    /** The actions held tiles run: empty while unlocked. */
    holdActions: Map<String, HoldAction>,
    onHoldAction: (HoldAction) -> Unit,
    unlocked: Boolean,
    drag: DragState,
    bounds: HashMap<Int, Rect>,
    hoverSlot: Int?,
    dwellSlot: Int?,
    onOpenFolder: (Int) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onAllApps: () -> Unit,
    onSearch: () -> Unit,
    onEmergency: () -> Unit,
    onMenu: (Int) -> Unit,
    onEmptyTap: (Int) -> Unit,
    onEmptyLong: () -> Unit,
    onDragStart: (Item, Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Boolean) -> Unit,
    onHoldMenu: (Int) -> Unit,
    widgets: WidgetSupport,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val toggleTextMode = LocalToggleTextMode.current
    // The drag handlers below are set up once per tile and outlive recompositions; they must always call the
    // latest callbacks, else they act on old settings (switching on Free layout while unlocked did nothing).
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDragMove by rememberUpdatedState(onDragMove)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    val latestHoldMenu by rememberUpdatedState(onHoldMenu)
    BoxWithConstraints(modifier.fillMaxWidth()) {
    // The smaller grids sit closer together.
    val tight = (columns to rows) in com.glimpsemedia.glimpselauncher.model.TIGHT_GRIDS
    val gap = if (tight) 8.dp else 12.dp
    val cellW = (maxWidth - 32.dp - gap * (columns - 1)) / columns
    val cellH = (maxHeight - 16.dp - gap * (rows - 1)) / rows
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (row in 0 until rows) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until columns) {
                    val slot = row * columns + col
                    val item = items.getOrNull(slot)
                    val app = (item as? Item.App)?.let { apps[it.key] }
                    var holdJob: Job? = null
                    fun holdOf(i: Item): (() -> Unit)? = i.holdKey?.let { holdActions[it] }?.let { a -> { onHoldAction(a) } }
                    val hovered = hoverSlot == slot
                    val makingFolder = dwellSlot == slot
                    val beingDragged = drag.active && drag.fromPage == pageIndex && drag.fromIndex == slot && item == drag.item
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .onGloballyPositioned { bounds[pageIndex * BOUNDS_STRIDE + slot] = it.boundsInRoot() }
                            .then(
                                if (makingFolder) Modifier.border(8.dp, LocalAccent.current, tileShapeOf(LocalTileShape.current, 28.dp))
                                else if (hovered) Modifier.border(4.dp, LocalAccent.current, tileShapeOf(LocalTileShape.current, 28.dp)) else Modifier,
                            )
                            .alpha(if (beingDragged) 0.3f else 1f)
                            .then(
                                if (unlocked && item != null && item !is Item.Widget && item !is Item.Covered) Modifier.pointerInput(item, slot) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { at ->
                                            latestDragStart(item, slot, (bounds[pageIndex * BOUNDS_STRIDE + slot]?.topLeft ?: Offset.Zero) + at)
                                            // Holding still for five seconds opens the menu instead.
                                            holdJob = scope.launch {
                                                delay(HOLD_MENU_MS)
                                                if (drag.moved < 24f) { latestDragEnd(false); latestHoldMenu(slot) }
                                            }
                                        },
                                        onDrag = { change, amount -> change.consume(); latestDragMove(amount) },
                                        onDragEnd = { holdJob?.cancel(); latestDragEnd(true) },
                                        onDragCancel = { holdJob?.cancel(); latestDragEnd(false) },
                                    )
                                } else Modifier,
                            ),
                    ) {
                        when {
                            item == null -> EmptySlot(unlocked, { onEmptyTap(slot) }, onEmptyLong)
                            app != null -> BigTile(app.label, { onLaunch(app) }, Modifier.fillMaxSize(), tileColors[item.colorKey] ?: 0, tileTransparency[item.colorKey], Badges.forPackage(app.component.packageName), iconColors[item.colorKey] ?: 0, iconTransparency[item.colorKey], compact = columns > 2, tight = tight, sizes = gridSizes(columns, rows), showName = showNames, holdMark = item.holdKey in holdMarks, onHold = holdOf(item), textColor = textColors[item.colorKey] ?: 0) { AppIcon(app, null) }
                            item is Item.Emergency -> BigTile("Emergency", onEmergency, Modifier.fillMaxSize(), tileColors[item.colorKey] ?: EMERGENCY_RED, tileTransparency[item.colorKey], 0, iconColors[item.colorKey] ?: 0, iconTransparency[item.colorKey], compact = columns > 2, tight = tight, sizes = gridSizes(columns, rows), showName = showNames, holdMark = item.holdKey in holdMarks, onHold = holdOf(item), textColor = textColors[item.colorKey] ?: 0) { BuiltInIcon("glimpse:emergency") { EmergencyGlyph() } }
                            item is Item.AllApps -> BigTile("All apps", onAllApps, Modifier.fillMaxSize(), tileColors[item.colorKey] ?: 0, tileTransparency[item.colorKey], 0, iconColors[item.colorKey] ?: 0, iconTransparency[item.colorKey], compact = columns > 2, tight = tight, sizes = gridSizes(columns, rows), showName = showNames, holdMark = item.holdKey in holdMarks, onHold = holdOf(item), textColor = textColors[item.colorKey] ?: 0) { BuiltInIcon("glimpse:allapps") { AllAppsGlyph() } }
                            item is Item.Search -> BigTile("Search", onSearch, Modifier.fillMaxSize(), tileColors[item.colorKey] ?: 0, tileTransparency[item.colorKey], 0, iconColors[item.colorKey] ?: 0, iconTransparency[item.colorKey], compact = columns > 2, tight = tight, sizes = gridSizes(columns, rows), showName = showNames, holdMark = item.holdKey in holdMarks, onHold = holdOf(item), textColor = textColors[item.colorKey] ?: 0) { BuiltInIcon("glimpse:search") { SearchGlyph() } }
                            item is Item.Folder -> {
                                // The folder-icon look: a folder in its own colour (or the shared one), with no square behind it.
                                val asIcon = LocalFolderStyle.current == 1
                                val folderInk = folderColors[item.colorKey] ?: LocalFolderColor.current
                                val badge = folderBadges[item.colorKey]?.let { n -> com.glimpsemedia.glimpselauncher.model.TextIcon.entries.firstOrNull { it.name == n } }?.takeIf { asIcon }
                                val colourBadge = folderBadges[item.colorKey]?.takeIf { asIcon && it.startsWith(com.glimpsemedia.glimpselauncher.model.COLOUR_BADGE) }
                                    ?.removePrefix(com.glimpsemedia.glimpselauncher.model.COLOUR_BADGE)?.let { n -> com.glimpsemedia.glimpselauncher.model.TextIcon.entries.firstOrNull { it.name == n } }
                                BigTile(item.name, { onOpenFolder(slot) }, Modifier.fillMaxSize(), tileColors[item.colorKey] ?: 0, tileTransparency[item.colorKey], item.apps.sumOf { k -> apps[k]?.let { Badges.forPackage(it.component.packageName) } ?: 0 }, iconColors[item.colorKey] ?: 0, if (asIcon) 100 else iconTransparency[item.colorKey], compact = columns > 2, tight = tight, sizes = gridSizes(columns, rows), showName = showNames, holdMark = item.holdKey in holdMarks, onHold = holdOf(item), textColor = textColors[item.colorKey] ?: 0, spokenExtra = (badge ?: colourBadge)?.let { folderBadgeSpoken(it) }) {
                                    if (asIcon) Box {
                                        FolderPicture(LocalFolderIcon.current, Color(folderInk))
                                        badge?.let { FolderBadge(it, Color(folderInk)) }
                                        colourBadge?.let { ColourFolderBadge(it) }
                                    } else FolderPreview(item.apps.mapNotNull { apps[it] })
                                }
                            }
                            else -> Unit  // a widget is drawn over the grid below; a covered square shows nothing
                        }
                        if (unlocked && item != null && item !is Item.Widget && item !is Item.Covered) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(if (columns > 2) 2.dp else 6.dp).size(if (columns > 2) 30.dp else 48.dp).clip(CircleShape).background(LocalAccent.current)
                                    .clickable(onClickLabel = "Menu") { onMenu(slot) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for " + (app?.label ?: (item as? Item.Folder)?.name ?: if (item is Item.Search) "Search" else "All apps"), tint = readableOn(LocalAccent.current), modifier = Modifier.size(if (columns > 2) 20.dp else 30.dp)) }
                        }
                    }
                }
            }
        }
    }
    // Widgets sit over the squares they cover
    items.forEachIndexed { slot, item ->
        if (item is Item.Widget) {
            val col = slot % columns
            val row = slot / columns
            val w = cellW * item.cols + gap * (item.cols - 1)
            val h = cellH * item.rows + gap * (item.rows - 1)
            // Remembered: a plain local would start again at zero after a recomposition and the drag would jump to the top
            val widgetTop = remember { arrayOf(Offset.Zero) }
            var holdJob: Job? = null
            val dragged = drag.active && drag.fromPage == pageIndex && drag.fromIndex == slot && drag.item == item
            val cellPx = with(androidx.compose.ui.platform.LocalDensity.current) { (cellW + gap).toPx() }
            val cellHpx = with(androidx.compose.ui.platform.LocalDensity.current) { (cellH + gap).toPx() }
            Box(
                Modifier.offset(x = 16.dp + (cellW + gap) * col, y = 8.dp + (cellH + gap) * row).size(w, h).clip(RoundedCornerShape(28.dp))
                    .onGloballyPositioned { widgetTop[0] = it.boundsInRoot().topLeft }
                    .alpha(if (dragged) 0.3f else 1f),
            ) {
                WidgetBox(widgets, item, w, h, unlocked)
                if (unlocked) {
                    // Over the widget: touches do not reach it, and holding then dragging moves it
                    Box(
                        Modifier.fillMaxSize().pointerInput(item.provider) {
                            // A tap does nothing, except on the Text mode switch, which works unlocked too.
                            detectTapGestures { if (isTextModeSwitch(item.provider, context.packageName)) toggleTextMode() }
                        }.pointerInput(item, slot) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { at ->
                                    drag.grabCol = (at.x / cellPx).toInt().coerceIn(0, item.cols - 1)
                                    drag.grabRow = (at.y / cellHpx).toInt().coerceIn(0, item.rows - 1)
                                    latestDragStart(item, slot, widgetTop[0] + at)
                                    holdJob = scope.launch {
                                        delay(HOLD_MENU_MS)
                                        if (drag.moved < 24f) { latestDragEnd(false); latestHoldMenu(slot) }
                                    }
                                },
                                onDrag = { change, amount -> change.consume(); latestDragMove(amount) },
                                onDragEnd = { holdJob?.cancel(); latestDragEnd(true) },
                                onDragCancel = { holdJob?.cancel(); latestDragEnd(false) },
                            )
                        },
                    )
                }
                if (unlocked) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(48.dp).clip(CircleShape).background(LocalAccent.current)
                            .clickable(onClickLabel = "Menu") { onMenu(slot) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for the widget", tint = readableOn(LocalAccent.current), modifier = Modifier.size(30.dp)) }
                }
            }
        }
    }
    }
}

/** True when the view has been laid out and draws nothing at all (every pixel see-through). */
private fun isDrawnBlank(view: android.view.View): Boolean {
    if (view.width < 8 || view.height < 8) return false
    return try {
        val scale = 8
        val bitmap = android.graphics.Bitmap.createBitmap(view.width / scale, view.height / scale, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.scale(1f / scale, 1f / scale)
        view.draw(canvas)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()
        pixels.none { (it ushr 24) > 8 }
    } catch (_: Exception) {
        false
    }
}

/** A widget: what the phone's widget shows, locked in place. When the padlock is open it only shows, so a touch cannot start anything by accident. */
@Composable
internal fun WidgetBox(widgets: WidgetSupport, widget: Item.Widget, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, unlocked: Boolean) {
    val view = remember(widget.id) { widgets.createView(widget.id) }
    if (view == null) {
        Box(Modifier.fillMaxSize().background(LocalPalette.current.tile), contentAlignment = Alignment.Center) {
            Text("This widget is no longer on the phone", color = LocalPalette.current.tileText, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
        }
    } else {
        // Told once per size, not on every redraw: a widget that is told its size again starts over
        androidx.compose.runtime.LaunchedEffect(view, width, height) { widgets.resize(view, width.value.toInt(), height.value.toInt()) }
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { (view.parent as? android.view.ViewGroup)?.removeView(view); view },
            modifier = Modifier.fillMaxSize(),
        )
        // Some widgets (Google's search widgets on some phones) draw nothing at all: say so, so it is not a mystery
        var empty by remember(view) { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(view) {
            // Look at what it has drawn a few times; widgets can take a moment to fill in
            repeat(6) {
                delay(2500)
                empty = isDrawnBlank(view)
                if (!empty) return@LaunchedEffect
            }
        }
        if (empty) {
            Box(Modifier.fillMaxSize().background(LocalPalette.current.tile.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Text("This widget has nothing to show. It may not work on this phone: remove it and try another.",
                    color = LocalPalette.current.tileText, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 56.dp, vertical = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptySlot(unlocked: Boolean, onTap: () -> Unit, onLong: () -> Unit) {
    if (!unlocked) return
    // With the tiles off an empty square is only a faint outline, so it does not look like a tile.
    val tiles = LocalShowTiles.current
    val shape = tileShapeOf(LocalSlotShape.current, 28.dp)
    Box(
        Modifier.fillMaxSize().clip(shape).background(Color.White.copy(alpha = if (tiles) 0.12f else 0f))
            .border(if (tiles) 2.dp else 1.5.dp, Color.White.copy(alpha = if (tiles) 0.5f else 0.3f), shape)
            .combinedClickable(onClickLabel = "Add an app here", onClick = onTap, onLongClick = onLong),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Add, contentDescription = "Add an app here", tint = Color.White.copy(alpha = if (tiles) 1f else 0.7f), modifier = Modifier.size(if (tiles) 44.dp else 28.dp)) }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            Box(
                Modifier.padding(horizontal = 5.dp).size(if (i == current) 14.dp else 10.dp).clip(CircleShape)
                    .background(if (i == current) LocalAccent.current else Color.White.copy(alpha = 0.6f)),
            )
        }
    }
}

/** Four or six apps, two or three each side of the All apps button (the outer ones hidden with four). */
@Composable
private fun Dock(
    layout: Layout, slots: Int, apps: Map<String, AppInfo>, onLaunch: (AppInfo) -> Unit, onAllApps: () -> Unit, showNames: Boolean = true,
    holdMarks: Set<String> = emptySet(), holdActions: Map<String, HoldAction> = emptyMap(), onHoldAction: (HoldAction) -> Unit = {},
    /** Unlocked only: opens a bar icon's menu (null is the All apps button). */
    onMenu: ((key: String?) -> Unit)? = null,
    /** Hold All apps to search: while locked, holding the All apps button opens Search. */
    holdToSearch: Boolean = false,
    onSearch: () -> Unit = {},
) {
    fun holdOf(key: String?): (() -> Unit)? = holdActions[dockHoldKey(key)]?.let { a -> { onHoldAction(a) } }
    // Locked (no menus): the switch takes the All apps hold; unlocked, holding opens its menu as before.
    val allAppsHold: (() -> Unit)? = when (val h = dockAllAppsHold(holdToSearch && onMenu == null, holdActions[dockHoldKey(null)])) {
        DockAllHold.OpenSearch -> onSearch
        is DockAllHold.Action -> ({ onHoldAction(h.action) })
        null -> null
    }
    // Each side shows its apps nearest All apps; free places are at the outer ends.
    val half = slots / 2
    val (leftKeys, rightKeys) = layout.dockSides(slots)
    val left: List<AppInfo?> = List(half - leftKeys.size) { null } + leftKeys.map { apps[it] }
    val right: List<AppInfo?> = rightKeys.map { apps[it] } + List(half - rightKeys.size) { null }
    val compact = slots > 4
    // Five or seven names have to fit across: the bar's text grows with the phone's font size, but only a little.
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, minOf(density.fontScale, 1.15f)),
    ) {
    Row(
        Modifier.fillMaxWidth().background(LocalPalette.current.bar).navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val slot = Modifier.weight(1f)
        fun menuOf(key: String?): (() -> Unit)? = onMenu?.let { m -> { m(key) } }
        for (app in left) app?.let { DockTile(it, { onLaunch(it) }, slot, showNames, dockHoldKey(it.key) in holdMarks, holdOf(it.key), menuOf(it.key), compact) } ?: Box(slot)
        AllAppsButton(onAllApps, slot, showNames, !holdToSearch && dockHoldKey(null) in holdMarks, allAppsHold, menuOf(null), compact, searchMark = holdToSearch)
        for (app in right) app?.let { DockTile(it, { onLaunch(it) }, slot, showNames, dockHoldKey(it.key) in holdMarks, holdOf(it.key), menuOf(it.key), compact) } ?: Box(slot)
    }
    }
}

/**
 * Every app A to Z under letter headings, with a search box. The Filter button at the top shows all apps or only
 * those with one tag. Unlocked, holding an app offers to add it to the screen or to give it tags.
 */
@Composable
private fun AllAppsList(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onAdd: ((AppInfo) -> Unit)? = null,
    layout: Layout,
    onChange: (Layout) -> Unit,
    filter: String?,
    onFilter: (String?) -> Unit,
    unlocked: Boolean,
    onAppInfo: (AppInfo) -> Unit = {},
    onUninstall: (AppInfo) -> Unit = {},
) {
    var choosingFilter by remember { mutableStateOf(false) }
    var managingTags by remember { mutableStateOf(false) }
    var heldApp by remember { mutableStateOf<AppInfo?>(null) }
    var taggingApp by remember { mutableStateOf<AppInfo?>(null) }
    val shown = if (filter == null) apps else layout.appsTagged(filter).let { keys -> apps.filter { it.key in keys } }
    SearchableAppList(
        "All apps", "Close", shown,
        if (filter == null) "There are no apps." else "No app has the tag “$filter” yet." + if (unlocked) " Hold an app in All apps to give it a tag." else "",
        onClose, onLaunch,
        searchOnOpen = true,
        onHold = if (unlocked && onAdd != null) ({ heldApp = it }) else null,
        hint = if (unlocked && onAdd != null) "Unlocked: hold an app's name to add it to the screen or give it a tag" else null,
        titleContent = {
            // Where the "All apps" heading was: a button showing what is listed, which changes it.
            Text(
                if (filter == null) "Filter: All apps" else "Filter: $filter",
                color = LocalMenu.current.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp).clip(RoundedCornerShape(20.dp)).background(LocalMenu.current.button)
                    .clickable(onClickLabel = "Choose which apps are listed") { choosingFilter = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    )
    if (choosingFilter) {
        MenuDialog(title = "Show", onClose = { choosingFilter = false }) {
            BigButton("All apps", { onFilter(null); choosingFilter = false }, primary = filter == null)
            for (tag in layout.tags) {
                BigButton("${tag.name} (${tag.apps.count { k -> apps.any { it.key == k } }})", { onFilter(tag.name); choosingFilter = false }, primary = filter == tag.name)
            }
            if (layout.tags.isEmpty()) {
                Text(
                    if (unlocked) "There are no tags yet. Hold an app to give it a tag, or make one with Manage tags." else "There are no tags yet. Unlock to make some.",
                    color = LocalMenu.current.dimText, fontSize = 18.sp,
                )
            }
            if (unlocked) BigButton("Manage tags", { managingTags = true; choosingFilter = false })
        }
    }
    if (managingTags) {
        var renaming by remember { mutableStateOf<String?>(null) }
        var newName by remember { mutableStateOf("") }
        MenuDialog(title = "Tags", onClose = { managingTags = false }) {
            for (tag in layout.tags) {
                if (renaming == tag.name) {
                    var text by remember(tag.name) { mutableStateOf(tag.name) }
                    OutlinedTextField(value = text, onValueChange = { text = it.take(24) }, singleLine = true, label = { Text("New name") },
                        textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
                    BigButton("Save the name", {
                        val renamed = layout.withTagRenamed(tag.name, text)
                        if (renamed != layout) {
                            onChange(renamed)
                            if (filter == tag.name) onFilter(com.glimpsemedia.glimpselauncher.model.cleanTagName(text))
                        }
                        renaming = null
                    }, primary = true)
                } else {
                    NameAndButton(start = {
                        Text(tag.name, color = LocalMenu.current.text, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton("Rename", { renaming = tag.name }, fontSize = 16.sp)
                            BigButton("Delete", {
                                onChange(layout.withoutTag(tag.name))
                                if (filter == tag.name) onFilter(null)
                            }, fontSize = 16.sp)
                        }
                    }
                }
            }
            Text("Deleting a tag never removes an app.", color = LocalMenu.current.dimText, fontSize = 16.sp)
            OutlinedTextField(value = newName, onValueChange = { newName = it.take(24) }, singleLine = true, label = { Text("A new tag, such as Games") },
                textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
            BigButton("Add the tag", { onChange(layout.withNewTag(newName)); newName = "" }, primary = true)
        }
    }
    heldApp?.let { app ->
        MenuDialog(title = app.label, onClose = { heldApp = null }) {
            if (onAdd != null) BigButton("Add to the screen", { onAdd(app); heldApp = null }, primary = true)
            BigButton("Tags", { taggingApp = app; heldApp = null })
            val changeIcon = LocalChangeIcon.current
            BigButton("Change the icon", { changeIcon(IconRequest(app.key, app.label, app)); heldApp = null })
            BigButton("App info", { onAppInfo(app); heldApp = null })
            BigButton("Uninstall", { onUninstall(app); heldApp = null })
        }
    }
    taggingApp?.let { app ->
        var newName by remember { mutableStateOf("") }
        MenuDialog(title = "Tags for ${app.label}", onClose = { taggingApp = null }) {
            val mine = layout.tagsOf(app.key)
            for (tag in layout.tags) {
                SwitchRow(tag.name, tag.name in mine) { on -> onChange(layout.withAppTagged(app.key, tag.name, on)) }
            }
            OutlinedTextField(value = newName, onValueChange = { newName = it.take(24) }, singleLine = true, label = { Text("A new tag, such as Games") },
                textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
            BigButton("Add the new tag to ${app.label}", {
                if (newName.isNotBlank()) onChange(layout.withAppTagged(app.key, newName.trim(), true))
                newName = ""
            }, primary = true)
        }
    }
}

/** Apps not yet on a screen or the bottom bar; picking one adds it and it leaves the list. */
@Composable
private fun AppPicker(title: String, apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onClose: () -> Unit) {
    SearchableAppList(
        title, "Done", apps, "Every app is already on a screen.", onClose, onPick, pickLabel = "Add",
        rowEnd = { Icon(Icons.Filled.Add, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(36.dp)) },
    )
}

/** One heading in Settings: it opens the section under it, or (without [arrow]) does one thing when touched. */
/**
 * The set-up settings, as headings that each open the section under them. Everything starts closed; opening one
 * closes the one that was open, so the list stays short.
 */
@Composable
private fun SettingsScreen(
    state: LauncherState,
    stack: List<String>,
    onStack: (List<String>) -> Unit,
    versionName: String,
    onClose: () -> Unit,
    onAddApps: () -> Unit,
    onAddToBar: () -> Unit,
    onWallpaper: () -> Unit,
    onMakeHome: () -> Unit,
    onEmergencySetup: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onCoffee: () -> Unit,
    onAddWidgets: () -> Unit,
    widgets: WidgetSupport,
    onPinStep: (PinStep) -> Unit,
    /** Opens the hold-action menu for a key, with the icon's name. */
    onHoldAction: (key: String, title: String) -> Unit,
    onThemeWallpaper: (c1: Int, c2: Int, c3: Int, c4: Int, dark: Boolean) -> Unit,
    onSavedTheme: (SavedThemeAction) -> Unit,
    onBackup: () -> Unit = {},
    onRestorePick: () -> Unit = {},
    onReportBug: () -> Unit = {},
    onSuggest: () -> Unit = {},
    onWebsite: () -> Unit = {},
    onEmailBugLog: () -> Unit = {},
    onClearBugLog: () -> Unit = {},
    settingsFlash: String? = null,
    onFlashTaken: () -> Unit = {},
    search: com.glimpsemedia.glimpselauncher.data.SearchSupport? = null,
) = MenuText {
    val s = state.settings
    val layout = state.layout
    var confirmReset by remember { mutableStateOf(0) }  // 0 not asking, 1 an empty start, 2 every app A to Z
    var picking by remember { mutableStateOf<ColorTarget?>(null) }
    var editContact by remember { mutableStateOf<Int?>(null) }
    var choosingWallpaper by remember { mutableStateOf(false) }
    var managingTheme by remember { mutableStateOf<com.glimpsemedia.glimpselauncher.data.SavedTheme?>(null) }
    managingTheme?.let { t -> SavedThemeMenu(t, onSavedTheme, onClose = { managingTheme = null }) }
    if (choosingWallpaper) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { choosingWallpaper = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ThemeWallpaperList(s, onPick = { c, dark -> choosingWallpaper = false; onThemeWallpaper(c[0], c[1], c[2], c[3], dark) }, onClose = { choosingWallpaper = false })
        }
    }
    // Notification access is the phone's own setting: read it again whenever Glimpse comes back to the front.
    val ctx = LocalContext.current
    var badgesOn by remember { mutableStateOf(Badges.allowed(ctx)) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) badgesOn = Badges.allowed(ctx) }
        val lc = (ctx as androidx.lifecycle.LifecycleOwner).lifecycle
        lc.addObserver(obs)
        onDispose { lc.removeObserver(obs) }
    }
    val allFilesPermitted = search?.allFiles?.let { a -> remember(a.permissionVersion) { !a.available || a.hasPermission() } } ?: true
    val facts = SummaryFacts(s, layout.columns, themeNameOf(s, state.savedThemes.list), state.isDefaultHome, versionName, badges = badgesOn, rows = layout.rows, allFilesPermitted = allFilesPermitted)
    fun say(text: String) = android.widget.Toast.makeText(ctx, text, android.widget.Toast.LENGTH_LONG).show()

    /** A row of equal choice buttons under a small title. */
    @Composable
    fun <T> Choices(title: String, options: List<Pair<T, String>>, chosen: T, onPick: (T) -> Unit) {
        CardTitle(title)
        ButtonRow(options.size, minButtonWidth(options.map { it.second }, 16f).dp, Modifier.padding(top = 8.dp)) { i, m ->
            val (value, label) = options[i]
            BigButton(label, { onPick(value) }, m, primary = chosen == value, fontSize = 16.sp)
        }
    }

    val shown: (String, String) -> Boolean = { _, label ->
        when (label) {
            "Add the All apps icon" -> !layout.hasAllAppsIcon
            "Add an app to the bottom bar" -> layout.dockHasRoom(s.barSlots)
            "Number to ring", "Seconds before it rings" -> s.emergencyEnabled
            "Add the Emergency icon to the screen" -> s.emergencyEnabled && !layout.hasEmergencyIcon
            "Set a PIN" -> s.pinHash == null
            "Change the PIN", "Remove the PIN" -> s.pinHash != null
            else -> true
        }
    }

    SettingsPages(
        stack = stack,
        onStack = onStack,
        facts = facts,
        onClose = onClose,
        onOpenElsewhere = { id -> when (id) { "help" -> onHelp(); "about" -> onAbout() } },
        onCoffee = onCoffee,
        initialFlash = settingsFlash,
        onFlashTaken = onFlashTaken,
        rowShown = shown,
        footer = { id ->
            when (id) {
                "screens" -> CardNote(
                    "A new screen is made by dragging an icon or a widget to the empty screen after the last one. The last screen goes away by itself when its last item is taken off. Hold an empty square on any other empty screen to remove it.",
                    Modifier.padding(horizontal = 4.dp),
                )
                "backup" -> if (s.lastBackupMillis > 0) {
                    val t = java.time.Instant.ofEpochMilli(s.lastBackupMillis).atZone(java.time.ZoneId.systemDefault())
                    CardNote("Last backup: " + t.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", java.util.Locale.getDefault())), Modifier.padding(horizontal = 4.dp))
                }
            }
        },
        rowContent = { _, label ->
            when (label) {
                // Home screen › Screens
                "Tiles behind the icons" -> {
                    SwitchRow(label, s.showTiles) { on ->
                        if (on) {
                            // The smaller grids need the tiles off: back on, they become 4×4.
                            val grid = com.glimpsemedia.glimpselauncher.model.gridAfterTilesOn(layout.columns, layout.rows)
                            if (grid != layout.columns to layout.rows) { state.change(layout.withGrid(grid.first, grid.second)); say("Tiles need 2×4 or 4×4: the grid is now 4×4.") }
                        }
                        state.change(s.copy(showTiles = on))
                    }
                    CardNote(if (s.showTiles) "A card behind each icon and its name." else "Icons and names sit on the wallpaper. The smaller grids can be used.")
                }
                "Grid" -> {
                    CardTitle("Grid")
                    val grids = com.glimpsemedia.glimpselauncher.model.GRIDS
                    for (line in listOf(grids.take(2), grids.drop(2))) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((c, r) in line) {
                                // The smaller grids show greyed while the tiles are on, and say why when touched.
                                val needsNoTiles = s.showTiles && (c to r) in com.glimpsemedia.glimpselauncher.model.TIGHT_GRIDS
                                BigButton(
                                    "$c × $r",
                                    { if (needsNoTiles) say("Switch off Tiles behind the icons to use the smaller grids.") else state.change(layout.withGrid(c, r)) },
                                    Modifier.weight(1f).alpha(if (needsNoTiles) 0.4f else 1f)
                                        .then(if (needsNoTiles) Modifier.semantics { contentDescription = "$c × $r, needs Tiles behind the icons off" } else Modifier),
                                    primary = layout.columns == c && layout.rows == r, fontSize = 16.sp,
                                )
                            }
                        }
                    }
                    if (s.showTiles) CardNote("Switch off Tiles behind the icons to use the smaller grids.")
                    CardNote("Icons and folders keep their order; widgets keep their size on screen.")
                }
                "Free layout: put icons in any square" -> {
                    SwitchRow(label, s.customLayout) { state.change(s.copy(customLayout = it)) }
                    CardNote("Off: icons always sit together at the top. On: you can leave gaps, for example an icon at the bottom or down one side.")
                }
                "Add apps to this screen" -> BigButton(label, { onClose(); onAddApps() })
                "Add widgets" -> BigButton(label, { onClose(); onAddWidgets() })
                "Add the All apps icon" -> BigButton(label, { state.change(layout.add(Item.AllApps)) })

                // Home screen › Search
                "Search icon" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, color = LocalMenu.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (layout.hasSearchIcon) CardNote("The Search tile is on your screens. Touch it to search.")
                    else {
                        CardNote("Add it from an empty place's menu, or here.")
                        BigButton("Add the Search icon", { state.change(layout.add(Item.Search)) })
                    }
                }
                "Hold All apps to search" -> {
                    SwitchRow(label, s.holdAllAppsToSearch) { state.change(s.copy(holdAllAppsToSearch = it)) }
                    CardNote("Holding the All apps button in the bottom bar, while locked, opens Search.")
                }
                "All files and folders" -> search?.allFiles?.takeIf { it.available }?.let { all ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        var asking by remember { mutableStateOf(false) }
                        // Set when the person went to Android's page for the permission; on coming back with it, the switch turns on.
                        var waiting by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
                        val permitted = remember(all.permissionVersion) { all.hasPermission() }
                        LaunchedEffect(all.permissionVersion) {
                            if (waiting && all.hasPermission()) { waiting = false; state.change(state.settings.copy(allFilesSearch = true)) }
                        }
                        SwitchRow(label, s.allFilesSearch) { on ->
                            when {
                                !on -> state.change(s.copy(allFilesSearch = false))
                                all.hasPermission() -> state.change(s.copy(allFilesSearch = true))
                                else -> asking = true
                            }
                        }
                        CardNote("Search the names of every file and folder on the phone. Names only, read on the phone, never sent anywhere.")
                        if (s.allFilesSearch && !permitted) {
                            CardNote("All files access is off for Glimpse Launcher Plus", strong = true)
                            BigButton("Open the setting", { all.request() })
                        }
                        if (asking) MenuDialog("All files access", onClose = { asking = false }, dismissible = false) {
                            Text(
                                allFilesAskText(all.usesSettingsPage),
                                color = LocalMenu.current.text, fontSize = 19.sp, lineHeight = 26.sp,
                            )
                            BigButton("Open the setting", { asking = false; waiting = true; all.request() }, primary = true)
                            BigButton("Cancel", { asking = false })
                        }
                    }
                }
                "Add folder location(s)" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRow(label, s.searchFolders) { state.change(s.copy(searchFolders = it)) }
                    val coveredByAllFiles = search?.allFiles?.let { it.available && s.allFilesSearch && remember(it.permissionVersion) { it.hasPermission() } } == true
                    if (s.searchFolders && coveredByAllFiles) CardNote("Not needed while All files and folders is on")
                    if (s.searchFolders && search != null) {
                        val folders = remember(search.foldersVersion) { search.store.folders() }
                        SearchFolderList(
                            folders, available = { search.isAvailable(it) },
                            onAdd = { search.pickFolder() },
                            onRemove = { f -> search.release(f); search.saveFolders(folders - f) },
                        )
                    } else CardNote("Off: files and folders are not searched.")
                }
                "Add search engine(s)" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwitchRow(label, s.searchEngines) { state.change(s.copy(searchEngines = it)) }
                    if (s.searchEngines && search != null) {
                        var engines by remember { mutableStateOf(search.store.engines()) }
                        SearchEngineList(engines) { engines = it; search.store.saveEngines(it) }
                    } else CardNote("Off: Search shows no web searches.")
                }
                "Change the wallpaper" -> BigButton(label, onWallpaper)
                "Full screen: hide the top bar" -> {
                    SwitchRow(label, s.fullScreen) { state.change(s.copy(fullScreen = it)) }
                    CardNote("Hides the bar with the time, battery and notifications. Swipe down from the top edge to see it for a moment.")
                }

                // Home screen › Bottom bar
                "Show in icon view" -> SwitchRow(label, s.showBar) { state.change(s.copy(showBar = it)) }
                "Show in text mode" -> SwitchRow(label, s.showBarText) { state.change(s.copy(showBarText = it)) }
                "Apps in the bar" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choices("Apps in the bar (All apps stays in the middle)", listOf(4 to "4: two each side", 6 to "6: three each side"), s.barSlots) { state.change(s.copy(barSlots = it)) }
                    CardNote("Left to right, as in the bar.")
                    val slots = s.barSlots
                    val hidden = layout.dockHidden(slots).toSet()
                    // The rows in bar order, with the All apps button in its place between the two sides.
                    val rows: List<String?> = layout.dock.take(layout.dockLeft) + listOf(null) + layout.dock.drop(layout.dockLeft)
                    for (key in rows) {
                        if (key == null) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalMenu.current.button).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("All apps button", color = LocalMenu.current.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(
                                    "Hold action", color = LocalMenu.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LocalMenu.current.surface)
                                        .clickable { onHoldAction(dockHoldKey(null), "All apps button") }.padding(horizontal = 12.dp, vertical = 9.dp),
                                )
                            }
                            continue
                        }
                        val app = state.apps[key]
                        val name = app?.label ?: key
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalMenu.current.background).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (app != null) AppIcon(app, 40.dp)
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(name, color = LocalMenu.current.text, fontSize = 17.sp)
                                    if (key in hidden) Text("Hidden while the bar shows 4", color = LocalMenu.current.dimText, fontSize = 14.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Icon buttons: arrows to move, a bin to take the app off the bar; each says what it does to screen readers.
                                @Composable fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, what: String, onClick: () -> Unit) = Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(LocalMenu.current.button)
                                        .clickable(onClickLabel = what, onClick = onClick),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(icon, contentDescription = what, tint = LocalMenu.current.text, modifier = Modifier.size(24.dp)) }
                                if (layout.canMoveInDock(key, -1, slots)) IconAction(Icons.AutoMirrored.Filled.ArrowBack, "Move $name left") { state.change(layout.dockMoved(key, -1, slots)) }
                                if (layout.canMoveInDock(key, +1, slots)) IconAction(Icons.AutoMirrored.Filled.ArrowForward, "Move $name right") { state.change(layout.dockMoved(key, +1, slots)) }
                                // The bottom bar has no tile menu while locked, so its hold actions are chosen here too.
                                Text(
                                    "Hold action", color = LocalMenu.current.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LocalMenu.current.button)
                                        .clickable { onHoldAction(dockHoldKey(key), name) }.padding(horizontal = 10.dp, vertical = 11.dp),
                                )
                                IconAction(Icons.Filled.Delete, "Remove $name from the bottom bar") { state.change(layout.removeFromDock(key)) }
                            }
                        }
                    }
                }
                "Add an app to the bottom bar" -> BigButton(label, onAddToBar)

                // Home screen › Clock
                "Show the time on the first screen" -> SwitchRow(label, s.showClock) { state.change(s.copy(showClock = it)) }
                "Show the date under the time" -> SwitchRow(label, s.showDate, enabled = s.showClock) { state.change(s.copy(showDate = it)) }
                "Clock style" -> Choices(label, listOf(CLOCK_DIGITAL to "Digital", CLOCK_SIDE_BY_SIDE to "Side by side", CLOCK_ANALOGUE to "Round"), s.clockStyle) { state.change(s.copy(clockStyle = it)) }
                "Clock size" -> {
                    Choices(label, listOf(CLOCK_SMALL to "Small", CLOCK_MEDIUM to "Medium", CLOCK_LARGE to "Large"), s.clockSize) { state.change(s.copy(clockSize = it)) }
                    CardNote("On a narrow screen the clock is made smaller by itself if it would not fit.")
                }
                "A background behind the clock" -> SwitchRow(label, s.clockBackground, enabled = s.showClock) { state.change(s.copy(clockBackground = it)) }
                "Show the clock" -> SwitchRow(label, s.textClock) { state.change(s.copy(textClock = it)) }
                "Size" -> Choices(label, listOf(0 to "Small", 1 to "Medium", 2 to "Large"), s.textClockSize) { state.change(s.copy(textClockSize = it)) }
                "Position" -> Choices(label, listOf(0 to "Left", 1 to "Middle", 2 to "Right"), s.textClockAlign) { state.change(s.copy(textClockAlign = it)) }

                // Home screen › Text mode
                "Use text mode" -> SwitchRow(label, s.textMode) { state.change(s.copy(textMode = it)) }
                "Text size" -> StepSlider(label, s.textStep, com.glimpsemedia.glimpselauncher.data.TEXT_STEPS) { state.change(s.copy(textStep = it)) }
                "List side" -> Choices(label, listOf(false to "Bottom left", true to "Bottom right"), s.textListRight) { state.change(s.copy(textListRight = it)) }
                "Text colour (names, folders and the clock)" -> ColorRow(label, s.textModeColor, "Automatic", 0) { picking = ColorTarget.TextModeText }
                "Apps and folders" -> TextModeEditor(state)

                // Colours and themes
                "Themes" -> ThemeChoices(
                    s, { state.change(s.withTheme(it)) }, state.savedThemes.list,
                    onPickSaved = { onSavedTheme(SavedThemeAction.Apply(it)) }, onManageSaved = { managingTheme = it },
                )
                "Save current theme" -> SaveThemeButton(state.savedThemes.list, onSavedTheme)
                "Theme wallpaper" -> BigButton(label, { choosingWallpaper = true })
                "Menus and standard colours" -> Choices(label, listOf(true to "Dark", false to "Light"), s.darkTheme) { state.change(s.copy(darkTheme = it)) }
                "Highlight colour (selections and touched icons)" -> ColorRow(label, s.highlightColor, "Standard yellow", 0) { picking = ColorTarget.Highlight }
                "Folder icon" -> Column {
                    SwitchRow(label, s.folderStyle == 1) { state.change(s.copy(folderStyle = if (it) 1 else 0)) }
                    CardNote(if (s.folderStyle == 1) "Folders show the picture chosen below." else "Off: folders show their apps' icons.")
                }
                "Folder picture" -> Column {
                    CardTitle(label)
                    FolderPictureChoices(s.folderIcon, Color(shaded(if (s.folderColor == 0) FOLDER_YELLOW else s.folderColor, s.folderShade)), s.folderStyle == 1) {
                        state.change(s.copy(folderIcon = it))
                    }
                    if (s.folderStyle != 1) CardNote("Used by the Folder icon look.")
                }
                "Folder colour" -> if (s.folderStyle == 1) ColorRow(label, s.folderColor, "Folder yellow", 0, Color(FOLDER_YELLOW)) { picking = ColorTarget.Folder }
                    else Column(Modifier.alpha(0.5f)) {
                        ColorRow(label, s.folderColor, "Folder yellow", 0, Color(FOLDER_YELLOW)) {}
                        CardNote("Used by the Folder icon look.")
                    }
                "Shapes preview" -> ShapesPreview(s.showTiles)
                "Icon pack" -> IconPackRow(label, s.iconPack) { state.change(s.copy(iconPack = it)) }
                "Tile shape" -> Column(Modifier.alpha(if (s.showTiles) 1f else 0.5f)) {
                    Choices(label, SHAPE_CHOICES, s.tileShape) { state.change(s.copy(tileShape = it)) }
                    if (!s.showTiles) CardNote("Tiles are switched off (Home screen › Screens).")
                }
                "Icon background shape" -> Column {
                    Choices(label, SHAPE_CHOICES, s.iconShape) { state.change(s.copy(iconShape = it)) }
                    if (LocalPalette.current.iconTransparency >= 100) CardNote("The icon background is hidden; give it a colour under Colours to see it.")
                }
                "Empty place shape" -> Column {
                    Choices(label, SHAPE_CHOICES, s.slotShape) { state.change(s.copy(slotShape = it)) }
                    CardNote("The outlined place with a + that shows while unlocked.")
                }
                "Tile colour" -> if (s.showTiles) ColorRow(label, s.tileColor, "Standard", s.tileTransparency) { picking = ColorTarget.Tiles }
                    else Column(Modifier.alpha(0.5f)) {
                        // No tiles, so nothing to colour: the colour is kept for when they come back.
                        ColorRow(label, s.tileColor, "Standard", s.tileTransparency) {}
                        CardNote("Tiles are switched off (Home screen › Screens).")
                    }
                "Icon background colour" -> ColorRow(label, s.iconColor, "Standard", s.iconTransparency) { picking = ColorTarget.IconSquare }
                "Text on the tiles and the bar" -> ColorRow(label, s.tileTextColor, "Automatic", 0) { picking = ColorTarget.TileText }
                "Names on the tiles" -> SwitchRow(label, s.showTileNames) { state.change(s.copy(showTileNames = it)) }
                "Names in the bottom bar" -> SwitchRow(label, s.showDockNames) { state.change(s.copy(showDockNames = it)) }
                "Menu text size" -> {
                    StepSlider(label, s.menuTextStep, com.glimpsemedia.glimpselauncher.data.MENU_TEXT_STEPS) { state.change(s.copy(menuTextStep = it)) }
                    CardNote("The size of the text in Settings, menus, Help and other pop-ups. Buttons move onto more lines when they no longer fit side by side.")
                }
                "Name size on the tiles" -> StepSlider(label, s.tileNameStep, com.glimpsemedia.glimpselauncher.data.NAME_STEPS) { state.change(s.copy(tileNameStep = it)) }
                "Icon size on the tiles" -> StepSlider(label, s.tileIconStep, com.glimpsemedia.glimpselauncher.data.ICON_STEPS) { state.change(s.copy(tileIconStep = it)) }
                "Mark tiles that have a hold action" -> SwitchRow(label, s.showHoldMark) { state.change(s.copy(showHoldMark = it)) }
                "Colour of the bottom bar" -> ColorRow(label, s.barColor, "Standard", s.barTransparency) { picking = ColorTarget.Bar }
                "Colour of the clock and date" -> ColorRow(label, s.clockColor, "White", s.clockTransparency) { picking = ColorTarget.Clock }
                "Colour behind the clock" -> ColorRow(label, s.clockBackColor, "Same as the bottom bar", s.clockBackTransparency) { picking = ColorTarget.ClockBack }

                // Safety and privacy › Locking and PIN
                "Lock again after no touching for" -> {
                    Choices(label, listOf(5 to "5 min", 10 to "10 min", 30 to "30 min", 0 to "Off"), s.autoLockMinutes) { state.change(s.copy(autoLockMinutes = it)) }
                    CardNote(
                        if (s.pinHash == null) "No PIN: holding the padlock for ${if (s.quickUnlock) "one second" else "three seconds"} unlocks."
                        else "After the ${if (s.quickUnlock) "one-second" else "three-second"} hold, the PIN is asked for. If it is forgotten, the phone's own screen lock lets you set a new one.",
                    )
                }
                "Quick unlock: hold the padlock for 1 second" -> {
                    SwitchRow(label, s.quickUnlock) { state.change(s.copy(quickUnlock = it)) }
                    CardNote("Usually 3 seconds, so a stray touch never unlocks. A PIN, if set, is still asked for.")
                }
                "Set a PIN" -> BigButton(label, { onPinStep(PinStep.NewFirst(unlockAfter = false)) })
                "Change the PIN" -> BigButton(label, { onPinStep(PinStep.Current(thenRemove = false)) })
                "Remove the PIN" -> BigButton(label, { onPinStep(PinStep.Current(thenRemove = true)) })
                "Notes need the PIN" -> {
                    SwitchRow(label, s.notesNeedPin, enabled = s.pinHash != null) { state.change(s.copy(notesNeedPin = it)) }
                    if (s.pinHash == null) CardNote("Set a PIN first.")
                }

                // Safety and privacy › Emergency button
                "Emergency button" -> {
                    SwitchRow(label, s.emergencyEnabled) {
                        state.change(s.copy(emergencyEnabled = it))
                        if (it) onEmergencySetup()
                    }
                    if (s.emergencyEnabled) CardNote(
                        "Touching the Emergency picture starts a ${s.emergencyCountdown} second countdown with a big Cancel button, then rings this number. Glimpse Launcher only starts the call: it never listens to it or records it.",
                    )
                }
                "Number to ring" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    val contact = s.emergencyContact
                    Column(Modifier.weight(1f)) {
                        Text("Number to ring", color = LocalMenu.current.dimText, fontSize = 14.sp)
                        Text(if (contact == null) "Not set" else "${contact.name}  ${contact.number}", color = LocalMenu.current.text, fontSize = 17.sp)
                    }
                    Text(
                        if (contact == null) "Add" else "Change", color = readableOn(LocalAccent.current), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(LocalAccent.current)
                            .clickable(onClickLabel = "Number to ring") { editContact = 0 }.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
                "Seconds before it rings" -> {
                    Choices(label, EMERGENCY_COUNTDOWN_CHOICES.map { it to "$it" }, s.emergencyCountdown) { state.change(s.copy(emergencyCountdown = it)) }
                    CardNote("20 is the usual: time to cancel if it was touched by mistake.")
                }
                "Add the Emergency icon to the screen" -> BigButton(label, { state.change(layout.add(Item.Emergency)) }, primary = true)

                // Safety and privacy › Notification numbers
                "Show a number on icons with new messages" -> {
                    SwitchRow(label, badgesOn) {
                        ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    CardNote("Opens the phone's Notification access list. Switch Glimpse Launcher on there to see a number, such as 3 new messages, on an icon; switch it off to stop. Only the count is used. Nothing you are sent is read, kept or shared.")
                }

                // Your setup
                "Back up to a file" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardNote(
                        "Save your whole setup (settings, screens, text mode, notes with their pictures, and saved themes) to one file, " +
                            "and put it back later on this phone or a new one. Widgets are not included.",
                    )
                    if (s.pinHash != null) CardNote("This backup includes your PIN. Keep the file private.", strong = true)
                    BigButton(label, onBackup, primary = true)
                }
                "Restore from a file" -> BigButton(label, onRestorePick)
                "Clear all apps from the screens" -> BigButton(label, { confirmReset = 1 })
                "Add all apps to the screens" -> BigButton(label, { confirmReset = 2 })
                "Show the welcome again" -> BigButton(label, { state.change(s.copy(welcomeShown = false, welcomePage = 0)); onClose() })
                "Set as home screen" -> if (state.isDefaultHome) CardNote("Glimpse Launcher is your home screen already.")
                    else Column {
                        BigButton(label, onMakeHome, primary = true)
                        CardNote("The phone will ask you to confirm.")
                    }

                // Help and support › Report bugs
                "Report a bug by email" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardNote(
                        "Found something that doesn't work, or have an idea? Write to $SUPPORT_EMAIL or visit $WEBSITE/glimpse. " +
                            "In a bug report, say what you were doing, what you expected, what happened instead, and your phone model.",
                    )
                    BigButton(label, onReportBug, primary = true)
                }
                "Suggest a feature" -> BigButton(label, onSuggest)
                "Visit the website" -> BigButton(label, onWebsite)
                "Bug log" -> {
                    SwitchRow(label, s.bugLog) { state.change(s.copy(bugLog = it)) }
                    CardNote(
                        "If something goes wrong, switch the bug log on, use Glimpse Launcher until it happens again, then email the log. " +
                            "It records crashes and errors only (when, where, the version and phone model), stays on this phone, and is sent only when you email it.",
                    )
                }
                "Email the bug log" -> BigButton(label, onEmailBugLog)
                "Clear the bug log" -> BigButton(label, onClearBugLog)
            }
        },
    )
    // The colour picker covers the page without being a dialog: Back closes it, not the page under it.
    BackHandler(enabled = picking != null) { picking = null }
    picking?.let { target ->
        val (title, current, standard) = when (target) {
            ColorTarget.Tiles -> Triple("Tile colour", s.tileColor, "Standard")
            ColorTarget.Bar -> Triple("Colour of the bottom bar", s.barColor, "Standard")
            ColorTarget.Clock -> Triple("Colour of the clock and date", s.clockColor, "White")
            ColorTarget.ClockBack -> Triple("Colour behind the clock", s.clockBackColor, "Same as the bottom bar")
            ColorTarget.Highlight -> Triple("Highlight colour", s.highlightColor, "Standard yellow")
            ColorTarget.IconSquare -> Triple("Icon background colour", s.iconColor, "Standard")
            ColorTarget.TileText -> Triple("Text on the tiles and the bar", s.tileTextColor, "Automatic")
            ColorTarget.TextModeText -> Triple("Text mode text and clock", s.textModeColor, "Automatic")
            ColorTarget.Folder -> Triple("Colour of the folders", s.folderColor, "Folder yellow")
        }
        val see = when (target) {
            ColorTarget.Tiles -> s.tileTransparency
            ColorTarget.Bar -> s.barTransparency
            ColorTarget.Clock -> s.clockTransparency
            ColorTarget.ClockBack -> s.clockBackTransparency
            ColorTarget.Highlight -> 0
            ColorTarget.IconSquare -> s.iconTransparency
            ColorTarget.TileText, ColorTarget.TextModeText, ColorTarget.Folder -> 0
        }
        val shade = when (target) {
            ColorTarget.Tiles -> s.tileShade
            ColorTarget.Bar -> s.barShade
            ColorTarget.Clock -> s.clockShade
            ColorTarget.ClockBack -> s.clockBackShade
            ColorTarget.Highlight -> s.highlightShade
            ColorTarget.IconSquare -> s.iconShade
            ColorTarget.TileText -> s.tileTextShade
            ColorTarget.TextModeText -> s.textModeShade
            ColorTarget.Folder -> s.folderShade
        }
        // What the colour looks like now, shade included (the standard colours are resolved by the palette).
        val palette = LocalPalette.current
        val clockPanel = shaded((if (s.clockBackColor == 0) palette.bar else Color(s.clockBackColor)).copy(alpha = 1f).toArgb(), s.clockBackShade)
        val sample = when (target) {
            ColorTarget.Tiles -> palette.tile
            ColorTarget.Bar -> palette.bar.copy(alpha = 1f)
            ColorTarget.IconSquare -> palette.icon
            ColorTarget.Highlight -> LocalAccent.current
            ColorTarget.Clock -> Color(clockInk(s.clockColor, s.clockShade, s.clockBackground, clockPanel))
            ColorTarget.ClockBack -> Color(clockPanel)
            ColorTarget.TileText -> palette.tileText
            ColorTarget.TextModeText -> Color(textInk(s.textModeColor, s.textModeShade, s.darkTheme))
            ColorTarget.Folder -> Color(shaded(if (s.folderColor == 0) FOLDER_YELLOW else s.folderColor, s.folderShade))
        }
        // The highlight is always solid: a see-through one could hide the buttons.
        ColourPicker(title, current, standard, extra = ({
            ShadeRow(shade, sample) { percent ->
                state.change(when (target) {
                    ColorTarget.Tiles -> s.copy(tileShade = percent)
                    ColorTarget.Bar -> s.copy(barShade = percent)
                    ColorTarget.Clock -> s.copy(clockShade = percent)
                    ColorTarget.ClockBack -> s.copy(clockBackShade = percent)
                    ColorTarget.Highlight -> s.copy(highlightShade = percent)
                    ColorTarget.IconSquare -> s.copy(iconShade = percent)
                    ColorTarget.TileText -> s.copy(tileTextShade = percent)
                    ColorTarget.TextModeText -> s.copy(textModeShade = percent)
                    ColorTarget.Folder -> s.copy(folderShade = percent)
                })
            }
            // Text is always solid, like the highlight.
            if (target != ColorTarget.Highlight && target != ColorTarget.TileText && target != ColorTarget.TextModeText && target != ColorTarget.Folder) TransparencyRow(if (target == ColorTarget.IconSquare) "Transparency (100% hides the squares)" else "Transparency", see, null) { percent ->
                state.change(when (target) {
                    ColorTarget.Tiles -> s.copy(tileTransparency = percent!!)
                    ColorTarget.Bar -> s.copy(barTransparency = percent!!)
                    ColorTarget.Clock -> s.copy(clockTransparency = percent!!)
                    ColorTarget.ClockBack -> s.copy(clockBackTransparency = percent!!)
                    ColorTarget.Highlight -> s
                    ColorTarget.IconSquare -> s.copy(iconTransparency = percent!!)
                    ColorTarget.TileText, ColorTarget.TextModeText, ColorTarget.Folder -> s
                })
            }
        }), onPick = { argb ->
            state.change(when (target) {
                ColorTarget.Tiles -> s.copy(tileColor = argb)
                ColorTarget.Bar -> s.copy(barColor = argb)
                ColorTarget.Clock -> s.copy(clockColor = argb)
                ColorTarget.ClockBack -> s.copy(clockBackColor = argb, clockBackground = true)  // choosing a colour turns the background on
                ColorTarget.Highlight -> s.copy(highlightColor = argb)
                // Choosing a colour for squares that are still hidden shows them, half see-through to start with.
                ColorTarget.IconSquare -> s.copy(iconColor = argb, iconTransparency = if (s.iconTransparency == 100) 50 else s.iconTransparency)
                ColorTarget.TileText -> s.copy(tileTextColor = argb)
                ColorTarget.TextModeText -> s.copy(textModeColor = argb)
                ColorTarget.Folder -> s.copy(folderColor = argb)
            })
        }, onClose = { picking = null })
    }
    editContact?.let { _ ->
        val existing = s.emergencyContact
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var number by remember { mutableStateOf(existing?.number ?: "") }
        MenuDialog(title = "Emergency number", onClose = { editContact = null }) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, label = { Text("Name") },
                textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = number, onValueChange = { number = it.take(20) }, singleLine = true, label = { Text("Phone number") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(), modifier = Modifier.fillMaxWidth())
            BigButton("Save", {
                val saved = Contact(name.trim().ifEmpty { "Emergency" }, number.trim())
                if (saved.isUsable) state.change(s.copy(emergencyContact = saved))
                editContact = null
            }, primary = true)
            if (existing != null) {
                BigButton("Remove this number", { state.change(s.copy(emergencyContact = null)); editContact = null })
            }
        }
    }
    if (confirmReset != 0) {
        MenuDialog(title = if (confirmReset == 1) "Clear all apps from the screens?" else "Add all apps to the screens?", onClose = { confirmReset = 0 }) {
            Text(
                if (confirmReset == 1) "This takes every app off the screens, so they are empty and ready for apps, and puts the usual apps back in the bottom bar. The apps stay on the phone."
                else "This puts every app on the screens in A-to-Z order, and puts the usual apps back in the bottom bar.",
                color = LocalMenu.current.text, fontSize = 18.sp,
            )
            BigButton(if (confirmReset == 1) "Yes, clear the screens" else "Yes, add all apps", {
                state.layout.widgets().forEach { widgets.delete(it.id) }  // the widgets go too
                if (confirmReset == 1) state.resetLayout() else state.resetLayoutFilled()
                confirmReset = 0
            }, primary = true)
        }
    }
}

/** 0, 25, 50, 75 and 100 percent; with [sameName] given, a first button goes back to following the setting. */
/** Darker or lighter, −50…50 in steps of 5, with a sample of the result and Reset. */
@Composable
private fun ShadeRow(percent: Int, sample: Color, onChange: (Int) -> Unit) {
    val menu = LocalMenu.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Darker or lighter", color = menu.text, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(sample).border(1.dp, menu.dimText, RoundedCornerShape(10.dp)))
        }
        androidx.compose.material3.Slider(
            value = percent.toFloat(),
            onValueChange = { v -> val p = (Math.round(v / SHADE_STEP) * SHADE_STEP).coerceIn(SHADE_MIN, SHADE_MAX); if (p != percent) onChange(p) },
            valueRange = SHADE_MIN.toFloat()..SHADE_MAX.toFloat(),
            steps = (SHADE_MAX - SHADE_MIN) / SHADE_STEP - 1,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = LocalAccent.current, activeTrackColor = LocalAccent.current),
            modifier = Modifier.fillMaxWidth().semantics { this.contentDescription = "Darker or lighter: ${if (percent == 0) "as picked" else if (percent > 0) "$percent% lighter" else "${-percent}% darker"}" },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Darker", color = menu.dimText, fontSize = 16.sp)
            Text(
                if (percent == 0) "As picked" else if (percent > 0) "$percent% lighter" else "${-percent}% darker",
                color = menu.text, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            Text("Lighter", color = menu.dimText, fontSize = 16.sp)
        }
        if (percent != 0) BigButton("Reset", { onChange(0) }, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TransparencyRow(title: String, current: Int?, sameName: String?, onPick: (Int?) -> Unit) {
    val menu = LocalMenu.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(title, color = menu.text, fontSize = 20.sp)
        if (sameName != null) {
            BigButton(if (current == null) "$sameName (chosen)" else sameName, { onPick(null) }, Modifier.padding(top = 8.dp), primary = current == null)
        }
        val percents = listOf(0, 25, 50, 75, 100)
        ButtonRow(percents.size, minButtonWidth(listOf("100%"), 16f).dp, Modifier.padding(top = 8.dp), gap = 6.dp) { i, m ->
            BigButton("${percents[i]}%", { onPick(percents[i]) }, m, primary = current == percents[i], fontSize = 16.sp)
        }
    }
}

private enum class ColorTarget { Tiles, Bar, Clock, ClockBack, Highlight, IconSquare, TileText, TextModeText, Folder }

/** A row: what is being coloured, the colour now, and a button to choose another. */
@Composable
private fun ColorRow(title: String, current: Int, standardName: String, transparency: Int, standardColor: Color? = null, onChoose: () -> Unit) {
    val menu = LocalMenu.current
    val inCard = LocalInCard.current
    NameAndButton(
        Modifier.then(if (inCard) Modifier.padding(vertical = 2.dp) else Modifier.clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(12.dp)),
        start = {
        Box(
            Modifier.size(if (inCard) 36.dp else 44.dp).clip(CircleShape).background(if (current == 0) standardColor ?: menu.button else Color(current))
                .border(2.dp, menu.dimText, CircleShape),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(title, color = menu.text, fontSize = if (inCard) 17.sp else 20.sp)
            val note = listOfNotNull(standardName.takeIf { current == 0 }, "$transparency% transparent".takeIf { transparency > 0 }).joinToString(", ")
            if (note.isNotEmpty()) Text(note, color = menu.dimText, fontSize = if (inCard) 14.sp else 16.sp)
        }
        },
    ) {
        Text(
            "Change", color = readableOn(LocalAccent.current), fontSize = if (inCard) 16.sp else 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(LocalAccent.current)
                .clickable(onClickLabel = "Change: $title", onClick = onChoose).padding(horizontal = if (inCard) 14.dp else 18.dp, vertical = if (inCard) 9.dp else 10.dp),
        )
    }
}

/** The colour chart: the standard colour, white and greys, then 18 hues from dark to light. */
@Composable
internal fun ColourPicker(
    title: String,
    current: Int,
    standardName: String,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) = MenuText {
    val menu = LocalMenu.current
    @Composable
    fun RowScope.Swatch(argb: Int, name: String, size: androidx.compose.ui.unit.Dp) {
        val chosen = argb == current
        // Each colour has an equal share of the row, at most [size] wide, so a row always fits a narrow screen.
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Box(
            Modifier.widthIn(max = size).fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(Color(argb))
                .border(if (chosen) 4.dp else 1.dp, if (chosen) LocalAccent.current else menu.dimText, CircleShape)
                .clickable(onClickLabel = "Use $name") { onPick(argb) },
            contentAlignment = Alignment.Center,
        ) {
            if (chosen) Icon(Icons.Filled.Check, contentDescription = "$name, chosen",
                tint = if (Color(argb).luminance() > 0.45f) Color.Black else Color.White, modifier = Modifier.fillMaxSize(0.6f))
        } }
    }
    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = menu.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "Done", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (extra != null) item { extra() }
            item { BigButton(if (current == 0) "$standardName (chosen)" else standardName, { onPick(0) }, primary = current == 0) }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((name, argb) in GreyChoices) Swatch(argb, name, 52.dp)
                }
            }
            items(ColourGrid.size) { hue ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((shade, argb) in ColourGrid[hue].withIndex()) Swatch(argb, "colour ${hue + 1}, shade ${shade + 1}", 44.dp)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    // Inside a Settings card the card is the background, and the text is a little smaller.
    val inCard = LocalInCard.current
    Row(
        Modifier.fillMaxWidth()
            .then(if (inCard) Modifier else Modifier.clip(RoundedCornerShape(18.dp)).background(LocalMenu.current.surface))
            .then(if (enabled) Modifier.clickable(onClickLabel = label) { onChange(!checked) } else Modifier)
            .padding(horizontal = if (inCard) 0.dp else 16.dp, vertical = if (inCard) 2.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (enabled) LocalMenu.current.text else LocalMenu.current.dimText, fontSize = if (inCard) 17.sp else 20.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current, checkedThumbColor = readableOn(LocalAccent.current)),
        )
    }
}

/** A folder opened: its apps as big tiles. Unlocked, each has a menu to take it out of the folder. */
@Composable
private fun FolderPanel(
    folder: Item.Folder,
    apps: Map<String, AppInfo>,
    unlocked: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onTakeOut: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onRename: () -> Unit,
    onClose: () -> Unit,
    showNames: Boolean = true,
    /** Unlocked: opens the list of apps to put in this folder. */
    onAddApps: () -> Unit = {},
    // An open folder sits on the menu background, not the wallpaper: its apps always keep their tiles, so the
    // names stay readable with the tiles switched off (white names would vanish on the light menus).
) = androidx.compose.runtime.CompositionLocalProvider(LocalShowTiles provides true) {
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    // Unlocked, holding an app and dragging it moves it to another place in the folder.
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragBy by remember { mutableStateOf(Offset.Zero) }
    val placed = remember { HashMap<String, Rect>() }
    val haptic = LocalHapticFeedback.current
    val order = folder.apps.filter { apps[it] != null }
    val latestMove by rememberUpdatedState(onMove)
    fun dropAt(key: String) {
        val from = placed[key] ?: return
        val center = from.center + dragBy
        // The place under the finger, else the nearest one.
        val target = order.minByOrNull { k -> placed[k]?.let { r -> if (r.contains(center)) -1f else (r.center - center).getDistance() } ?: Float.MAX_VALUE }
        if (target != null && target != key) latestMove(key, order.indexOf(target))
    }
    Column(Modifier.fillMaxSize().background(LocalMenu.current.background).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(folder.name, color = LocalMenu.current.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (unlocked) {
                Text(
                    "Name", color = LocalMenu.current.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 10.dp).clip(RoundedCornerShape(20.dp)).background(LocalMenu.current.button)
                        .clickable(onClickLabel = "Change the folder's name", onClick = onRename).padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            Text(
                "Close", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        if (unlocked) {
            BigButton(
                if (folder.apps.size >= FOLDER_MAX) "This folder is full ($FOLDER_MAX apps)" else "Add apps",
                { if (folder.apps.size < FOLDER_MAX) onAddApps() }, Modifier.padding(horizontal = 16.dp), primary = folder.apps.size < FOLDER_MAX,
            )
        }
        if (unlocked && order.size > 1) {
            Text("Hold an app and drag it to change the order.", color = LocalMenu.current.dimText, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            userScrollEnabled = dragKey == null,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            gridItems(folder.apps.mapNotNull { apps[it] }, key = { it.key }) { app ->
                val dragging = dragKey == app.key
                Box(
                    Modifier.fillMaxWidth().size(width = 150.dp, height = 150.dp)
                        .onGloballyPositioned { placed[app.key] = it.boundsInRoot() }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            if (dragging) { translationX = dragBy.x; translationY = dragBy.y; scaleX = 1.06f; scaleY = 1.06f }
                        }
                        .then(
                            if (unlocked && order.size > 1) Modifier.pointerInput(app.key, order) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragKey = app.key; dragBy = Offset.Zero; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    onDrag = { change, amount -> change.consume(); dragBy += amount },
                                    onDragEnd = { dropAt(app.key); dragKey = null; dragBy = Offset.Zero },
                                    onDragCancel = { dragKey = null; dragBy = Offset.Zero },
                                )
                            } else Modifier,
                        ),
                ) {
                    BigTile(app.label, { onLaunch(app) }, Modifier.fillMaxSize(), showName = showNames) { AppIcon(app, null) }
                    if (unlocked) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(48.dp).clip(CircleShape).background(LocalAccent.current)
                                .clickable(onClickLabel = "Menu") { menuFor = app },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for ${app.label}", tint = readableOn(LocalAccent.current), modifier = Modifier.size(30.dp)) }
                    }
                }
            }
        }
    }
    menuFor?.let { app ->
        MenuDialog(title = app.label, onClose = { menuFor = null }) {
            val at = order.indexOf(app.key)
            if (at > 0) BigButton("Move earlier", { onMove(app.key, at - 1); menuFor = null })
            if (at in 0 until order.size - 1) BigButton("Move later", { onMove(app.key, at + 1); menuFor = null })
            BigButton("Take it out of the folder", { onTakeOut(app.key); menuFor = null; onClose() }, primary = true)
            val changeIcon = LocalChangeIcon.current
            BigButton("Change the icon", { changeIcon(IconRequest(app.key, app.label, app)); menuFor = null })
            BigButton("App info (to uninstall)", { onAppInfo(app); menuFor = null })
        }
    }
}

/** Four friendly pages shown once, and again from Settings. */
/** Every widget the phone offers, A to Z; picking one starts adding it. */
@Composable
private fun WidgetPicker(choices: List<WidgetChoice>, onPick: (WidgetChoice) -> Unit, onClose: () -> Unit, suggested: (WidgetChoice) -> Pair<Int, Int>) = MenuText {
    val menu = LocalMenu.current
    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Add a widget", color = menu.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                "Close", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        Text("A widget shows live information on the screen, such as the weather. Touch one to add it.", color = menu.dimText, fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        var query by remember { mutableStateOf("") }
        OutlinedTextField(
            value = query, onValueChange = { query = it.take(40) }, singleLine = true,
            placeholder = { Text("Search widgets", fontSize = 20.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = menu.dimText, modifier = Modifier.size(30.dp)) },
            textStyle = TextStyle(fontSize = 22.sp), colors = menuFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        val shown = choices.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) || it.appLabel.contains(query.trim(), ignoreCase = true) }
        if (choices.isEmpty()) Text("This phone has no widgets to add.", color = menu.text, fontSize = 20.sp, modifier = Modifier.padding(24.dp))
        else if (shown.isEmpty()) Text("No widget matches “${query.trim()}”.", color = menu.text, fontSize = 20.sp, modifier = Modifier.padding(24.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.provider.flattenToString() }) { choice ->
                val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, choice.provider) {
                    value = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            // Keep the picture's own proportions (a search bar is wide, not square)
                            val d = choice.icon
                            if (d == null) null else {
                                val w = d.intrinsicWidth.coerceIn(1, 512)
                                val h = d.intrinsicHeight.coerceIn(1, 512)
                                val scale = 256f / maxOf(w, h)
                                d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                            }
                        } catch (_: Exception) { null }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(menu.surface)
                        .clickable(onClickLabel = "Add ${choice.label}") { onPick(choice) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val image = bitmap
                    if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.size(64.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                    else Box(Modifier.size(64.dp))
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(choice.label, color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        val (c, r) = suggested(choice)
                        Text(
                            (if (choice.appLabel.isNotEmpty()) choice.appLabel + " · " else "") + "Starts at $c × $r squares",
                            color = menu.dimText, fontSize = 16.sp,
                        )
                    }
                    Icon(Icons.Filled.Add, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

/** The four ready-made looks as a grid of buttons; the one in use is marked. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ThemeChoices(
    current: com.glimpsemedia.glimpselauncher.data.Settings,
    onPick: (ThemePreset) -> Unit,
    saved: List<com.glimpsemedia.glimpselauncher.data.SavedTheme> = emptyList(),
    onPickSaved: (com.glimpsemedia.glimpselauncher.data.SavedTheme) -> Unit = {},
    /** Holding a saved theme (unlocked, in Settings): its menu. */
    onManageSaved: ((com.glimpsemedia.glimpselauncher.data.SavedTheme) -> Unit)? = null,
) {
    @Composable
    fun Card(name: String, dots: List<Int>, chosen: Boolean, onTap: () -> Unit, onHold: (() -> Unit)?, modifier: Modifier) {
        Column(
            modifier.clip(RoundedCornerShape(18.dp))
                .background(if (chosen) LocalAccent.current else LocalMenu.current.button)
                .combinedClickable(onClickLabel = "Use the $name theme", onClick = onTap, onLongClickLabel = if (onHold != null) "Theme menu" else null, onLongClick = onHold)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (c in dots) Box(Modifier.size(24.dp).clip(CircleShape).background(Color(c)).border(1.dp, Color.Gray, CircleShape))
            }
            val ink = if (chosen) readableOn(LocalAccent.current) else LocalMenu.current.text
            Text(name, color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((heading, dark) in listOf("Light themes" to false, "Dark themes" to true)) {
            Text(heading, color = LocalMenu.current.dimText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            for (row in THEME_PRESETS.filter { it.dark == dark }.chunked(2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in row) Card(p.name, listOf(p.c1, p.c2, p.c3, p.c4), current.isTheme(p), { onPick(p) }, null, Modifier.weight(1f))
                }
            }
        }
        if (saved.isNotEmpty()) {
            Text("My themes", color = LocalMenu.current.dimText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            for (row in saved.chunked(2)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in row) Card(t.name, t.cardColours(), current.look() == t.look, { onPickSaved(t) }, onManageSaved?.let { m -> { m(t) } }, Modifier.weight(1f))
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/** The first-run guide, and what "Show the welcome again" opens: how to use it, how to unlock, and a few choices. */
@Composable
private fun WelcomeScreen(
    state: LauncherState, onMakeHome: () -> Unit, onDone: () -> Unit, onAddApps: () -> Unit,
    onThemeWallpaper: (c1: Int, c2: Int, c3: Int, c4: Int, dark: Boolean) -> Unit,
    onSavedTheme: (SavedThemeAction) -> Unit,
) {
    // The theme tapped on the welcome page, so its matching wallpaper can be offered.
    var pickedTheme by remember { mutableStateOf<ThemePreset?>(null) }
    val s = state.settings
    val titles = listOf("Welcome to Glimpse Launcher", "The padlock", "Grid and look", "Swipes", "Your home screen")
    val last = titles.size - 1
    // The page is saved: making this the home screen restarts the app, and the welcome carries on from here.
    val page = s.welcomePage.coerceIn(0, last)
    fun goTo(p: Int) = state.change(state.settings.copy(welcomePage = p.coerceIn(0, last)))
    Column(
        Modifier.fillMaxSize().background(LocalMenu.current.welcome).topSafePadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(titles[page], color = LocalMenu.current.text, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            val body = LocalMenu.current.text
            when (page) {
                0 -> Text("A fast, tidy home screen you can make your own.\n\nTouch a tile to open its app, and swipe sideways for more screens. The round button at the bottom lists every app.", color = body, fontSize = 22.sp, lineHeight = 32.sp)
                1 -> {
                    Text("The layout stays locked, so nothing moves by accident.", color = body, fontSize = 22.sp, lineHeight = 32.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(104.dp).clip(CircleShape).background(LocalMenu.current.surface), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { 0.75f }, modifier = Modifier.size(96.dp), color = LocalAccent.current, strokeWidth = 6.dp)
                            Icon(Icons.Filled.Lock, contentDescription = "The padlock", tint = LocalMenu.current.text, modifier = Modifier.size(52.dp))
                        }
                    }
                    Text("Hold the padlock at the bottom right for three seconds to unlock. A Settings cog appears at the bottom left; tap the padlock to lock again.\n\nWant more safety? Set a PIN in Settings > Safety and privacy > Locking and PIN: it is asked for after the hold.", color = body, fontSize = 20.sp, lineHeight = 30.sp)
                }
                2 -> {
                    Text("Eight big tiles a screen, or sixteen smaller ones. You can change this, and everything else here, in Settings.", color = body, fontSize = 20.sp, lineHeight = 30.sp)
                    val columns = com.glimpsemedia.glimpselauncher.model.GRID_COLUMNS
                    ButtonRow(columns.size, minButtonWidth(listOf("4 × 4"), 20f).dp) { i, m ->
                        val n = columns[i]
                        BigButton("$n × 4", { state.change(state.layout.withColumns(n)) }, m, primary = state.layout.columns == n)
                    }
                    ButtonRow(2, minButtonWidth(listOf("Light"), 20f).dp) { i, m ->
                        if (i == 0) BigButton("Dark", { state.change(s.copy(darkTheme = true)) }, m, primary = s.darkTheme)
                        else BigButton("Light", { state.change(s.copy(darkTheme = false)) }, m, primary = !s.darkTheme)
                    }
                    ThemeChoices(
                        s, { state.change(s.withTheme(it)); pickedTheme = it }, state.savedThemes.list,
                        onPickSaved = { onSavedTheme(SavedThemeAction.Apply(it)); pickedTheme = null },
                    )
                    pickedTheme?.let { p ->
                        BigButton("Use the matching wallpaper", { onThemeWallpaper(p.c1, p.c2, p.c3, p.c4, p.dark) })
                    }
                    SwitchRow("Names on the tiles", s.showTileNames) { state.change(s.copy(showTileNames = it)) }
                    SwitchRow("Names in the bottom bar", s.showDockNames) { state.change(s.copy(showDockNames = it)) }
                }
                3 -> Text("Swipe down on the home screen for your notifications.\n\nSwipe up for All apps.\n\nSwipe sideways to turn the screens.\n\nSwipe right on the first screen for your notes.", color = body, fontSize = 22.sp, lineHeight = 32.sp)
                else -> {
                    Text("The first screen starts empty. Add the apps you want; the bottom bar is already filled.\n\nLocked, a tile with a lightning mark does something extra when you hold it.", color = body, fontSize = 22.sp, lineHeight = 32.sp)
                    BigButton("Add apps to the screen", onAddApps)
                    if (state.layout.hasSearchIcon) Text("Search icon added", color = body, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))
                    else BigButton("Add the Search icon", { state.change(state.layout.addToPage(0, Item.Search) ?: state.layout.add(Item.Search)) })
                    Text(
                        if (state.isDefaultHome) "Glimpse Launcher is already your home screen. You are all set."
                        else "To use it every time you press Home, make it your home screen. The phone will ask you to confirm.",
                        color = body, fontSize = 22.sp, lineHeight = 32.sp,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            PageDots(titles.size, page)
            if (page == last) {
                if (!state.isDefaultHome) BigButton("Make Glimpse Launcher my home screen", { onMakeHome() }, primary = true)
                BigButton(if (state.isDefaultHome) "Start" else "Not now", onDone, primary = state.isDefaultHome)
                BigButton("Back", { goTo(page - 1) })
            } else {
                BigButton("Next", { goTo(page + 1) }, primary = true)
                BigButton(if (page == 0) "Skip" else "Back", { if (page == 0) onDone() else goTo(page - 1) })
            }
        }
    }
}

/** After touching the Emergency picture: a countdown with a big Cancel. The call is made when it reaches 0. */
@Composable
private fun EmergencyOverlay(ui: EmergencyUi, contact: Contact?, onCallNow: () -> Unit, onCancel: () -> Unit) {
    if (ui !is EmergencyUi.Countdown) return
    // Only the Cancel button ends it: a stray Back or touch outside must not cancel a call for help.
    MenuDialog(title = "Calling ${contact?.name ?: "for help"} in", onClose = onCancel, dismissible = false) {
        Text(
            "${ui.secondsLeft}", color = LocalAccent.current, fontSize = 88.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            "seconds. The phone will ring ${contact?.number ?: "the emergency number"}.",
            color = LocalMenu.current.text, fontSize = 20.sp, textAlign = TextAlign.Center,
        )
        BigButton("Cancel the call", onCancel, primary = true)
        BigButton("Call now", onCallNow)
    }
}

/** A hold-action call counting down. Unlike the Emergency button, Back or a touch outside cancels it: these are the accidental ones. */
@Composable
private fun HoldCallOverlay(ui: HoldCallUi?, onCallNow: () -> Unit, onCancel: () -> Unit) {
    if (ui == null) return
    MenuDialog(title = "Calling ${ui.number} in", onClose = onCancel) {
        Text(
            "${ui.secondsLeft}", color = LocalAccent.current, fontSize = 88.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        BigButton("Cancel", onCancel, primary = true)
        BigButton("Call now", onCallNow)
    }
}

/** Theme wallpaper: "My colours now", then the eight themes, each with a small painted sample. */
@Composable
private fun ThemeWallpaperList(
    s: com.glimpsemedia.glimpselauncher.data.Settings,
    onPick: (colours: List<Int>, dark: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val menu = LocalMenu.current
    val entries = listOf(Triple("My colours now", s.currentThemeColours(), s.darkTheme)) +
        THEME_PRESETS.map { Triple(it.name, listOf(it.c1, it.c2, it.c3, it.c4), it.dark) }
    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Theme wallpaper", color = menu.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "Done", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entries.size) { i ->
                val (name, colours, dark) = entries[i]
                val sample = remember(colours, dark) {
                    drawThemeWallpaper(wallpaperPlan(216, 468, colours[0], colours[1], colours[2], colours[3], dark)).asImageBitmap()
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface)
                        .clickable(onClickLabel = "Use the $name wallpaper") { onPick(colours, dark) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.Image(sample, contentDescription = null, modifier = Modifier.size(72.dp, 156.dp).clip(RoundedCornerShape(10.dp)))
                    Text(name, color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp).weight(1f))
                }
            }
        }
    }
}

/** The Settings pages to open for a search result: its group, then its page. */
private fun settingsStackFor(hit: SettingsHit): List<String> = listOf("g:${SettingsMap.groupOf(hit.section).id}", "s:${hit.section}")
