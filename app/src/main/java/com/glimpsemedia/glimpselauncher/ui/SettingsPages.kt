package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CallToAction
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.glimpsemedia.glimpselauncher.R

/** True for settings drawn inside a page's card: rows then drop their own background and use the smaller text. */
val LocalInCard = staticCompositionLocalOf { false }

/** A row title inside a card (a choice's name above its buttons). */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, color = LocalMenu.current.text, fontSize = 17.sp, modifier = modifier)

/** A dim note under a setting, inside a card. */
@Composable
fun CardNote(text: String, modifier: Modifier = Modifier, strong: Boolean = false) =
    Text(text, color = if (strong) LocalMenu.current.text else LocalMenu.current.dimText, fontSize = 15.sp,
        fontWeight = if (strong) FontWeight.SemiBold else null, modifier = modifier.padding(top = 4.dp))

private fun contrast(a: Color, b: Color): Float {
    val l1 = a.copy(alpha = 1f).luminance()
    val l2 = b.copy(alpha = 1f).luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}

/** The highlight colour where it reads on [on], otherwise the menu text colour. */
@Composable
private fun accentOn(on: Color): Color = LocalAccent.current.let { if (contrast(it, on) >= 3f) it else LocalMenu.current.text }

fun iconFor(key: String): ImageVector = when (key) {
    "grid" -> Icons.Outlined.GridView
    "palette" -> Icons.Outlined.Palette
    "shield" -> Icons.Outlined.Shield
    "box" -> Icons.Outlined.Inventory2
    "help" -> Icons.AutoMirrored.Outlined.HelpOutline
    "screens" -> Icons.Outlined.Apps
    "bar" -> Icons.Outlined.CallToAction
    "search" -> Icons.Outlined.Search
    "clock" -> Icons.Outlined.Schedule
    "text" -> Icons.AutoMirrored.Outlined.Notes
    "themes" -> Icons.Outlined.AutoAwesome
    "menus" -> Icons.Outlined.DarkMode
    "tiles" -> Icons.Outlined.Dashboard
    "colours" -> Icons.Outlined.ColorLens
    "lock" -> Icons.Outlined.Lock
    "sos" -> Icons.Outlined.Sos
    "badge" -> Icons.Outlined.Notifications
    "backup" -> Icons.Outlined.Backup
    "reset" -> Icons.Outlined.RestartAlt
    "homeset" -> Icons.Outlined.Home
    "info" -> Icons.Outlined.Info
    "bug" -> Icons.Outlined.BugReport
    else -> Icons.Outlined.Apps
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMenu.current.dimText.copy(alpha = .25f)))

@Composable
private fun PageBar(title: String, onBack: (() -> Unit)?, onDone: () -> Unit) {
    val menu = LocalMenu.current
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack == null) 18.dp else 6.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = menu.text, modifier = Modifier.size(26.dp)) }
        }
        Text(
            title, color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp).semantics { heading() },
        )
        Text(
            "Done", color = readableOn(LocalAccent.current), fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                .clickable(onClick = onDone).padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun EntryCard(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LocalMenu.current.surface), content = content)

