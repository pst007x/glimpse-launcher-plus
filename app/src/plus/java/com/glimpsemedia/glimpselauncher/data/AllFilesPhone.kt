package com.glimpsemedia.glimpselauncher.data

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.WalkResult
import com.glimpsemedia.glimpselauncher.model.walk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * The whole-phone search of Glimpse Launcher Plus: every file and folder name in the phone's own storage, read with
 * Android's All files access (Android 11+) or file access (8 to 10). Names only, never what is inside the files.
 * The walk runs on the IO threads, in the activity's lifetime (see [WalkCache]).
 */
class AllFilesPhone(private val activity: ComponentActivity) : AllFiles {
    private val storage: File = Environment.getExternalStorageDirectory()
    private val mime: (String) -> String? = { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
    private fun tree() = FileTree(storage, mime)
    // Runs in the activity's lifetime, so closing Search does not stop a long first walk (see WalkCache).
    private val walks = WalkCache(activity.lifecycleScope, now = { System.currentTimeMillis() }) {
        withContext(Dispatchers.IO) {
            val job = coroutineContext
            val r = walk(listOf(phoneRoot(storage)), tree(), PHONE_MAX_ENTRIES, PHONE_MAX_DEPTH, cancelled = { !job.isActive })
            // Packed down at once: the full entries are only needed for the few results shown.
            WalkResult(PhoneNames.of(r.entries, storage.absolutePath, mime), r.cut, r.failedRoots)
        }
    }

    override val available = true
    override val usesSettingsPage = Build.VERSION.SDK_INT >= 30
    override var permissionVersion by mutableIntStateOf(0)
        private set

    // Android 8 to 10 ask the usual question; registered now, while the activity is being made.
    // Refused at once with no question (the person once chose Don't ask again): open App info instead, with a note.
    private val asker = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            if (start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.packageName))))
                Toast.makeText(activity, com.glimpsemedia.glimpselauncher.ui.ALL_FILES_APP_INFO_NOTE, Toast.LENGTH_LONG).show()
        }
        permissionVersion++
    }

    override fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    override fun request() {
        if (Build.VERSION.SDK_INT < 30) { try { asker.launch(Manifest.permission.READ_EXTERNAL_STORAGE) } catch (_: Exception) { }; return }
        val forApp = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + activity.packageName))
        if (!start(forApp)) start(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }

    override fun onResume() { permissionVersion++ }

    override fun last(): WalkResult? = walks.last()

    override suspend fun current(): WalkResult = walks.current()

    override fun opened() = walks.opened()
    override fun closed() = walks.closed()

    override fun owns(entry: Entry): Boolean = entry.root == storage.absolutePath

    override fun open(entry: Entry): Boolean {
        val uri = try { FileProvider.getUriForFile(activity, activity.packageName + ".files", File(entry.id)) } catch (_: Exception) { return false }
        return start(Intent(Intent.ACTION_VIEW).setDataAndType(uri, entry.mime.ifEmpty { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    override fun openFolder(entry: Entry): Boolean {
        val uri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, externalDocId(tree().relative(File(entry.id))))
        return start(Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    override suspend fun childrenOf(entry: Entry): List<Entry>? = withContext(Dispatchers.IO) {
        try { tree().children(entry.id) } catch (_: Exception) { null }
    }

    private fun start(intent: Intent): Boolean = try {
        activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

    private companion object {
        const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    }
}
