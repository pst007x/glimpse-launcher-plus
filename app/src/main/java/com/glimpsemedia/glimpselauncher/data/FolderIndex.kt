package com.glimpsemedia.glimpselauncher.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.FolderLocation
import com.glimpsemedia.glimpselauncher.model.Tree
import com.glimpsemedia.glimpselauncher.model.WalkResult
import com.glimpsemedia.glimpselauncher.model.walk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

/**
 * Used from the main thread only (its cache is not locked); the walk itself runs on the IO threads.
 * The names in the folders picked for Search, read through Android's folder grants (no storage permission).
 * Only names are read, never what is inside the files. The last result is kept for a minute.
 */
class FolderIndex(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private var cached: WalkResult? = null
    private var cachedFor: List<FolderLocation> = emptyList()
    private var cachedAt = 0L

    /** The last result, at once, or null before the first walk (shown while a new one is made). */
    fun last(locations: List<FolderLocation>): WalkResult? = cached?.takeIf { cachedFor == locations }

    /** The names in [locations]: the kept result when it is under a minute old and for the same folders, else a new walk. */
    suspend fun current(locations: List<FolderLocation>, now: Long = System.currentTimeMillis()): WalkResult {
        cached?.let { if (cachedFor == locations && now - cachedAt < MAX_AGE_MS) return it }
        val result = withContext(Dispatchers.IO) { val job = coroutineContext; walkAll(locations) { !job.isActive } }
        // A walk stopped because nobody wants it any more is not kept.
        if (!coroutineContext.isActive) return result
        cached = result; cachedFor = locations; cachedAt = now
        return result
    }

    private fun walkAll(locations: List<FolderLocation>, cancelled: () -> Boolean): WalkResult {
        val entries = ArrayList<Entry>()
        val failed = LinkedHashSet<String>()
        var cut = false
        // Shared by every picked folder, so one inside another is not listed twice.
        val seen = HashSet<String>()
        for (loc in locations) {
            val left = MAX_ENTRIES - entries.size
            if (left <= 0) { cut = true; break }
            val root = rootOf(loc) ?: run { failed += loc.uri; null } ?: continue
            val r = walk(listOf(root), treeOf(loc), maxEntries = left, cancelled = cancelled, seen = seen)
            entries += r.entries
            if (r.failedRoots.isNotEmpty()) failed += loc.uri
            if (r.cut) cut = true
        }
        return WalkResult(entries, cut, failed)
    }

    /** The picked folder itself as an entry; its [Entry.root] and id are the tree's own document id. */
    private fun rootOf(loc: FolderLocation): Entry? = try {
        val id = DocumentsContract.getTreeDocumentId(Uri.parse(loc.uri))
        Entry(loc.name, true, id, "", DocumentsContract.Document.MIME_TYPE_DIR, root = id)
    } catch (_: Exception) { null }

    private fun treeOf(loc: FolderLocation): Tree {
        val tree = Uri.parse(loc.uri)
        // Where each folder sits, for the line under its children ("Download/Recipes").
        val paths = HashMap<String, String>()
        return object : Tree {
            override fun children(id: String): List<Entry> {
                val here = paths[id] ?: loc.name
                val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
                val cols = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                )
                val out = ArrayList<Entry>()
                resolver.query(uri, cols, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val kidId = c.getString(0) ?: continue
                        val name = c.getString(1) ?: continue
                        val mime = c.getString(2) ?: ""
                        val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                        if (isDir) paths[kidId] = "$here/$name"
                        out += Entry(name, isDir, kidId, here, mime)
                    }
                } ?: throw IllegalStateException("Folder not readable")
                return out
            }
        }
    }

    /** The address to open [entry] found in [loc] with. */
    fun uriOf(loc: FolderLocation, entry: Entry): Uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(loc.uri), entry.id)

    /** The entries directly in folder [entry] of [loc] (for showing a folder no app opens), or null when unreadable. */
    suspend fun childrenOf(loc: FolderLocation, entry: Entry): List<Entry>? = withContext(Dispatchers.IO) {
        try { treeOf(loc).children(entry.id).filter { !it.name.startsWith(".") }.map { it.copy(path = if (entry.path.isEmpty()) entry.name else entry.path + "/" + entry.name, root = entry.root) } } catch (_: Exception) { null }
    }

    companion object {
        const val MAX_ENTRIES = 20_000
        const val MAX_AGE_MS = 60_000L
    }
}
