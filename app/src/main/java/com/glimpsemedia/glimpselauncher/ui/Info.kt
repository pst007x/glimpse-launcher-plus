package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val SUPPORT_EMAIL = "support@glimpse-media.com"
const val WEBSITE = "glimpse-media.com"
const val PRIVACY_URL = "https://glimpse-media.com/glimpse/privacy.html"

/** What everything does, in short entries. */
/**
 * What the All files and folders dialog says before Android is asked: Android 11 and later open their own settings
 * page ([settingsPage]); Android 8 to 10 ask the usual question.
 */
fun allFilesAskText(settingsPage: Boolean): String =
    if (settingsPage) "Glimpse Launcher Plus needs All files access to see every file and folder name. Android will open its page for this app: switch on Allow access to manage all files, then come back."
    else "Glimpse Launcher Plus needs access to your files to see every file and folder name. Android will ask: choose Allow."

/** Shown with the App info page when Android 8 to 10 no longer ask (the person once chose Don't ask again). */
const val ALL_FILES_APP_INFO_NOTE = "Open Permissions › Files and media › Allow"

/** The Search help topic; Glimpse Launcher Plus adds a paragraph on All files and folders. */
fun searchHelpText(plus: Boolean): String = SEARCH_HELP + if (plus) SEARCH_HELP_PLUS else ""

private const val SEARCH_HELP = "Touch the Search tile (add it from an empty place's menu, or Settings > Home screen > Search) to open the search window. Type, and results show under Apps, Files, Folders, Menu items, Help items and Online; touch one to go to it. Apps, Settings and Help are always searched. Add folder location(s) lets you pick folders whose file and folder names are searched (never what is inside the files). Add search engine(s) lets you add your own web searches with a name and an address containing %s; none are built in. Hold All apps to search makes holding the bottom bar's All apps button, while locked, open Search; a small magnifying glass shows on it."
private const val SEARCH_HELP_PLUS = " In Glimpse Launcher Plus, Settings > Home screen > Search > All files and folders searches the names of every file and folder in the phone's own storage (not memory cards, hidden files, or Android/data and Android/obb). Android asks you to allow All files access first. Only names are read, on the phone, and nothing is sent anywhere."

