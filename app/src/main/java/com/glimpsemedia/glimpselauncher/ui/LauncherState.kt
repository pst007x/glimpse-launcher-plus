package com.glimpsemedia.glimpselauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.model.withBigRepaired
import com.glimpsemedia.glimpselauncher.model.Notes
import com.glimpsemedia.glimpselauncher.model.TextLayout
import com.glimpsemedia.glimpselauncher.model.withApp
import com.glimpsemedia.glimpselauncher.model.withoutEmptyExtraPages

/** Everything the screen shows and changes. Each change to the layout or settings is saved at once. */
class LauncherState(
    layout: Layout,
    settings: Settings,
    private val saveLayout: (Layout) -> Unit,
    private val saveSettings: (Settings) -> Unit,
    notes: Notes = Notes(),
    private val saveNotes: (Notes) -> Unit = {},
    textLayout: TextLayout? = null,
    private val saveTextLayout: (TextLayout) -> Unit = {},
) {
    /** Text mode's list and folders; null until text mode is first turned on. Never touches the tile layout. */
    var textLayout by mutableStateOf(textLayout)
        private set

    fun changeTextLayout(t: TextLayout) {
        if (t != textLayout) {
            textLayout = t
            saveTextLayout(t)
        }
    }

    /** The text-mode folder shown in place of the main list; null shows the main list. */
    var textFolderOpen by mutableStateOf<String?>(null)

    /** Builds the first text layout from the phone's default apps; set by the activity once the apps are known. */
    var startingTextLayoutFor: (() -> TextLayout)? = null

    /** All apps' "Add" in text mode: to the end of the text list (never the tiles); false if it is already there. */
    fun addToTextList(key: String): Boolean {
        val t = textLayout ?: TextLayout()
        if (key in t.apps) return false
        changeTextLayout(t.withApp(key))
        return true
    }

    /** Text mode on with nothing saved yet: start with the basic layout (later, if the apps are not known yet). */
    fun ensureTextLayout() {
        val start = startingTextLayoutFor ?: return
        if (settings.textMode && textLayout == null) changeTextLayout(start())
    }

    /** The notes screen's notes; each change is saved shortly after (see the activity). */
    var notes by mutableStateOf(notes)
        private set

    fun changeNotes(new: Notes) {
        // Typing is in the keyboard's own window: a change to the notes counts as a touch for the auto-lock.
        touched()
        if (new != notes) {
            notes = new
            saveNotes(new)
        }
    }

    /** The person's own saved themes and the wallpapers Glimpse last set; the activity changes and saves them. */
    var savedThemes by mutableStateOf(com.glimpsemedia.glimpselauncher.data.SavedThemes())

    /** Counts requests to show the notes screen (a hold action); the pager slides to it. */
    var openNotes by mutableStateOf(0)

    var layout by mutableStateOf(layout)
        private set
    var settings by mutableStateOf(settings)
        private set
    var apps by mutableStateOf<Map<String, AppInfo>>(emptyMap())

    /** Always starts locked, and locks again when the screen goes off or after the auto-lock time. */
    var unlocked by mutableStateOf(false)
    /** With "Notes need the PIN": the notes alone were unlocked; any locking ends it. */
    var notesUnlocked by mutableStateOf(false)
    /** Whether this launcher is the phone's Home app now; set by the activity. */
    var isDefaultHome by mutableStateOf(true)
    var emergency by mutableStateOf<EmergencyUi>(EmergencyUi.Idle)
    /** A "Call a number" hold action counting down before it rings; null when none is. */
    var holdCall by mutableStateOf<HoldCallUi?>(null)
    /** Counts presses of the Home button while this launcher is already showing; the screen goes back to its start. */
    var homePresses by mutableStateOf(0)
    var lastTouch by mutableLongStateOf(System.currentTimeMillis())

    /** Without the free layout every page is kept packed to the top. */
    private fun fit(l: Layout) = l.withBigRepaired().let { if (settings.customLayout) it else it.compacted() }.withoutStrayColors().withoutStrayHolds().withoutEmptyExtraPages()

    fun change(new: Layout) {
        val fitted = fit(new)
        if (fitted != layout) {
            layout = fitted
            saveLayout(fitted)
        }
    }

    fun change(new: Settings) {
        settings = new
        saveSettings(new)
        ensureTextLayout()
        change(layout) // switching the free layout off packs the pages
    }

    /** A fresh layout: an empty screen ready for apps, with the phone's own apps in the bottom bar. */
    var defaultFor: (Map<String, AppInfo>) -> Layout = { layout }

    /** The other fresh layout: every app A to Z, with the phone's own apps in the bottom bar. */
    var filledFor: (Map<String, AppInfo>) -> Layout = { layout }

    fun resetLayout() = change(layout.startedAgainAs(defaultFor(apps)))

    fun resetLayoutFilled() = change(layout.startedAgainAs(filledFor(apps)))

    fun touched() {
        lastTouch = System.currentTimeMillis()
    }
}

/** Where the Emergency button is: nothing, or counting down to the call. */
sealed interface EmergencyUi {
    data object Idle : EmergencyUi
    data class Countdown(val secondsLeft: Int) : EmergencyUi
}

/** A hold-action call about to ring [number] in [secondsLeft] seconds, unless cancelled. */
data class HoldCallUi(val number: String, val secondsLeft: Int)

/** Seconds a hold-action call waits, so a thumb resting on the tile can still cancel it. */
const val HOLD_CALL_SECONDS = 3
