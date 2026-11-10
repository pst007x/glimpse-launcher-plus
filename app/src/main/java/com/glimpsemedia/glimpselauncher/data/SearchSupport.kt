package com.glimpsemedia.glimpselauncher.data

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.glimpsemedia.glimpselauncher.model.FolderLocation
import com.glimpsemedia.glimpselauncher.model.withPicked

/**
 * What Search needs from the phone: its saved lists, the folder index, Android's folder picker, and opening a file,
 * a folder or a web search in other apps. Made with the activity, before it starts (the picker must be registered then).
 */
class SearchSupport(private val activity: ComponentActivity) {
    val store by lazy { SearchStore(activity) }
    val index by lazy { FolderIndex(activity) }
    /** Every file and folder name on the phone: Glimpse Launcher Plus only (made now: it may register a permission question). */
    val allFiles: AllFiles = allFilesFor(activity)

    /** Goes up by one each time the folder list is saved here, so a Settings page showing it reads it again. */
    var foldersVersion by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    // The picked folder is saved straight away, not handed to whoever asked: if the phone is turned while the
    // picker is open, the activity (and whoever asked) is made again, and the answer must not be lost.
    private val picker = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { return@registerForActivityResult }
        saveFolders(store.folders().withPicked(FolderLocation(uri.toString(), folderName(uri))))
    }

    /** Opens Android's folder picker; a chosen folder is added to the saved list, with its grant kept. */
    fun pickFolder() {
        try { picker.launch(null) } catch (_: ActivityNotFoundException) { }
    }

    /** Saves the folder list and tells a page showing it. */
    fun saveFolders(l: List<FolderLocation>) { store.saveFolders(l); foldersVersion++ }

    /** Gives up the grant for a folder taken off the list. */
    fun release(loc: FolderLocation) {
        try { activity.contentResolver.releasePersistableUriPermission(Uri.parse(loc.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
    }

    /** Whether the phone still lets Glimpse read [loc] (the folder may have gone, or the grant been taken back). */
    fun isAvailable(loc: FolderLocation): Boolean =
        activity.contentResolver.persistedUriPermissions.any { it.uri.toString() == loc.uri && it.isReadPermission }

    /** The picked folder's own id in its tree, as found entries carry it in [com.glimpsemedia.glimpselauncher.model.Entry.root]. */
    fun rootIdOf(loc: FolderLocation): String? = try { DocumentsContract.getTreeDocumentId(Uri.parse(loc.uri)) } catch (_: Exception) { null }

    /** Opens a file in the app the phone uses for its kind; false when no app can. */
    fun openDocument(uri: Uri, mime: String): Boolean = start(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime.ifEmpty { "*/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    /** Opens a folder in the phone's Files app; false when no app takes it. */
    fun openFolder(uri: Uri): Boolean = start(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    /** Opens a web search in the phone's browser. */
    fun openWeb(url: String): Boolean = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun start(intent: Intent): Boolean = try {
        activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

    private fun folderName(tree: Uri): String {
        val id = try { DocumentsContract.getTreeDocumentId(tree) } catch (_: Exception) { null }
        val shown = try {
            activity.contentResolver.query(
                DocumentsContract.buildDocumentUriUsingTree(tree, id), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null }
        // "primary:Download/Recipes" → "Recipes"; the phone's top folder ("primary:") reads as "Phone".
        return shown?.takeIf { it.isNotBlank() } ?: id?.substringAfter(':')?.substringAfterLast('/')?.ifEmpty { "Phone" } ?: "Folder"
    }
}