private val helpTopics = listOf(
    "The home screen" to "Touch a tile to open its app. Swipe sideways for more screens; the dots show where you are. The Home button brings you back to the first screen.",
    "Text mode" to "Settings > Home screen > Text mode > Use text mode swaps the tile screens for one calm screen of app names. It has an optional clock at the top, set in Settings > Home screen > Clock > Text mode view: on or off, Small, Medium or Large, on the left, in the middle or on the right. Below it, the app list sits on the bottom left or bottom right (List side) and the folder icons on the other side. Both fill from the bottom up: the first app sits lowest. The text comes in three sizes: Small, Medium and Large. Its colour (the names, the folder titles and the clock) is set in Settings > Home screen > Text mode > Text colour, apart from the tile screens, so you can choose one that stands out on your wallpaper. Folder icons follow it, or each folder can have its own Icon colour in its editor. Touch a name to open the app, locked or unlocked. Touch a folder icon to show its apps in place of the list, under the folder's name (its icon is underlined); touch the icon again, or use Back, Home or locking, to return to the list. Swipe left for the widgets screen: one column of full-width widgets. While unlocked, use the Add widget button at its top (or hold an empty part of it), and hold a widget for Change the size, Move up, Move down or Remove the widget. The Text mode switch widget (on a tile screen or the widgets screen) swaps between the tiles and text mode with one touch. The first time, it starts with the phone's own Phone, Messages, Gallery and browser, and a Settings folder. Change the list and the folders in Settings > Home screen > Text mode, where they are listed in the same order as on the screen: add, remove and move apps; add folders with a name and one of 21 icons. Text mode keeps its own list and folders: your tile screens are kept exactly as they were and come back when you turn it off. Swipes, the notes, the padlock and the PIN work as usual.",
    "Glimpse Settings in text mode" to "A text-mode list or folder can hold Glimpse Settings (this launcher's Settings) and System settings (the phone's own). While unlocked, Glimpse Settings opens straight away. While locked it never gets round the padlock: with a PIN set it asks for the PIN, and the right PIN unlocks and opens Settings; without a PIN it says to hold the padlock first.",
    "The grid" to "Settings > Home screen > Screens > Grid: 2 × 4 for eight big tiles a screen, or 4 × 4 for sixteen smaller ones. With Tiles behind the icons switched off, three smaller grids can be used too: 4 × 5, 5 × 5 and 5 × 6. Switching keeps the order of your tiles; folders move as one, and widgets are resized to match.",
    "Tiles and icon shapes" to "Settings > Home screen > Screens > Tiles behind the icons: on, each icon and its name sit on a card; off, they sit straight on the wallpaper, and the smaller grids can be used. Switching the tiles back on while a smaller grid is chosen moves it to 4 × 4. Settings > Colours and themes > Tiles and icons > Shapes: a preview names the three parts, the Tile (the card behind an icon and its name), the Icon background (the square behind each icon) and the Empty place (the outlined place with a + while unlocked), and each can be Default or Circle. A notification number sits at the top left of its icon.",
    "Search" to searchHelpText(com.glimpsemedia.glimpselauncher.BuildConfig.PLUS),
    "The bottom bar" to "Settings > Home screen > Bottom bar: show or hide the bar in icon view and in text mode separately, and choose 4 apps (two each side of the All apps button) or 6 (three each side). Apps are listed left to right as in the bar: Left and Right move one, Hold action gives it a second job, Remove takes it off the bar (the app stays on the phone). While unlocked, each icon in the bar has the same Move left and Move right in its own menu. Going back from 6 to 4 keeps the outer apps, hidden, until you choose 6 again. With Settings > Home screen > Search > Hold All apps to search on, holding the All apps button while locked opens Search.",
    "Swipes" to "On the home screen, swipe down for the notifications and swipe up for All apps. A sideways swipe turns the screen, and dragging a tile (when unlocked) never counts as a swipe.",
    "Names" to "Settings > Colours and themes > Tiles and icons has two separate switches: Names on the tiles and Names in the bottom bar. Turn off either or both for icons only; screen readers still say each name.",
    "The padlock" to "While locked, tiles only open: nothing can be moved, removed or changed. Hold the padlock at the bottom right for three seconds to unlock (one second with Quick unlock, in Settings > Safety and privacy > Locking and PIN). A settings cog appears at the bottom left (Settings opens on five groups; search at the top finds any setting by name) and each tile gets a menu button. Tap the padlock to lock again. Settings > Home screen > Bottom bar can hide the bar completely, in either mode; the padlock stays. It also locks by itself after the time set in Settings > Safety and privacy > Locking and PIN, and when the screen turns off.",
    "The PIN" to "Settings > Safety and privacy > Locking and PIN > Set a PIN (4 to 8 digits). After the three-second hold the PIN is asked for. Five wrong tries in a row block the pad for 30 seconds. Forgotten it? Choose Forgotten PIN? and confirm with the phone's own screen lock to set a new one. Change the PIN and Remove the PIN ask for the current one first. Only a scrambled hash of the PIN is kept, on the phone.",
    "All apps" to "The round button in the middle of the bottom bar (or a swipe up) lists every app from A to Z. The keyboard comes up ready: type to narrow the list, and scroll to put the keyboard away; under Related come apps of that kind (try internet or social). Filter shows only the apps with one tag.",
    "App info and Uninstall" to "While unlocked, hold an app's name in All apps: Add to the screen, Tags, App info (the phone's page for the app) and Uninstall (the phone asks you to confirm). An uninstalled app's tile goes from the screens by itself.",
    "Hold actions" to "Give a tile one extra thing to do. While unlocked, open the tile's menu and choose Hold action: open an app, open a web address, or a phone action (torch on and off, call or text a number, camera, alarms, timer, or a settings page such as Wi-Fi or Bluetooth). Bottom-bar icons have the same menu button while unlocked (or use Settings > Home screen > Bottom bar). While locked, hold the tile for half a second: the phone buzzes and the action runs. A call first counts down three seconds, with Cancel. A tap still opens the app. Tiles with an action show a small lightning mark; Settings > Colours and themes > Tiles and icons can hide it. The Emergency tile and widgets have no hold action.",
    "Notes" to "Swipe right on the first screen for your notes; swipe left or press Home to go back. New note starts a card at the top. Each line has a tick box: Enter starts a new line, and deleting at the very start of a line joins it to the line above. A line's menu gives it a colour band. Add a picture takes a photo or chooses one from the gallery; the launcher keeps its own copy. The ▾ arrow folds a card to its title and a summary. The card's menu shares the note (the lines as ☐ and ☑, with the pictures), changes its colour, moves it up or down, or deletes it. Notes are kept only on the phone, and can be read and ticked while the screens are locked.",
    "Notes and the PIN" to "Settings > Safety and privacy > Locking and PIN > Notes need the PIN (once a PIN is set) hides the notes until the PIN is entered. Then, after holding the padlock for three seconds (on any screen) and entering the PIN, choose Unlock notes (the notes only) or Edit mode (all editing, and the notes). Without this setting, the PIN goes straight to edit mode. Lock the notes, the padlock, the automatic lock and the screen turning off all lock them again. A hold action can open the notes screen too.",
    "Tags" to "While unlocked, hold an app in All apps and choose Tags to tick its tags or make one. Filter > Manage tags renames or deletes them. Deleting a tag never removes an app.",
    "Moving and adding" to "While unlocked, hold a tile and drag it; drag to the screen edge to move it to the next screen. Touch a plus square to add an app or a widget there. Hold an empty square for more, such as the wallpaper or removing an empty screen. Drag a tile to the empty screen after the last one to start a new screen. Remove from the screen never uninstalls the app.",
    "Folders" to "Drag one tile onto another and hold until the square lights up, then let go. While unlocked you can rename a folder, add apps straight into it (Add apps in the open folder, or Add apps to this folder in its menu; up to 8), take apps out, and reorder them by dragging or with Move earlier and Move later. Folders show their apps' icons; to show a folder picture instead, switch on Settings > Colours and themes > Tiles and icons > Folder icon and choose one of six in Folder picture. Folder colour sets the colour of all of them, and a folder's menu can give it its own.",
    "Free layout" to "Settings > Home screen > Screens > Free layout puts a tile in any square, not only from the top. A plus square then adds exactly there, and a dragged tile lands where you let go.",
    "Text mode switch" to "Glimpse Launcher has its own widget, Text mode switch: a small pill that shows Text mode: On or Off in your theme's colours. Put it on a tile screen or on text mode's widgets screen; one touch swaps between the tiles and text mode, even while locked. Only the view changes: your tiles and your text list stay as they are.",
    "Widgets" to "Settings > Home screen > Screens > Add widgets, or Add a widget from a plus square. Glimpse Launcher's own Text mode switch is in the list too; a touch on it swaps to text mode, even while unlocked. A widget's menu (when unlocked) has Change the size and Remove the widget; hold and drag it to move it.",
    "Colours and themes" to "Settings > Colours and themes: eight ready-made themes, four light (Coral, Sage, Indigo, Sand) and four dark (Cyan, Violet, Neon, Gold). A theme sets the colours of the tiles, the squares behind the icons, the bottom bar, the clock and the highlight; your wallpaper stays until you choose one. You can still pick any colour yourself, with its transparency, and Darker or lighter fine-tunes it (Reset puts it back). A single tile can have its own colours from its menu. Text on the tiles and the bar sets one colour for every name on the tiles and in the bottom bar; Automatic picks whatever reads on the tile colour. A theme puts it back to Automatic. A single tile or folder can have its own name colour from its menu (Change the colour of the name), which wins over the colour for all tiles. Name size on the tiles makes the names under the tiles smaller or larger.",
    "Saved themes" to "Settings > Colours and themes > Themes > Save current theme keeps your whole look under a name: light or dark menus, the colours of the tiles, the squares behind the icons, the bottom bar, the clock and its panel, the highlight, and every transparency and Darker or lighter setting. It appears under My themes (in Settings and on the welcome's Grid and look page); touch it to use it again. Hold a saved theme in Settings for Rename, Update to the current look or Delete. Up to 20 can be saved. A single tile's own colours stay with that tile and are not part of a theme. The grid, names, apps, notes, hold actions and locking are never changed by a theme.",
    "Saved themes and the wallpaper" to "A saved theme also keeps the wallpaper, as long as Glimpse Launcher set it: a theme wallpaper, My colours now, or a picture chosen with Change the wallpaper (the launcher keeps its own copy of that picture). Android does not let any app copy a wallpaper set elsewhere, for example in the phone's own Settings or a gallery app; then the theme is saved with its colours only and you are told so. Using a saved theme puts its wallpaper back on the same screens it was on: the home screen, the lock screen or both. Everything is kept only on the phone.",
    "Theme wallpapers" to "Settings > Colours and themes > Themes > Theme wallpaper paints a wallpaper to match a theme: a soft blend with two gentle glows, made at your screen's size. My colours now uses the colours you have set at the moment. Choose Home screen, Lock screen or Both. On the welcome's Grid and look page, Use the matching wallpaper appears after you pick a theme. Change the wallpaper (Settings > Home screen > Screens) uses a picture of your own instead.",
    "The clock" to "Settings > Home screen > Clock > Icon view: digital, side by side or a round face, in three sizes, with or without the date and a background. Text mode view sets text mode's own clock: on or off, its size and its position.",
    "Notification numbers" to "Settings > Safety and privacy > Notification numbers shows how many notifications an app has waiting. Switch Glimpse Launcher on in the phone's Notification access list. Only the count is used; nothing is read, kept or shared.",
    "Full screen" to "Settings > Home screen > Screens > Full screen hides the top bar. Swipe down from the very top edge to see it for a moment. The screens still keep clear of the camera at the top.",
    "The Emergency button" to "Switch it on in Settings and add a phone number, then put the Emergency tile on a screen. Touching it starts a countdown (5, 10, 15 or 20 seconds) with a big Cancel, then rings the number. Glimpse Launcher only starts the call.",
    "Making it your home screen" to "Use the button in Settings or on the last welcome page and confirm when the phone asks. To go back, the phone's Settings > Apps > Default apps > Home app.",
    "Backup and restore" to "Settings > Your setup > Backup and restore > Back up to a file saves your whole setup to one file, wherever you choose: settings, screens, text mode, notes with their pictures, saved themes and the PIN. Restore from a file shows what a backup holds and asks before replacing anything; Glimpse Launcher then restarts on the restored setup. Widgets are not included (add them again), and apps that are not on the phone are left out. If a restore cannot finish, your setup stays as it was. A backup with a PIN should be kept private.",
    "Report bugs" to "Settings > Help and support > Report bugs: Report a bug by email (say what you were doing, what you expected and what happened instead), Suggest a feature, and Visit the website. If something keeps going wrong, switch the Bug log on, use Glimpse Launcher until it happens again, then Email the bug log. The log records crashes and errors only (when, where, the version and the phone model), stays on this phone, and is sent only when you email it. Clear the bug log empties it.",
    "Something went wrong?" to "Press Home to get back to the first screen. A missing tile is still in All apps. Settings > Your setup > Start again clears the screens or fills them with every app. For anything else, write to $SUPPORT_EMAIL.",
)