/** One row of a list of groups, pages or search results: an icon, the name, a one-line summary and "›". */
@Composable
private fun Entry(icon: String, title: String, summary: String, first: Boolean, onClick: () -> Unit) {
    val menu = LocalMenu.current
    if (!first) Divider()
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = title, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(menu.button), contentAlignment = Alignment.Center) {
            Icon(iconFor(icon), contentDescription = null, tint = accentOn(menu.button), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = menu.text, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (summary.isNotEmpty()) Text(summary, color = menu.dimText, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = menu.dimText, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SearchBox(query: String, onQuery: (String) -> Unit) {
    val menu = LocalMenu.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(menu.surface).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = menu.dimText, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 10.dp)) {
            if (query.isEmpty()) Text("Search settings", color = menu.dimText, fontSize = 17.sp)
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = TextStyle(color = menu.text, fontSize = 17.sp),
                cursorBrush = SolidColor(LocalAccent.current),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { },
            )
        }
        if (query.isNotEmpty()) Text(
            "Clear", color = menu.dimText, fontSize = 15.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onQuery("") }.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

/** A setting inside a card; when [flashing], it is brought into view and lit softly in the highlight colour. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlashRow(flashing: Boolean, onDone: () -> Unit, content: @Composable () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    val glow = remember { Animatable(0f) }
    val accent = LocalAccent.current
    LaunchedEffect(flashing) {
        if (flashing) {
            delay(80)
            requester.bringIntoView()
            glow.snapTo(0.35f)
            glow.animateTo(0f, tween(1500))
            onDone()
        }
    }
    Box(
        Modifier.fillMaxWidth().bringIntoViewRequester(requester).clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = glow.value)).padding(vertical = 8.dp),
    ) {
        // A setting can be several parts (a title, its buttons, a note): they go one under another.
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

/**
 * Settings as pages: the first page lists the groups (with a search box), a group page lists its pages, and a page
 * shows its settings in blocks. [stack] holds "g:<group>" and "s:<section>" entries; empty is the first page. The
 * settings themselves are drawn by [rowContent], by name, and only those [rowShown] says apply now.
 */
@Composable
fun SettingsPages(
    stack: List<String>,
    onStack: (List<String>) -> Unit,
    facts: SummaryFacts,
    onClose: () -> Unit,
    onOpenElsewhere: (sectionId: String) -> Unit,
    /** Opens the Buy me a coffee page, from the card at the foot of the first page. */
    onCoffee: () -> Unit,
    rowShown: (sectionId: String, label: String) -> Boolean,
    rowContent: @Composable (sectionId: String, label: String) -> Unit,
    footer: @Composable (sectionId: String) -> Unit,
    /** A row to bring into view and light when Settings opens at it (from Search); [onFlashTaken] clears it. */
    initialFlash: String? = null,
    onFlashTaken: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var flash by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialFlash) { if (initialFlash != null) { flash = initialFlash; onFlashTaken() } }
    val listStates = remember { mutableMapOf<String, LazyListState>() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val menu = LocalMenu.current

    fun open(sectionId: String, from: List<String>) {
        if (SettingsMap.sections.getValue(sectionId).opensElsewhere) onOpenElsewhere(sectionId) else onStack(from + "s:$sectionId")
    }
    fun openHit(h: SettingsHit) {
        keyboard?.hide(); focus.clearFocus()
        if (SettingsMap.sections.getValue(h.section).opensElsewhere) return onOpenElsewhere(h.section)
        flash = h.row
        onStack(listOf("g:${SettingsMap.groupOf(h.section).id}", "s:${h.section}"))
    }

    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
        AnimatedContent(
            targetState = stack,
            transitionSpec = {
                val forward = targetState.size >= initialState.size
                (slideInHorizontally(tween(220)) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
            },
            label = "settings page",
        ) { pages ->
            val top = pages.lastOrNull()
            val back: (() -> Unit)? = if (pages.isEmpty()) null else ({ onStack(pages.dropLast(1)) })
            val list = listStates.getOrPut(top ?: "root") { LazyListState() }
            Column(Modifier.fillMaxSize()) {
                when {
                    top == null -> {
                        PageBar("Settings", null, onClose)
                        val hits = SettingsMap.search(query)
                        LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            item(key = "search") { SearchBox(query) { query = it } }
                            if (query.isNotBlank()) {
                                item(key = "hits") {
                                    if (hits.isEmpty()) {
                                        Text("No setting called “${query.trim()}”.", color = menu.dimText, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                                    } else EntryCard {
                                        hits.forEachIndexed { i, h ->
                                            val sec = SettingsMap.sections.getValue(h.section)
                                            Entry(sec.icon, h.label, "${SettingsMap.groupOf(h.section).title} › ${sec.title}", i == 0) { openHit(h) }
                                        }
                                    }
                                }
                            } else {
                                item(key = "groups") {
                                    EntryCard {
                                        SettingsMap.groups.forEachIndexed { i, g ->
                                            Entry(g.icon, g.title, groupSummary(g.id, facts), i == 0) { onStack(listOf("g:${g.id}")) }
                                        }
                                    }
                                }
                                item(key = "version") { Text("Glimpse Launcher ${facts.versionName}", color = menu.dimText, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
                                item(key = "coffee") { CoffeeCard(onCoffee) }
                            }
                        }
                    }
                    top.startsWith("g:") -> {
                        val g = SettingsMap.groups.first { it.id == top.removePrefix("g:") }
                        PageBar(g.title, back, onClose)
                        LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp)) {
                            item(key = "sections") {
                                EntryCard {
                                    g.sections.forEachIndexed { i, id ->
                                        val sec = SettingsMap.sections.getValue(id)
                                        Entry(sec.icon, sec.title, sectionSummary(id, facts), i == 0) { open(id, pages) }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val id = top.removePrefix("s:")
                        val sec = SettingsMap.sections.getValue(id)
                        PageBar(sec.title, back, onClose)
                        val blocks = sec.blocks.map { b -> b to b.rows.filter { rowShown(id, it) } }.filter { it.second.isNotEmpty() }
                        val target = flash
                        LaunchedEffect(target, id) {
                            if (target != null) {
                                val at = blocks.indexOfFirst { target in it.second }
                                if (at >= 0) list.scrollToItem(at) else flash = null   // not shown at the moment: the page opens at the top
                            }
                        }
                        LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            itemsIndexed(blocks, key = { i, _ -> "b$i" }) { _, (block, rows) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (block.heading != null) Text(
                                        block.heading.uppercase(), color = accentOn(menu.background), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.08.em, modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
                                    )
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(menu.surface).padding(horizontal = 14.dp, vertical = 2.dp)) {
                                        CompositionLocalProvider(LocalInCard provides true) {
                                            rows.forEachIndexed { i, label ->
                                                if (i > 0) Divider()
                                                FlashRow(flashing = label == flash, onDone = { flash = null }) { rowContent(id, label) }
                                            }
                                        }
                                    }
                                }
                            }
                            item(key = "footer") { footer(id) }
                        }
                    }
                }
            }
        }
    }
}

/** The coffee card's own colours: fixed, so every theme shows the same warm card. */
private val CoffeeDark = Color(0xFF5B3A24)
private val CoffeeLight = Color(0xFFB9783A)
private val Cream = Color(0xFFFFE9C7)
private val CreamDim = Color(0xFFF6DDBA)

/** The app's own icon, drawn from its two adaptive-icon layers (only the middle two thirds show, as on a home screen). */
@Composable
private fun AppIconBadge(size: androidx.compose.ui.unit.Dp) = Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.25f))) {
    Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize().scale(1.5f))
    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize().scale(1.5f))
}

