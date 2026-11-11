package com.glimpsemedia.glimpselauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.glimpsemedia.glimpselauncher.data.AppInfo
import com.glimpsemedia.glimpselauncher.data.BugLogHeader
import com.glimpsemedia.glimpselauncher.data.BugLog
import com.glimpsemedia.glimpselauncher.data.RestoreResult
import com.glimpsemedia.glimpselauncher.data.BackupTooBig
import com.glimpsemedia.glimpselauncher.data.PrefValue
import com.glimpsemedia.glimpselauncher.data.BackupStore
import com.glimpsemedia.glimpselauncher.data.BackupRead
import com.glimpsemedia.glimpselauncher.data.BackupManifest
import com.glimpsemedia.glimpselauncher.data.BackupContents
import com.glimpsemedia.glimpselauncher.data.Backup
import com.glimpsemedia.glimpselauncher.data.withTextModeToggled
import com.glimpsemedia.glimpselauncher.data.AppRepository
import com.glimpsemedia.glimpselauncher.data.LayoutStore
import com.glimpsemedia.glimpselauncher.data.NotesStore
import com.glimpsemedia.glimpselauncher.data.drawThemeWallpaper
import com.glimpsemedia.glimpselauncher.data.GlimpseWallpaper
import com.glimpsemedia.glimpselauncher.data.SAVED_THEMES_MAX
import com.glimpsemedia.glimpselauncher.data.SavedTheme
import com.glimpsemedia.glimpselauncher.data.SavedThemes
import com.glimpsemedia.glimpselauncher.data.SavedWallpaper
import com.glimpsemedia.glimpselauncher.data.ThemeStore
import com.glimpsemedia.glimpselauncher.data.look
import com.glimpsemedia.glimpselauncher.data.withLook
import com.glimpsemedia.glimpselauncher.ui.SavedThemeAction
import com.glimpsemedia.glimpselauncher.model.wallpaperPlan
import com.glimpsemedia.glimpselauncher.model.Notes
import com.glimpsemedia.glimpselauncher.model.changed
import com.glimpsemedia.glimpselauncher.model.displayTitle
import com.glimpsemedia.glimpselauncher.model.shareText
import com.glimpsemedia.glimpselauncher.ui.PictureRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.FileProvider
import android.content.ClipData
import java.io.File
import com.glimpsemedia.glimpselauncher.model.Contact
import com.glimpsemedia.glimpselauncher.data.SettingsStore
import com.glimpsemedia.glimpselauncher.data.WidgetSupport
import com.glimpsemedia.glimpselauncher.model.DefaultLayout
import com.glimpsemedia.glimpselauncher.model.Layout
import com.glimpsemedia.glimpselauncher.model.Page
import com.glimpsemedia.glimpselauncher.model.withoutMissing
import com.glimpsemedia.glimpselauncher.model.widgetsGone
import com.glimpsemedia.glimpselauncher.model.HoldAction
import com.glimpsemedia.glimpselauncher.model.PhoneAction
import com.glimpsemedia.glimpselauncher.model.note
import com.glimpsemedia.glimpselauncher.ui.EmergencyUi
import com.glimpsemedia.glimpselauncher.ui.HOLD_CALL_SECONDS
import com.glimpsemedia.glimpselauncher.ui.HoldCallUi
import com.glimpsemedia.glimpselauncher.ui.HomeScreen
import com.glimpsemedia.glimpselauncher.ui.wallpaperWasSet
import com.glimpsemedia.glimpselauncher.ui.PRIVACY_URL
import com.glimpsemedia.glimpselauncher.ui.SUPPORT_EMAIL
import com.glimpsemedia.glimpselauncher.ui.LauncherState
import com.glimpsemedia.glimpselauncher.ui.GlimpseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var repository: AppRepository
    private lateinit var state: LauncherState
    private lateinit var widgets: WidgetSupport
    // Made with the activity: its folder picker has to be registered before the activity starts.
    private val search = com.glimpsemedia.glimpselauncher.data.SearchSupport(this)
    /** App icons from icon packs, kept ready. */
    val icons by lazy {
        // Knows the chosen pack from the start, so icons are not first drawn without it.
        com.glimpsemedia.glimpselauncher.data.IconStore(this).also { if (::state.isInitialized) it.setPack(state.settings.iconPack) }
    }

    /** Locks again when the screen goes off, so it is never left open by accident. */
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            state.unlocked = false
            state.notesUnlocked = false
            cancelHoldCall()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = AppRepository(this)
        widgets = WidgetSupport(this)
        // A restore that was stopped part-way (power off, the phone closing the launcher) is undone first.
        BackupStore(filesDir).recoverIfInterrupted { writeSettingsPrefs(it) }
        val layoutStore = LayoutStore(this)
        val settingsStore = SettingsStore(this)
        notesStore = NotesStore(filesDir)
        themeStore = ThemeStore(filesDir)
        val textStore = com.glimpsemedia.glimpselauncher.data.TextModeStore(filesDir)
        // Until the apps are known the layout is a single empty page; it is replaced in refresh().
        state = LauncherState(
            layout = layoutStore.load() ?: Layout(listOf(Page()), emptyList()),
            settings = settingsStore.load(),
            // While a restore is running nothing may save over the files it is putting back.
            saveLayout = { if (!restoring) layoutStore.save(it) },
            saveSettings = { if (!restoring) settingsStore.save(it) },
            notes = notesStore.load(),
            saveNotes = ::queueNotesSave,
            textLayout = textStore.load(),
            saveTextLayout = { if (!restoring) textStore.save(it) },
        )
        state.savedThemes = themeStore.load()
        screenWalls = com.glimpsemedia.glimpselauncher.ui.ScreenWallpaperControl(
            store = com.glimpsemedia.glimpselauncher.data.ScreenWallpaperStore(filesDir),
            scope = lifecycleScope,
            resolver = contentResolver,
            pick = { done ->
                screenPictureDone = done
                try { pickScreenPicture.launch(arrayOf("image/*")) } catch (_: Exception) {
                    screenPictureDone = null; done(null); note("This phone has no way to pick a picture")
                }
            },
            screenSize = ::screenSize,
            showPositioner = { positioning.value = it },
            canSave = { !restoring },
            note = ::note,
        )
        val screenPicturesNow = screenWalls.pictures
        lifecycleScope.launch(Dispatchers.IO) { synchronized(screenWalls.store) { screenWalls.store.removeUnusedIfSafe(screenPicturesNow) } }
        val themesNow = state.savedThemes
        lifecycleScope.launch(Dispatchers.IO) { synchronized(themeStore) { themeStore.removeUnusedWallpapersIfSafe(themesNow) } }
        // A photo being taken when the launcher was closed by the phone: carry on waiting for it.
        photoFor = savedInstanceState?.getString("photoNote")?.let { id ->
            savedInstanceState.getString("photoFile")?.let { name -> id to File(notesStore.pictureDir, name) }
        }
        // Picture files no note mentions (a cancelled photo, a crash before saving) are cleared away,
        // except while a photo is still on its way.
        val loaded = state.notes
        if (photoFor == null) lifecycleScope.launch(Dispatchers.IO) { synchronized(notesStore) { notesStore.removeOrphansIfSafe(loaded) } }
        hadSavedLayout = layoutStore.load() != null
        // Started by the Text mode switch widget (not a restart of an earlier start).
        if (savedInstanceState == null && intent?.action == TOGGLE_TEXT_MODE) toggleTextMode()
        watchForCrashes()
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        try { getSystemService(CameraManager::class.java)?.registerTorchCallback(torchWatch, null) } catch (_: Exception) {}
        setContent {
            // Full screen hides the phone's top bar; a swipe down from the top edge shows it for a moment.
            val fullScreen = state.settings.fullScreen
            androidx.compose.runtime.LaunchedEffect(fullScreen) { applyFullScreen(fullScreen) }
            // Placed Text mode switches show the mode in the theme's colours.
            val s = state.settings
            androidx.compose.runtime.LaunchedEffect(s.textMode, s.tileColor, s.barColor, s.barShade, s.darkTheme, s.highlightColor, s.highlightShade) {
                TextModeSwitchWidget.refresh(this@MainActivity, s)
            }
            GlimpseTheme {
                // App icons come from the chosen icon pack (or none), the same everywhere.
                androidx.compose.runtime.LaunchedEffect(s.iconPack) { icons.setPack(s.iconPack); icons.tidyCache() }
                // At midnight calendar icons turn to the new day.
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    while (true) {
                        val now = java.util.Calendar.getInstance()
                        val next = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 5) }
                        kotlinx.coroutines.delay(next.timeInMillis - now.timeInMillis)
                        icons.checkDay()
                    }
                }
                // An app uninstalled: its own icon choice (and picture) goes too.
                androidx.compose.runtime.LaunchedEffect(state.apps) { if (state.apps.isNotEmpty()) icons.prune(state.apps.keys) }
                androidx.compose.runtime.CompositionLocalProvider(
                    com.glimpsemedia.glimpselauncher.ui.LocalIconStore provides icons,
                    com.glimpsemedia.glimpselauncher.ui.LocalMenuTextScale provides com.glimpsemedia.glimpselauncher.ui.menuTextScale(s.menuTextStep),
                    com.glimpsemedia.glimpselauncher.ui.LocalScreenWallpapers provides screenWalls,
                ) {
                HomeScreen(
                    state = state,
                    onLaunch = repository::launch,
                    onAppInfo = ::openAppInfo,
                    onUninstall = ::uninstall,
                    onNotifications = ::openNotifications,
                    onConfirmPhoneLock = ::confirmPhoneLock,
                    onWallpaper = ::openWallpaperChooser,
                    onMakeHome = ::requestHome,
                    onEmergencyTap = ::emergencyTapped,
                    onEmergencySetup = { withCallPermission { } },
                    onEmergencyCallNow = ::callNow,
                    onEmergencyCancel = ::cancelCountdown,
                    onHoldCallNow = ::holdCallNow,
                    onHoldCallCancel = ::cancelHoldCall,
                    onPictureAction = ::pictureAction,
                    notePicture = { name -> File(notesStore.pictureDir, name) },
                    onThemeWallpaper = ::setThemeWallpaper,
                    onSavedTheme = ::savedThemeAction,
                    onDeletePictures = { names -> lifecycleScope.launch(Dispatchers.IO) { synchronized(notesStore) { notesStore.deletePictures(names) } } },
                    onEmail = ::reportBug,
                    onWebsite = ::openWebsite,
                    onPrivacy = ::openPrivacy,
                    onCoffee = ::openCoffee,
                    widgets = widgets,
                    search = search,
                    versionName = versionName(),
                    onHoldAction = ::runHoldAction,
                    onToggleTextMode = ::toggleTextMode,
                    onBackup = { createBackup.launch(Backup.backupFileName(System.currentTimeMillis(), java.time.ZoneId.systemDefault())) },
                    // Zips are labelled in several ways (a download, an email): any file can be picked, and it is checked anyway.
                    onRestorePick = { openBackup.launch(arrayOf("*/*")) },
                    restoreOffer = restoreOffer.value,
                    onRestoreAnswer = ::restoreAnswer,
                    restoring = restoringShown.value,
                    onSuggest = ::suggestFeature,
                    onEmailBugLog = ::emailBugLog,
                    onClearBugLog = ::clearBugLog,
                    onSystemSettings = { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {} },
                )
                positioning.value?.let { com.glimpsemedia.glimpselauncher.ui.PicturePositioner(it) { positioning.value = null } }
            }
            }
        }
    }

    private var hadSavedLayout = false

    private lateinit var notesStore: NotesStore
    private var notesSaveJob: Job? = null

    /** Notes are saved 0.4 s after the last change (typing), off the main thread; one writer at a time. */
    private fun queueNotesSave(notes: Notes) {
        notesSaveJob?.cancel()
        notesSaveJob = lifecycleScope.launch {
            delay(400)
            withContext(Dispatchers.IO) { synchronized(notesStore) { if (!restoring) notesStore.save(notes) } }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoFor?.let { (id, file) -> outState.putString("photoNote", id); outState.putString("photoFile", file.name) }
    }

    /** The note a photo is being taken for, and the file the camera app writes it to. */
    private var photoFor: Pair<String, File>? = null
    private var galleryFor: String? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val (id, file) = photoFor ?: return@registerForActivityResult
        photoFor = null
        if (taken && file.length() > 0) addPicture(id, file.name) else file.delete()
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = galleryFor
        galleryFor = null
        if (uri == null || id == null) return@registerForActivityResult
        lifecycleScope.launch {
            // A copy in the launcher's own storage, so the note keeps it when the original is deleted.
            val name = withContext(Dispatchers.IO) {
                val file = notesStore.newPictureFile()
                try {
                    contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                    file.name
                } catch (_: Exception) {
                    file.delete()
                    null
                }
            }
            if (name == null) note("That picture could not be added") else addPicture(id, name)
        }
    }

    private fun addPicture(noteId: String, name: String) {
        state.changeNotes(state.notes.changed(noteId) { it.copy(pictures = it.pictures + name) })
    }

    private fun pictureUri(name: String): Uri = FileProvider.getUriForFile(this, "$packageName.files", File(notesStore.pictureDir, name))

    private fun pictureAction(request: PictureRequest) {
        when (request) {
            is PictureRequest.TakePhoto -> {
                val file = notesStore.newPictureFile()
                photoFor = request.noteId to file
                try {
                    takePhoto.launch(pictureUri(file.name))
                } catch (_: ActivityNotFoundException) {
                    photoFor = null
                    note("No camera app on this phone can do that")
                }
            }
            is PictureRequest.FromGallery -> {
                galleryFor = request.noteId
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            is PictureRequest.Share -> shareNote(request.noteId)
            is PictureRequest.SharePicture -> {
                val uri = pictureUri(request.name)
                val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("", uri) }
                start(Intent.createChooser(send, "Share the picture"))
            }
        }
    }

    /** The phone's share list: the title, the lines as ☐/☑, and the pictures (each one readable only by the app chosen). */
    private fun shareNote(id: String) {
        val n = state.notes.list.firstOrNull { it.id == id } ?: return
        val uris = n.pictures.filter { File(notesStore.pictureDir, it).exists() }.map { pictureUri(it) }
        val send = if (uris.isEmpty()) {
            Intent(Intent.ACTION_SEND).setType("text/plain")
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/jpeg")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .apply { clipData = ClipData.newRawUri("", uris[0]).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } } }
        }
        send.putExtra(Intent.EXTRA_TEXT, n.shareText()).putExtra(Intent.EXTRA_SUBJECT, n.displayTitle())
        start(Intent.createChooser(send, "Share the note"))
    }

    /** Saves a change still waiting, now (the launcher is leaving the screen). */
    private fun flushNotes() {
        val waiting = notesSaveJob ?: return
        if (waiting.isActive) {
            waiting.cancel()
            synchronized(notesStore) { notesStore.save(state.notes) }
        }
        notesSaveJob = null
    }

    private fun applyFullScreen(on: Boolean) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (on) controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        else controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Coming back from an app can bring the bar back: hide it again.
        if (hasFocus && ::state.isInitialized && state.settings.fullScreen) applyFullScreen(true)
    }

    override fun onStart() {
        super.onStart()
        widgets.start()
    }

    override fun onStop() {
        // Leaving the screen closes an open text-mode folder.
        state.textFolderOpen = null
        flushNotes()
        widgets.stop()
        super.onStop()
    }

    @Deprecated("The widget host reports back this way")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.handleResult(requestCode, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == TOGGLE_TEXT_MODE) toggleTextMode()
        else if (intent.hasCategory(Intent.CATEGORY_HOME)) state.homePresses++
    }

    /** The Text mode switch widget: flips only the mode (locked or not), then shows the first screen like Home. */
    private fun toggleTextMode() {
        state.change(state.settings.withTextModeToggled())
        state.homePresses++
    }

    override fun onDestroy() {
        try { getSystemService(CameraManager::class.java)?.unregisterTorchCallback(torchWatch) } catch (_: Exception) {}
        unregisterReceiver(screenOff)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // A new day: calendar icons from the icon pack show today's date.
        icons.checkDay()
        // A pack's own Apply, agreed to in ApplyIconPackActivity: used only if it is an installed icon pack.
        com.glimpsemedia.glimpselauncher.data.PendingIconPack.take { p -> icons.packs.installed().any { it.pkg == p } }
            ?.let { state.change(state.settings.copy(iconPack = it)) }
        // Back from Android's All files access page (Plus): read its answer again.
        search.allFiles.onResume()
        state.isDefaultHome = isDefaultHome()
        refresh()
    }

    private fun isDefaultHome(): Boolean {
        val home = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY,
        )
        return home?.activityInfo?.packageName == packageName
    }

    private val homeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        state.isDefaultHome = isDefaultHome()
    }

    /** Asks the phone to make this the Home app: a one-touch question on Android 10 and newer, Settings before. */
    private fun requestHome() {
        try {
            val roles = if (Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java) else null
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                homeRole.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
            } else {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Looks at what is installed again; an uninstalled app drops off the screen. */
    private fun refresh() {
        lifecycleScope.launch {
            val installed: List<AppInfo> = withContext(Dispatchers.IO) { repository.loadApps() }
            if (installed.isEmpty()) return@launch  // the phone did not answer: change nothing
            state.apps = installed.associateBy { it.key }
            state.defaultFor = { DefaultLayout.build(installed.map { it.key }, repository.preferredDockKeys(installed)) }
            state.filledFor = { DefaultLayout.buildFilled(installed.map { it.key }, repository.preferredDockKeys(installed)) }
            state.startingTextLayoutFor = {
                val (phone, messages, gallery, browser) = repository.textModeDefaults(installed)
                com.glimpsemedia.glimpselauncher.model.startingTextLayout(phone, messages, gallery, browser)
            }
            state.textLayout?.let { state.changeTextLayout(it.withoutMissing(state.apps.keys)) }
            // Text-mode widgets whose app is gone (or that the widget host forgot) are dropped and freed.
            state.textLayout?.let { t ->
                val gone = t.widgetsGone { widgets.info(it) != null }
                if (gone.isNotEmpty()) {
                    gone.forEach(widgets::delete)
                    state.changeTextLayout(t.copy(widgets = t.widgets.filter { it.id !in gone }))
                }
            }
            state.ensureTextLayout()
            if (!hadSavedLayout) {
                hadSavedLayout = true
                state.change(state.defaultFor(state.apps))
            } else {
                state.change(state.layout.withoutMissing(state.apps.keys))
            }
        }
    }

    private var afterPermissions: (() -> Unit)? = null

    private var refusedText = EMERGENCY_NEEDS_CALLS

    private val askCallPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val next = afterPermissions
        afterPermissions = null
        if (granted.values.all { it }) next?.invoke()
        else Toast.makeText(this, refusedText, Toast.LENGTH_LONG).show()
    }

    /** Runs [then] once the phone has allowed calls; asks the person first if it has not, and says [refused] if they say no. */
    private fun withCallPermission(refused: String = EMERGENCY_NEEDS_CALLS, then: () -> Unit) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            then()
        } else {
            afterPermissions = then
            refusedText = refused
            askCallPermissions.launch(arrayOf(Manifest.permission.CALL_PHONE))
        }
    }

    /** The torch's state, as the phone reports it (another app may switch it too). */
    private var torchOn = false
    private val torchWatch = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
    }

    private fun note(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    /** Starts [intent], or says no app can; true if it started. */
    private fun start(intent: Intent): Boolean = try {
        startActivity(intent); true
    } catch (_: ActivityNotFoundException) {
        note("No app on this phone can do that"); false
    } catch (_: SecurityException) {
        note("No app on this phone can do that"); false
    }

    /** A locked tile was held: runs its action with a short note. Every failure is a plain message. */
    private fun runHoldAction(action: HoldAction) {
        val label = (action as? HoldAction.OpenApp)?.let { state.apps[it.key]?.label }
        fun done() = note(action.note(label, torchOn))
        when (action) {
            is HoldAction.OpenApp -> {
                val app = state.apps[action.key]
                if (app == null) note("That app is no longer on the phone") else { repository.launch(app); done() }
            }
            is HoldAction.Web -> if (start(Intent(Intent.ACTION_VIEW, Uri.parse(action.url)))) done()
            // A short countdown with Cancel first: a thumb resting on the tile must not ring anyone.
            is HoldAction.Call -> withCallPermission("Calls need the phone permission. Allow it in the phone's Settings > Apps.") {
                startHoldCall(action.number)
            }
            is HoldAction.Text -> if (start(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", action.number, null)))) done()
            HoldAction.Notes -> { state.openNotes++; done() }
            is HoldAction.Phone -> when (action.what) {
                PhoneAction.Torch -> switchTorch()
                PhoneAction.DoNotDisturb -> {
                    // Not every phone has its own Do not disturb page; it lives under Sound on the others.
                    val ok = try { startActivity(Intent("android.settings.ZEN_MODE_SETTINGS")); true } catch (_: Exception) {
                        start(Intent(Settings.ACTION_SOUND_SETTINGS))
                    }
                    if (ok) done()
                }
                else -> if (start(Intent(phoneIntent(action.what)))) done()
            }
        }
    }

    private fun phoneIntent(p: PhoneAction): String = when (p) {
        PhoneAction.Camera -> MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
        PhoneAction.Alarms -> AlarmClock.ACTION_SHOW_ALARMS
        PhoneAction.Timer -> AlarmClock.ACTION_SHOW_TIMERS
        PhoneAction.Wifi -> Settings.ACTION_WIFI_SETTINGS
        PhoneAction.Bluetooth -> Settings.ACTION_BLUETOOTH_SETTINGS
        PhoneAction.Sound -> Settings.ACTION_SOUND_SETTINGS
        PhoneAction.Display -> Settings.ACTION_DISPLAY_SETTINGS
        PhoneAction.Battery -> Intent.ACTION_POWER_USAGE_SUMMARY
        PhoneAction.Location -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
        PhoneAction.DoNotDisturb -> Settings.ACTION_SOUND_SETTINGS
        PhoneAction.Torch -> error("the torch is switched, not opened")
    }

    private var holdCallJob: Job? = null

    /** Counts down [HOLD_CALL_SECONDS], then calls [number]. Cancel, Back, a touch outside or the screen going off stop it. */
    private fun startHoldCall(number: String) {
        if (holdCallJob != null) return
        holdCallJob = lifecycleScope.launch {
            try {
                for (left in HOLD_CALL_SECONDS downTo 1) {
                    state.holdCall = HoldCallUi(number, left)
                    delay(1000)
                }
                state.holdCall = null
                callHoldNumber(number)
            } finally {
                holdCallJob = null
            }
        }
    }

    private fun cancelHoldCall() {
        holdCallJob?.cancel()
        state.holdCall = null
    }

    private fun holdCallNow() {
        val number = state.holdCall?.number
        cancelHoldCall()
        if (number != null) callHoldNumber(number)
    }

    private fun callHoldNumber(number: String) {
        if (start(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))) note(HoldAction.Call(number).note(null, false))
    }

    /** Switches the torch on the first camera with a flash. No permission is needed. */
    private fun switchTorch() {
        try {
            val cameras = getSystemService(CameraManager::class.java)
            val id = cameras.cameraIdList.firstOrNull { cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            if (id == null) { note("The torch can't be used right now"); return }
            val on = !torchOn
            cameras.setTorchMode(id, on)
            torchOn = on
            note(HoldAction.Phone(PhoneAction.Torch).note(null, on))
        } catch (_: Exception) {
            note("The torch can't be used right now")
        }
    }

    private fun emergencyTapped() {
        val s = state.settings
        when {
            !s.emergencyEnabled -> Toast.makeText(this, "The Emergency button is switched off. Turn it on in Settings.", Toast.LENGTH_LONG).show()
            s.emergencyContact?.isUsable != true -> Toast.makeText(this, "No phone number yet. Add one in Settings.", Toast.LENGTH_LONG).show()
            else -> withCallPermission { startCountdown() }
        }
    }

    private var emergencyJob: Job? = null

    /** Counts down, then rings the number. Only the Cancel button stops it; the screen stays on meanwhile. */
    private fun startCountdown() {
        if (emergencyJob != null) return
        val contact = state.settings.emergencyContact?.takeIf { it.isUsable } ?: return
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        emergencyJob = lifecycleScope.launch {
            try {
                for (left in state.settings.emergencyCountdown downTo 1) {
                    state.emergency = EmergencyUi.Countdown(left)
                    delay(1000)
                }
                state.emergency = EmergencyUi.Idle
                placeCall(contact)
            } finally {
                emergencyJob = null
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun cancelCountdown() {
        emergencyJob?.cancel()
        state.emergency = EmergencyUi.Idle
    }

    private fun callNow() {
        val contact = state.settings.emergencyContact?.takeIf { it.isUsable }
        emergencyJob?.cancel()
        state.emergency = EmergencyUi.Idle
        if (contact != null) placeCall(contact)
    }

    /** One call, made by the phone itself (this also works with the screen off). Nothing about it is watched. */
    @SuppressLint("MissingPermission")
    private fun placeCall(contact: Contact) {
        try {
            getSystemService(TelecomManager::class.java)
                .placeCall(Uri.fromParts("tel", contact.number.filter { it.isDigit() || it == '+' }, null), Bundle())
        } catch (_: Exception) {
            Toast.makeText(this, "The call could not be started.", Toast.LENGTH_LONG).show()
        }
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    // ---- Settings > Help and support > Report bugs ------------------------------------------------------------------------

    /** The bug log: written only while Settings > Help and support > Report bugs > Bug log is on. */
    private val bugLog by lazy {
        BugLog(File(filesDir, "logs/bug-log.txt"), BugLogHeader(versionName(), "${Build.MANUFACTURER} ${Build.MODEL}", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"))
    }

    /** Records [t] in the bug log when the person has switched it on. */
    private fun logError(where: String, t: Throwable?) {
        if (::state.isInitialized && state.settings.bugLog) runCatching { bugLog.record(where, t) }
    }

    /** A crash is written to the bug log (when it is on) before the phone's usual crash handling. */
    private fun watchForCrashes() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            logError("Crash (${thread.name})", e)
            previous?.uncaughtException(thread, e)
        }
    }

    /**
     * Opens the phone's default email app (not the share list), addressed to support, with [attachLog] adding the
     * bug log when there is one. A send aimed only at email apps: the "mailto:" selector keeps it to them.
     */
    private fun email(subject: String, body: String, attachLog: Boolean = false) {
        val log = File(filesDir, "logs/bug-log.txt")
        val attach = attachLog && bugLog.hasEntries()
        val intent = if (attach) {
            val uri = FileProvider.getUriForFile(this, "$packageName.files", log)
            Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .apply { clipData = ClipData.newRawUri("", uri); selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")) }
        } else {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL)).putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // No email app is on (or installed): offer the share list instead, with everything filled in.
            try {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL)).putExtra(Intent.EXTRA_SUBJECT, subject)
                    .putExtra(Intent.EXTRA_TEXT, "To: $SUPPORT_EMAIL\n\n$body")
                if (attach) {
                    val uri = FileProvider.getUriForFile(this, "$packageName.files", log)
                    share.putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("", uri) }
                }
                startActivity(Intent.createChooser(share, "No email app is on: send it with").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (_: Exception) {
                Toast.makeText(this, "Write to $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun phoneLine() = "${com.glimpsemedia.glimpselauncher.model.appName(BuildConfig.PLUS)} ${versionName()} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"

    /** A bug report; with the bug log on and something in it, the log goes with it. */
    private fun reportBug() {
        val withLog = state.settings.bugLog && bugLog.hasEntries()
        email(
            "Bug report: ${com.glimpsemedia.glimpselauncher.model.appName(BuildConfig.PLUS)} ${versionName()}",
            "What were you doing?\n\nWhat did you expect?\n\nWhat happened instead?\n\n" +
                (if (withLog) "The bug log is attached.\n\n" else "") + phoneLine() + "\n",
            attachLog = withLog,
        )
    }

    private fun suggestFeature() = email("Feature suggestion: ${com.glimpsemedia.glimpselauncher.model.appName(BuildConfig.PLUS)}", "My idea:\n\n${phoneLine()}\n")

    /** The bug log on its own, attached, in the default email app. */
    private fun emailBugLog() {
        if (!bugLog.hasEntries()) {
            Toast.makeText(this, if (state.settings.bugLog) "The bug log is empty: nothing has gone wrong yet" else "The bug log is off: switch it on first", Toast.LENGTH_LONG).show()
            return
        }
        email("Bug log: ${com.glimpsemedia.glimpselauncher.model.appName(BuildConfig.PLUS)} ${versionName()}", "The bug log is attached.\n\nWhat went wrong?\n\n${phoneLine()}\n", attachLog = true)
    }

    private fun clearBugLog() {
        bugLog.clear()
        Toast.makeText(this, "The bug log is empty", Toast.LENGTH_SHORT).show()
    }

    private fun writeToSupport() {
        try {
            startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                    .putExtra(Intent.EXTRA_SUBJECT, "Glimpse Launcher ${versionName()}"),
            )
        } catch (_: Exception) {
            Toast.makeText(this, "Write to $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPrivacy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
        } catch (_: Exception) {
            Toast.makeText(this, "Open $PRIVACY_URL in a web browser", Toast.LENGTH_LONG).show()
        }
    }

    private fun openWebsite() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glimpse-media.com")))
        } catch (_: Exception) {
            Toast.makeText(this, "Open glimpse-media.com in a web browser", Toast.LENGTH_LONG).show()
        }
    }

    private fun openCoffee() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/salvatoreturone")))
        } catch (_: Exception) {
            Toast.makeText(this, "Open buymeacoffee.com/salvatoreturone in a web browser", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppInfo(app: AppInfo) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private var phoneLockOk: (() -> Unit)? = null
    private val phoneLock = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) phoneLockOk?.invoke()
        phoneLockOk = null
    }

    /** The phone's own PIN, pattern or fingerprint, as the way back in when the padlock PIN is forgotten. */
    @Suppress("DEPRECATION")
    private fun confirmPhoneLock(onOk: () -> Unit) {
        val intent = getSystemService(android.app.KeyguardManager::class.java)
            ?.createConfirmDeviceCredentialIntent(com.glimpsemedia.glimpselauncher.model.appName(BuildConfig.PLUS), "Confirm it's you to set a new padlock PIN")
        if (intent == null) {
            Toast.makeText(this, "This phone has no screen lock to confirm with", Toast.LENGTH_LONG).show()
            return
        }
        phoneLockOk = onOk
        phoneLock.launch(intent)
    }

    /** Pulls down the notification shade. Android has no public call for it, so this asks the status bar by name. */
    @android.annotation.SuppressLint("WrongConstant")
    private fun openNotifications() {
        try {
            val bar = getSystemService("statusbar") ?: return
            bar.javaClass.getMethod("expandNotificationsPanel").invoke(bar)
        } catch (_: Throwable) {
        }
    }

    /** The phone's own uninstall dialog; it refuses system apps itself. The tile goes when the list reloads on return. */
    private fun uninstall(app: AppInfo) {
        try {
            startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.component.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "This app can't be uninstalled from here", Toast.LENGTH_LONG).show()
        }
    }

    /** The system's file browser (not the photo picker), so Downloads and every other folder can be opened. */
    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) positionWallpaper(uri)
    }

    /** "Position the picture" while it is open (the default wallpaper, or a screen's own picture). */
    private val positioning = androidx.compose.runtime.mutableStateOf<com.glimpsemedia.glimpselauncher.ui.PositionRequest?>(null)
    private lateinit var screenWalls: com.glimpsemedia.glimpselauncher.ui.ScreenWallpaperControl
    /** Waiting for a picture for a screen of its own (gone if the phone closed the launcher meanwhile). */
    private var screenPictureDone: ((Uri?) -> Unit)? = null
    private val pickScreenPicture = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        screenPictureDone.also { screenPictureDone = null }?.invoke(uri)
    }

    /** The whole screen in pixels, as wallpapers are made for it. */
    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= 30) windowManager.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        else resources.displayMetrics.let { it.widthPixels to it.heightPixels }

    /** Change the wallpaper: the picture is placed first, then the screen question, then it is set at exactly the screen size. */
    private fun positionWallpaper(uri: Uri) {
        lifecycleScope.launch {
            val (sw, sh) = screenSize()
            val picture = withContext(Dispatchers.IO) { com.glimpsemedia.glimpselauncher.data.PictureFiles.decodeUpright(contentResolver, uri, minOf(3072, 2 * maxOf(sw, sh))) }
            if (picture == null) { Toast.makeText(this@MainActivity, "That picture could not be used", Toast.LENGTH_LONG).show(); return@launch }
            positioning.value = com.glimpsemedia.glimpselauncher.ui.PositionRequest(
                picture, sw, sh, com.glimpsemedia.glimpselauncher.model.PicturePlace(),
                onUse = { place -> askWhichScreen { flags -> setWallpaper(picture, sw, sh, place, flags) } },
                onCancel = {},
            )
        }
    }

    private fun openWallpaperChooser() {
        try {
            pickImage.launch(arrayOf("image/*"))
        } catch (_: Exception) {
            Toast.makeText(this, "This phone has no way to pick a picture", Toast.LENGTH_LONG).show()
        }
    }

    /** "Use this picture for": the home screen, the lock screen or both; [apply] gets the wallpaper flags. */
    private fun askWhichScreen(apply: (Int) -> Unit) {
        val choices = arrayOf("Home screen", "Lock screen", "Both")
        AlertDialog.Builder(this)
            .setTitle("Use this picture for")
            .setItems(choices) { _, which ->
                val flags = when (which) {
                    0 -> WallpaperManager.FLAG_SYSTEM
                    1 -> WallpaperManager.FLAG_LOCK
                    else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                apply(flags)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Draws a theme wallpaper at the full screen size and sets it where the person chooses. */
    private fun setThemeWallpaper(c1: Int, c2: Int, c3: Int, c4: Int, dark: Boolean) {
        askWhichScreen { flags ->
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { applyWallpaper(SavedWallpaper.Drawn(c1, c2, c3, c4, dark), flags) }
                Toast.makeText(this@MainActivity, if (ok) "The wallpaper has been changed" else "The wallpaper could not be set", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * A picture of the person's own, placed: the launcher keeps a copy of the part shown, at exactly the screen size
     * (so the phone cannot show it off centre, and a saved theme can put it back), then sets it.
     */
    private fun setWallpaper(picture: android.graphics.Bitmap, sw: Int, sh: Int, place: com.glimpsemedia.glimpselauncher.model.PicturePlace, flags: Int) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val copy = themeStore.newWallpaperFile()
                try {
                    val shown = com.glimpsemedia.glimpselauncher.data.PictureFiles.cut(picture, sw, sh, place)
                    com.glimpsemedia.glimpselauncher.data.PictureFiles.writeJpeg(shown, copy)
                    applyWallpaper(SavedWallpaper.Picture(copy.name), flags).also { if (!it) copy.delete() }
                } catch (_: Exception) {
                    copy.delete()
                    false
                } catch (_: OutOfMemoryError) {
                    copy.delete()
                    false
                }
            }
            Toast.makeText(this@MainActivity, if (ok) "The wallpaper has been changed" else "That picture could not be used", Toast.LENGTH_LONG).show()
        }
    }

    /** The phone's own ids for the home and lock wallpapers; a lock screen that shares the home one has the home id. */
    private fun wallpaperIds(wm: WallpaperManager): Pair<Int, Int> {
        val home = wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
        val lock = wm.getWallpaperId(WallpaperManager.FLAG_LOCK)
        return home to (if (lock < 0) home else lock)
    }

    /**
     * Sets [w] on the screens in [flags] (call off the main thread) and remembers it as the wallpaper Glimpse set there,
     * with the phone's id for it. False if it could not be set (unsupported, forbidden on a managed phone, a missing file).
     */
    private fun applyWallpaper(w: SavedWallpaper, flags: Int): Boolean = try {
        val wm = WallpaperManager.getInstance(this)
        val id = if (!(wm.isWallpaperSupported && wm.isSetWallpaperAllowed)) 0 else when (w) {
            is SavedWallpaper.Drawn -> {
                val bounds = screenSize()
                val bitmap = drawThemeWallpaper(wallpaperPlan(bounds.first, bounds.second, w.c1, w.c2, w.c3, w.c4, w.dark))
                try { wm.setBitmap(bitmap, null, true, flags) } finally { bitmap.recycle() }
            }
            is SavedWallpaper.Picture -> File(themeStore.wallpaperDir, w.file).takeIf { it.exists() }
                ?.inputStream()?.use { wm.setStream(it, null, true, flags) } ?: 0
        }
        val ok = wallpaperWasSet(wm.isWallpaperSupported, wm.isSetWallpaperAllowed, id)
        if (ok) {
            val (homeId, lockId) = wallpaperIds(wm)
            var t = state.savedThemes
            if (flags and WallpaperManager.FLAG_SYSTEM != 0) t = t.copy(lastHome = GlimpseWallpaper(w, homeId))
            if (flags and WallpaperManager.FLAG_LOCK != 0) t = t.copy(lastLock = GlimpseWallpaper(w, lockId))
            changeThemes(t)
        }
        ok
    } catch (_: Throwable) {
        false
    }

    private lateinit var themeStore: ThemeStore

    // ---- Settings > Your setup > Backup and restore ----------------------------------------------------------------------

    /** A backup that was read and checked, waiting for "Restore" or "Keep my current setup". */
    private val restoreOffer = androidx.compose.runtime.mutableStateOf<BackupContents?>(null)
    /** True from "Restore" until the launcher restarts: nothing is saved, and "Restoring…" is shown. */
    @Volatile private var restoring = false
    private val restoringShown = androidx.compose.runtime.mutableStateOf(false)
    private val staging get() = File(cacheDir, "restore-staging")

    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) saveBackup(uri)
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) readBackup(uri)
    }

    /** The settings exactly as stored, each with its type. */
    private fun prefsNow(): Map<String, PrefValue> =
        getSharedPreferences("settings", Context.MODE_PRIVATE).all.mapNotNull { (k, v) ->
            when (v) {
                is Boolean -> k to PrefValue.B(v)
                is Int -> k to PrefValue.I(v)
                is Long -> k to PrefValue.L(v)
                is Float -> k to PrefValue.F(v)
                is String -> k to PrefValue.S(v)
                else -> null
            }
        }.toMap()

    /** Replaces every setting with [m], keeping when the last backup was made and Search's folders. Throws if they could not be written. */
    private fun writeSettingsPrefs(m: Map<String, PrefValue>) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val e = prefs.edit().clear()
        Backup.restoredPrefs(m, prefsNow()).forEach { (k, v) ->
            when (v) {
                is PrefValue.B -> e.putBoolean(k, v.v)
                is PrefValue.I -> e.putInt(k, v.v)
                is PrefValue.L -> e.putLong(k, v.v)
                is PrefValue.F -> e.putFloat(k, v.v)
                is PrefValue.S -> e.putString(k, v.v)
            }
        }
        if (!e.commit()) throw java.io.IOException("settings not written")
    }

    private fun saveBackup(uri: Uri) {
        flushNotes()
        val now = System.currentTimeMillis()
        val base = BackupManifest(packageName, Backup.FORMAT, versionName(), now, 0, 0, false)
        lifecycleScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    synchronized(notesStore) {
                        val contents = BackupStore(filesDir).collect(base, prefsNow())
                        // "wt" empties the new file first; a few storage apps only take "w" (a new file is empty anyway).
                        val out = try { contentResolver.openOutputStream(uri, "wt") } catch (_: Exception) { contentResolver.openOutputStream(uri, "w") }
                        (out ?: throw java.io.IOException("no file")).use { Backup.write(it, contents) }
                    }
                    null
                } catch (t: Throwable) {
                    logError("Backup", t)
                    // A half-written file is not left behind looking like a backup.
                    runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) }
                    if (t is BackupTooBig) "The backup is too big: remove some note pictures or saved themes" else "The backup could not be saved"
                }
            }
            if (message == null) state.change(state.settings.copy(lastBackupMillis = now))
            Toast.makeText(this@MainActivity, message ?: "Backup saved", Toast.LENGTH_LONG).show()
        }
    }

    private fun readBackup(uri: Uri) {
        lifecycleScope.launch {
            val read = withContext(Dispatchers.IO) {
                try {
                    val r = contentResolver.openInputStream(uri)?.use { Backup.read(it, staging) } ?: BackupRead.NotABackup
                    if (r is BackupRead.Ok) Backup.forThisPhone(r.contents)
                    r
                } catch (t: Throwable) {
                    logError("Reading a backup", t)
                    staging.deleteRecursively()
                    BackupRead.NotABackup
                }
            }
            when (read) {
                is BackupRead.Ok -> restoreOffer.value = read.contents
                BackupRead.TooNew -> Toast.makeText(this@MainActivity, "This backup is from a newer Glimpse Launcher. Update the app first.", Toast.LENGTH_LONG).show()
                BackupRead.NotABackup -> Toast.makeText(this@MainActivity, "This isn't a Glimpse Launcher backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreAnswer(yes: Boolean) {
        val c = restoreOffer.value
        restoreOffer.value = null
        if (!yes || c == null) {
            lifecycleScope.launch(Dispatchers.IO) { staging.deleteRecursively() }
            return
        }
        // From here nothing saves over the files being restored, and "Restoring…" covers the screen.
        notesSaveJob?.cancel()
        restoring = true
        restoringShown.value = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    synchronized(notesStore) {
                        val current = prefsNow()
                        // A setting this app keeps with another type is left out: it could stop the launcher starting.
                        val settings = Backup.matchTypes(c.settings, current)
                        BackupStore(filesDir).restore(BackupContents(c.manifest, settings, c.files), writePrefs = { writeSettingsPrefs(it) }, currentPrefs = current)
                    }
                } catch (t: Throwable) {
                    logError("Restore", t)
                    RestoreResult.Failed
                } finally {
                    staging.deleteRecursively()
                }
            }
            if (result != RestoreResult.Done) {
                logError("Restore: $result", null)
                restoring = false
                restoringShown.value = false
                Toast.makeText(
                    this@MainActivity,
                    if (result == RestoreResult.NoRoom) "Not enough space to restore safely. Nothing was changed." else "The backup could not be restored. Nothing was changed.",
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            // The old widgets are in no restored layout: free them, then start again on the restored setup.
            (state.layout.widgets().map { it.id } + state.textLayout?.widgets.orEmpty().map { it.id }).forEach { widgets.delete(it) }
            startActivity(Intent(this@MainActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            Runtime.getRuntime().exit(0)
        }
    }

    /** The saved themes changed: shown at once, written off the main thread, one writer at a time. */
    private fun changeThemes(t: SavedThemes) {
        lifecycleScope.launch(Dispatchers.Main) { state.savedThemes = t }
        lifecycleScope.launch(Dispatchers.IO) {
            synchronized(themeStore) {
                if (!restoring) themeStore.save(t)
                themeStore.removeUnusedWallpapersIfSafe(t)
            }
        }
    }

    /** The wallpapers Glimpse set that are still showing, as the saved theme's own copies. */
    private fun captureWallpapers(): Pair<SavedWallpaper?, SavedWallpaper?> {
        val (homeId, lockId) = try { wallpaperIds(WallpaperManager.getInstance(this)) } catch (_: Throwable) { -2 to -2 }
        val (home, lock) = state.savedThemes.wallpapersNow(homeId, lockId)
        fun own(w: SavedWallpaper?): SavedWallpaper? = when (w) {
            is SavedWallpaper.Picture -> try {
                val copy = themeStore.newWallpaperFile()
                File(themeStore.wallpaperDir, w.file).copyTo(copy)
                SavedWallpaper.Picture(copy.name)
            } catch (_: Exception) { null }
            else -> w
        }
        val h = own(home)
        // One picture on both screens gets one copy.
        val l = if (lock != null && lock == home) h else own(lock)
        return h to l
    }

    private fun savedThemeAction(a: SavedThemeAction) {
        lifecycleScope.launch {
            when (a) {
                is SavedThemeAction.Save -> {
                    val (home, lock) = withContext(Dispatchers.IO) { captureWallpapers() }
                    val theme = SavedTheme("t" + System.currentTimeMillis(), a.name, state.settings.look(), home, lock)
                    changeThemes(state.savedThemes.copy(list = (state.savedThemes.list + theme).take(SAVED_THEMES_MAX)))
                    note(if (home == null && lock == null) "Saved without a wallpaper: this one wasn't set by Glimpse Launcher." else "Theme saved")
                }
                is SavedThemeAction.Update -> {
                    val (home, lock) = withContext(Dispatchers.IO) { captureWallpapers() }
                    changeThemes(state.savedThemes.copy(list = state.savedThemes.list.map {
                        if (it.id == a.id) it.copy(look = state.settings.look(), home = home, lock = lock) else it
                    }))
                    note(if (home == null && lock == null) "Updated without a wallpaper: this one wasn't set by Glimpse Launcher." else "Theme updated")
                }
                is SavedThemeAction.Rename -> changeThemes(state.savedThemes.copy(list = state.savedThemes.list.map { if (it.id == a.id) it.copy(name = a.name) else it }))
                is SavedThemeAction.Delete -> changeThemes(state.savedThemes.copy(list = state.savedThemes.list.filter { it.id != a.id }))
                is SavedThemeAction.Apply -> {
                    val t = a.theme
                    state.change(state.settings.withLook(t.look))
                    // Its wallpapers go back on the same screens (one call when both screens had the same one).
                    val ok = withContext(Dispatchers.IO) {
                        val both = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        when {
                            t.home != null && t.home == t.lock -> applyWallpaper(t.home, both)
                            else -> (t.home?.let { applyWallpaper(it, WallpaperManager.FLAG_SYSTEM) } ?: true) and
                                (t.lock?.let { applyWallpaper(it, WallpaperManager.FLAG_LOCK) } ?: true)
                        }
                    }
                    note(if (ok) "${t.name} applied" else "${t.name} applied; the wallpaper could not be set")
                }
            }
        }
    }
}

private const val EMERGENCY_NEEDS_CALLS = "The Emergency button needs permission to make phone calls."