@Composable
private fun InfoFrame(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    val menu = LocalMenu.current
    Column(Modifier.fillMaxSize().background(menu.background).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = menu.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                "Close", color = readableOn(LocalAccent.current), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(LocalAccent.current)
                    .clickable(onClick = onClose).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        content()
    }
}

/** Every Help topic, title and text, for Search. */
val helpTopicList: List<Pair<String, String>> get() = helpTopics

/** Help; [startAt] is a topic's title to open at (from Search). */
@Composable
fun HelpScreen(onClose: () -> Unit, startAt: String? = null) = MenuText {
    val menu = LocalMenu.current
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(startAt) {
        val at = helpTopics.indexOfFirst { it.first == startAt }
        if (at >= 0) list.scrollToItem(at)
    }
    InfoFrame("Help", onClose) {
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(helpTopics) { (heading, text) ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(16.dp)) {
                    Text(heading, color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    Text(text, color = menu.text, fontSize = 19.sp, lineHeight = 27.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/** The features in brief: a bold lead and one short line each. */
private val aboutFeatures = listOf(
    "Your grid" to "2 × 4 big tiles or 4 × 4 compact ones, with names on or off, separately for the tiles and the bottom bar.",
    "Quick gestures" to "Swipe down for notifications, up for All apps.",
    "Locked when you want" to "A three-second hold on the padlock unlocks; add a PIN if you like, with the phone's own lock as the backup.",
    "All apps, organised" to "Search, tags and a filter, with App info and Uninstall a hold away when unlocked.",
    "Folders, widgets and free layout" to "Group apps, add live widgets, and place tiles in any square.",
    "Your colours" to "Ready-made looks, or your own colours and transparency for every part.",
    "Private" to "No adverts and no internet use. Everything stays on the phone.",
)

@Composable
fun AboutScreen(versionName: String, onEmail: () -> Unit, onWebsite: () -> Unit, onPrivacy: () -> Unit, onClose: () -> Unit) = MenuText {
    val menu = LocalMenu.current
    InfoFrame("About", onClose) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(16.dp)) {
                    Text(com.glimpsemedia.glimpselauncher.model.appName(com.glimpsemedia.glimpselauncher.BuildConfig.PLUS), color = menu.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Version $versionName", color = menu.dimText, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
                    // Plus is not on Google Play, so it does not update itself: this opens the page with the newest version.
                    if (com.glimpsemedia.glimpselauncher.BuildConfig.PLUS) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        BigButton("Get the latest version", {
                            try {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.glimpsemedia.glimpselauncher.model.PLUS_PAGE)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Open glimpse-media.com/glimpse/plus in a web browser", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }, Modifier.padding(top = 10.dp), primary = true)
                    }
                    Text(
                        "A fast, tidy home screen you can make your own.",
                        color = menu.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "For people who want their phone their way: a grid that suits you, quick gestures, and a padlock that keeps the layout safe.",
                        color = menu.text, fontSize = 19.sp, lineHeight = 27.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("What you get", color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    for ((lead, line) in aboutFeatures) {
                        Text(
                            androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
                                append(". ")
                                append(line)
                            },
                            color = menu.text, fontSize = 19.sp, lineHeight = 26.sp,
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(menu.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Questions or help", color = menu.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    BigButton(SUPPORT_EMAIL, onEmail, primary = true)
                    BigButton(WEBSITE, onWebsite)
                    BigButton("Privacy policy", onPrivacy)
                }
            }
            item {
                Text(
                    "Designed by Salvatore Turone and written with the help of Claude Code, Anthropic's AI coding tool.\n\n© Glimpse Media",
                    color = menu.dimText, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