/** Buy me a coffee, at the foot of the first Settings page: a thank-you and a way to say thanks back. */
@Composable
private fun CoffeeCard(onCoffee: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(CoffeeDark, CoffeeLight)))
            .clickable(onClickLabel = "Buy me a coffee", onClick = onCoffee)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppIconBadge(56.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("Buy me a coffee", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Thank you for installing this app. I hope you enjoy it. Please feel free to contact me with any suggestions or bugs.",
                    color = CreamDim, fontSize = 16.5.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Text(
            "Buy me a coffee \u2615", color = CoffeeDark, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Cream).padding(vertical = 12.dp),
        )
    }
}

/** A size step in [range] (0 as usual): the name with its value, a slider with a stop for each step, and the steps written under it. */
@Composable
fun StepSlider(label: String, step: Int, range: IntRange, onStep: (Int) -> Unit) {
    val menu = LocalMenu.current
    CardTitle("$label: ${stepLabel(step)}")
    androidx.compose.material3.Slider(
        value = step.toFloat(),
        onValueChange = { v -> val n = Math.round(v).coerceIn(range.first, range.last); if (n != step) onStep(n) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = range.count() - 2,
        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = LocalAccent.current, activeTrackColor = LocalAccent.current),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: ${stepLabel(step)}" },
    )
    Row(Modifier.fillMaxWidth()) {
        for (n in range) Text(
            stepLabel(n), color = if (n == step) menu.text else menu.dimText, fontSize = 13.sp,
            fontWeight = if (n == step) FontWeight.Bold else null, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
    }
}
