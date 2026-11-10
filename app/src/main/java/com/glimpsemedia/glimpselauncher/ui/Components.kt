package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.shaded
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.composed
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.glimpsemedia.glimpselauncher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The phone's touch timings with the long press at half a second: the hold that runs a tile's hold action. */
private class HoldTiming(base: androidx.compose.ui.platform.ViewConfiguration) : androidx.compose.ui.platform.ViewConfiguration by base {
    override val longPressTimeoutMillis: Long = holdTimeoutMs(base.longPressTimeoutMillis)
}

/** How long a locked tile is held to run its hold action. */
const val HOLD_ACTION_MS = 500L

/** The long-press time for hold actions, given the phone's own touch-and-hold delay. */
fun holdTimeoutMs(phoneMs: Long): Long = maxOf(HOLD_ACTION_MS, phoneMs)  // a longer accessibility delay wins

/** Gives [content] the half-second long press. */
@Composable
private fun HoldTimed(content: @Composable () -> Unit) {
    val base = androidx.compose.ui.platform.LocalViewConfiguration.current
    val timed = remember(base) { HoldTiming(base) }
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalViewConfiguration provides timed, content = content)
}

/** A tap does [onClick]; with [onHold], holding buzzes and does that too. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Modifier.tapOrHold(
    touch: androidx.compose.foundation.interaction.MutableInteractionSource,
    clickLabel: String,
    onClick: () -> Unit,
    onHold: (() -> Unit)?,
): Modifier {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    return if (onHold == null) clickable(interactionSource = touch, indication = null, onClickLabel = clickLabel, onClick = onClick)
    else combinedClickable(
        interactionSource = touch, indication = null, onClickLabel = clickLabel, onLongClickLabel = "Hold action",
        onLongClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onHold() },
        onClick = onClick,
    )
}

/** An app's own icon, loaded off the main thread. */
@Composable
fun AppIcon(app: AppInfo, size: Dp?, modifier: Modifier = Modifier) {
    // With no size the icon fills what it is given (see BigTile), keeping its shape.
    val box = if (size != null) modifier.size(size) else modifier.fillMaxSize()
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.key) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getActivityIcon(app.component).toBitmap(192, 192).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    val image = bitmap
    if (image != null) {
        Image(bitmap = image, contentDescription = null, modifier = box, contentScale = ContentScale.Fit)
    } else {
        Box(box)
    }
}

/** A large tile for the pages: the icon and its name, at least 72 dp tall and easy to hit. */
@Composable
fun BigTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Int = 0,
    transparency: Int? = null,
    badge: Int = 0,
    /** The square behind the icon: its own colour (0 follows the setting) and transparency (null follows the setting). */
    iconColor: Int = 0,
    iconTransparency: Int? = null,
    /** Smaller name, icon, corners and padding: the tiles of the 4×4 grid. */
    compact: Boolean = false,
    /** Off: no name under the icon, and the icon grows into the space. The name is still read out. */
    showName: Boolean = true,
    /** The lightning mark: this tile has a hold action. */
    holdMark: Boolean = false,
    /** A tile of the smaller grids (4×5, 5×5, 5×6): a one-line, smaller name. */
    tight: Boolean = false,
    /** The grid's own name and icon sizes (the home screen's tiles); null keeps the sizes of [compact]. */
    sizes: GridSizes? = null,
    /** Locked only: holding the tile runs its hold action. */
    onHold: (() -> Unit)? = null,
    /** The name's own colour (0: the colour for all tiles, else automatic). */
    textColor: Int = 0,
    icon: @Composable () -> Unit,
) = HoldTimed {
    val corner = if (compact) 18.dp else 28.dp
    val cardShape = tileShapeOf(LocalTileShape.current, corner)
    val palette = LocalPalette.current
    // Tiles switched off: no card, so the icon and its name sit on the wallpaper.
    val tiles = LocalShowTiles.current
    val iconPercent = iconTransparency ?: palette.iconTransparency
    val iconBack = (if (iconColor == 0) palette.icon else Color(iconColor).copy(alpha = 1f)).copy(alpha = tileOpacity(iconPercent))
    val solid = if (color == 0) palette.tile else Color(color).copy(alpha = 1f)
    val percent = transparency ?: palette.tileTransparency
    val background = solid.copy(alpha = tileOpacity(percent))
    // The tile's own name colour, else the one for all tiles, else whatever reads on this tile.
    val ink = tileNameInk(textColor, LocalTileText.current, if (!tiles) Color.White else if (color == 0) palette.tileText else readableOn(solid))
    // A tile that lets the wallpaper through gets a halo on its name so the name stays readable.
    // While a finger is on the tile it gets an outline in the highlight colour.
    val touch = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val halo = if (percent >= 50 || !tiles) androidx.compose.ui.graphics.Shadow(readableOn(ink), androidx.compose.ui.geometry.Offset(2f, 2f), 6f) else null
    Column(
        modifier = modifier
            // The card is drawn in its shape, but what sits on it is not cut to that shape, so a round tile never cuts off a number or a name.
            .then(if (tiles) Modifier.background(background, cardShape) else Modifier)
            .then(if (pressed) Modifier.border(if (compact) 4.dp else 6.dp, LocalAccent.current, cardShape) else Modifier)
            .tapOrHold(touch, "Open $label", onClick, onHold)
            .semantics(mergeDescendants = true) { contentDescription = tileContentDescription(label, badge, holdMark) }
            // Without tiles there is no card to keep clear of: the icon's square may reach the edge of its place.
            .padding(horizontal = if (!tiles) 0.dp else if (compact) 4.dp else 10.dp, vertical = (if (compact) 6.dp else 8.dp) + (if (tiles && LocalTileShape.current == com.glimpsemedia.glimpselauncher.data.SHAPE_CIRCLE) 10.dp else 0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The icon takes the height the name leaves, up to 80 dp, so the name is never cut off.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val most = when {
                sizes != null -> (if (showName) sizes.iconDp else sizes.iconNoNameDp).dp * LocalTileIconScale.current
                !showName -> if (compact) 72.dp else 96.dp
                compact -> 56.dp
                else -> 80.dp
            }
            val iconGap = if (sizes != null) 7.dp / LocalTileIconScale.current.coerceAtLeast(1f) else 7.dp
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.sizeIn(maxWidth = most, maxHeight = most).fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true)) {
                val iconSize = maxWidth
                // The square behind the icon has its own colour, transparency and shape, apart from the tile's.
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    val shape = iconSquareShape(LocalIconShape.current, maxWidth)
                    Box(
                        Modifier.fillMaxSize().then(
                            // The gap round the icon shrinks as the icon grows, so a large icon fills more of its square.
                            if (iconPercent < 100) Modifier.clip(shape).background(iconBack).padding(iconGap) else Modifier,
                        ),
                    ) { icon() }
                }
                // Inside the icon's own area, so a large icon at the tile's edge never cuts them off.
                if (badge > 0) CountBadge(badge, Modifier.align(Alignment.TopStart), small = smallBadgeFor(iconSize))
                if (holdMark) HoldMark(if (compact || smallBadgeFor(iconSize)) 18.dp else 24.dp, Modifier.align(Alignment.TopEnd))
            }
        }
        if (showName) Text(
            text = label,
            color = ink,
            fontSize = (sizes?.nameSp ?: if (tight) 12f else if (compact) 13f else 20f).times(LocalTileNameScale.current).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = if (compact || tight) 1 else 2,
            // Every tile keeps the same room for its name, so a long name never makes its icon smaller than the rest.
            minLines = if (compact || tight) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            style = androidx.compose.ui.text.TextStyle(shadow = halo),
            modifier = Modifier.padding(top = 4.dp, start = if (!tiles) 2.dp else 0.dp, end = if (!tiles) 2.dp else 0.dp),
        )
    }
}

/** A tile's name colour: the tile's own (0 if none), else the one for all tiles (0 if automatic), else [automatic]. */
fun tileNameInk(own: Int, all: Int, automatic: Color): Color = when {
    own != 0 -> Color(own)
    all != 0 -> Color(all)
    else -> automatic
}

/** Whether the tiles' cards are shown (Settings › Home screen › Screens), and the shapes of the tile, icon background and empty place. */
val LocalShowTiles = androidx.compose.runtime.compositionLocalOf { true }
val LocalIconShape = androidx.compose.runtime.compositionLocalOf { com.glimpsemedia.glimpselauncher.data.SHAPE_DEFAULT }
val LocalTileShape = androidx.compose.runtime.compositionLocalOf { com.glimpsemedia.glimpselauncher.data.SHAPE_DEFAULT }
val LocalSlotShape = androidx.compose.runtime.compositionLocalOf { com.glimpsemedia.glimpselauncher.data.SHAPE_DEFAULT }

/** The icon background [size] across: the usual rounded square (18 dp at 80 dp, in proportion), or a circle. */
fun iconSquareShape(shape: Int, size: androidx.compose.ui.unit.Dp): androidx.compose.ui.graphics.Shape =
    if (shape == com.glimpsemedia.glimpselauncher.data.SHAPE_CIRCLE) CircleShape else RoundedCornerShape(18.dp * (size / 80.dp))

/** A tile's or an empty place's shape: rounded [corner]s, or a circle (fully rounded ends where the place is long). */
fun tileShapeOf(shape: Int, corner: Dp): androidx.compose.ui.graphics.Shape =
    if (shape == com.glimpsemedia.glimpselauncher.data.SHAPE_CIRCLE) RoundedCornerShape(50) else RoundedCornerShape(corner)

/** Small icons (the 5×5 and 5×6 grids) get the small number, so it does not cover the whole icon. */
fun smallBadgeFor(iconSize: Dp): Boolean = iconSize < 64.dp


/** The colour chosen for every name on the tiles (0 is automatic), and the names' size; given by the home screen. */
val LocalTileText = androidx.compose.runtime.compositionLocalOf { 0 }
val LocalTileNameScale = androidx.compose.runtime.compositionLocalOf { 1f }
/** The icons' size on the tiles, as a scale (Tiles and icons › Icon size on the tiles). */
val LocalTileIconScale = androidx.compose.runtime.compositionLocalOf { 1f }

/** What a screen reader says for a tile, whether or not its name is shown: the name, and how many are waiting. */
fun tileContentDescription(label: String, badge: Int, hasHold: Boolean = false): String =
    (if (badge > 0) "$label, $badge new" else label) + (if (hasHold) ", has a hold action" else "")

/** The small round menu button a bottom-bar icon has while unlocked (the screen tiles have their own). */
@Composable
fun BarMenuButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        // Outlined in the bar's colour so it stands apart from the round All apps button, which shares its colour.
        modifier.size(30.dp).clip(CircleShape).background(LocalAccent.current).border(2.dp, LocalPalette.current.bar.copy(alpha = 1f), CircleShape)
            .clickable(onClickLabel = "Menu", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu for $name", tint = readableOn(LocalAccent.current), modifier = Modifier.size(20.dp)) }
}

/** The small lightning mark on a tile that has a hold action. */
@Composable
fun HoldMark(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = readableOn(LocalAccent.current), modifier = Modifier.fillMaxSize(0.8f))
    }
}

/** The small magnifying glass on the bar's All apps button while holding it opens Search. */
@Composable
fun SearchMark(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = readableOn(LocalAccent.current), modifier = Modifier.fillMaxSize(0.75f))
    }
}

/** A small red number for waiting notifications. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, small: Boolean = false) {
    val min = if (small) 24.dp else 32.dp
    Box(
        modifier.defaultMinSize(minWidth = min, minHeight = min).clip(CircleShape).background(Color(0xFFD32F2F)).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) { Text(if (count > 99) "99+" else count.toString(), color = Color.White, fontSize = if (small) 14.sp else 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun AllAppsGlyph() {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Apps, contentDescription = null, tint = readableOn(LocalAccent.current), modifier = Modifier.fillMaxSize(0.7f))
    }
}

/** The Search tile's picture: a magnifying glass on an accent disc, like the All apps one. */
@Composable
fun SearchGlyph() {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = readableOn(LocalAccent.current), modifier = Modifier.fillMaxSize(0.62f))
    }
}

/** The Emergency tile's picture: a white telephone on a red disc. */
@Composable
fun EmergencyGlyph() {
    Box(Modifier.fillMaxSize().clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFB71C1C)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Call, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.fillMaxSize(0.6f))
    }
}

/** The red an Emergency tile has until it is given a colour of its own. */
const val EMERGENCY_RED = 0xFFC62828.toInt()

@Composable
fun FolderGlyph() {
    Icon(Icons.Filled.Folder, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.fillMaxSize())
}

/** A small tile for the bottom bar. */
@Composable
fun DockTile(
    app: AppInfo, onClick: () -> Unit, modifier: Modifier = Modifier, showName: Boolean = true, holdMark: Boolean = false,
    onHold: (() -> Unit)? = null,
    /** Unlocked only: the icon's menu button. */
    onMenu: (() -> Unit)? = null,
    /** A six-app bar: smaller icons and names so seven places fit across. */
    compact: Boolean = false,
) = HoldTimed {
    val palette = LocalPalette.current
    val touch = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) LocalAccent.current.copy(alpha = 0.55f) else Color.Transparent)
            .tapOrHold(touch, "Open ${app.label}", onClick, onHold)
            .semantics(mergeDescendants = true) {
                contentDescription = tileContentDescription(app.label, com.glimpsemedia.glimpselauncher.data.Badges.forPackage(app.component.packageName), holdMark)
            }
            .padding(vertical = 6.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AppIcon(app, dockIconSize(showName, compact))
            val n = com.glimpsemedia.glimpselauncher.data.Badges.forPackage(app.component.packageName)
            // Unlocked, the menu button takes the corner the count normally uses.
            if (onMenu != null) BarMenuButton(app.label, onMenu, Modifier.align(Alignment.TopEnd))
            else if (n > 0) CountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp), small = true)
            if (holdMark) HoldMark(18.dp, Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp))
        }
        if (showName) Text(
            app.label, color = palette.barText, fontSize = if (compact) 11.sp else 13.sp, maxLines = 1,
            overflow = TextOverflow.Clip, softWrap = false, textAlign = TextAlign.Center,
        )
    }
}

/** A bottom-bar icon's size: larger without names, smaller when six apps share the bar. */
fun dockIconSize(showName: Boolean, compact: Boolean) = when {
    compact && showName -> 42.dp
    compact -> 48.dp
    showName -> 52.dp
    else -> 60.dp
}

/** The fixed button in the middle of the bottom bar. */
@Composable
fun AllAppsButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, showName: Boolean = true, holdMark: Boolean = false,
    onHold: (() -> Unit)? = null, onMenu: (() -> Unit)? = null, compact: Boolean = false,
    /** Hold All apps to search is on: a magnifying glass in place of the lightning mark. */
    searchMark: Boolean = false,
) = HoldTimed {
    val palette = LocalPalette.current
    val touch = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) LocalAccent.current.copy(alpha = 0.55f) else Color.Transparent)
            .tapOrHold(touch, "Show all apps", onClick, onHold)
            .semantics { contentDescription = tileContentDescription("All apps", 0, holdMark) + if (searchMark) ". Hold to search" else "" }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier.size(dockIconSize(showName, compact)).clip(CircleShape).background(LocalAccent.current),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Apps, contentDescription = null, tint = readableOn(LocalAccent.current), modifier = Modifier.size(if (compact) 28.dp else 34.dp))
            }
            if (holdMark) HoldMark(18.dp, Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp))
            if (searchMark) SearchMark(18.dp, Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
            if (onMenu != null) BarMenuButton("the All apps button", onMenu, Modifier.align(Alignment.TopEnd))
        }
        if (showName) Text("All apps", color = palette.barText, fontSize = if (compact) 11.sp else 13.sp, maxLines = 1, softWrap = false)
    }
}

/** A folder's face: up to four of its apps' icons in a small square. */
@Composable
fun FolderPreview(apps: List<AppInfo>) {
    val shown = apps.take(4)
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in shown.chunked(2).ifEmpty { listOf(emptyList()) }) {
            androidx.compose.foundation.layout.Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (app in row) Box(Modifier.weight(1f).fillMaxSize()) { AppIcon(app, null) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Keeps clear of the top bar and the camera cutout, even in full screen when the bar is hidden: without this, the
 * clock and the first row slide up under the camera hole.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun Modifier.topSafePadding(): Modifier = composed {
    windowInsetsPadding(
        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
            .union(androidx.compose.foundation.layout.WindowInsets.displayCutout)
            .only(androidx.compose.foundation.layout.WindowInsetsSides.Top),
    )
}

/** The standard folder colour for the folder-icon look: a warm folder yellow. */
const val FOLDER_YELLOW = 0xFFF2B33D.toInt()

/** How folders look on the screens (0 their apps' icons, 1 a folder icon), which picture (1 to 6) and the colour they share; given by the home screen. */
val LocalFolderStyle = androidx.compose.runtime.compositionLocalOf { 0 }
val LocalFolderIcon = androidx.compose.runtime.compositionLocalOf { 3 }
val LocalFolderColor = androidx.compose.runtime.compositionLocalOf { FOLDER_YELLOW }

