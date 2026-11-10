package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.Tree
import java.io.File
import java.nio.file.Files

/** How many names, and how many levels down, the whole-phone search reads at most. */
const val PHONE_MAX_ENTRIES = 200_000
const val PHONE_MAX_DEPTH = 20

/** The folders Android keeps for each app, which no app may read: never walked. */
private val SKIPPED = setOf("Android/data", "Android/obb")

/**
 * The phone's own storage as a [Tree]: ids are absolute paths, [Entry.path] the folders between [root] and the entry
 * ("Download/Recipes"). Hidden names, symbolic links and Android/data and Android/obb are left out; a folder that
 * cannot be listed reads as empty.
 */
open class FileTree(private val root: File, private val mimeLookup: (String) -> String? = { null }) : Tree {
    private val rootPath = root.absolutePath

    /** The folder's contents, or null when it cannot be listed (made a separate step so tests can refuse one). */
    protected open fun list(dir: File): Array<File>? = dir.listFiles()

    /** [f]'s path below the root ("" for the root itself). */
    fun relative(f: File): String = f.absolutePath.removePrefix(rootPath).trimStart('/')

    override fun children(id: String): List<Entry> {
        val dir = File(id)
        val here = relative(dir)
        val out = ArrayList<Entry>()
        for (f in list(dir) ?: return emptyList()) {
            if (f.name.startsWith(".")) continue
            if (Files.isSymbolicLink(f.toPath())) continue
            if (relative(f) in SKIPPED) continue
            val isDir = f.isDirectory
            out += Entry(f.name, isDir, f.absolutePath, here, if (isDir) "" else mimeOf(f.name, mimeLookup), rootPath)
        }
        return out
    }
}

/** The phone's storage itself, as the one place the whole-phone walk starts from. */
fun phoneRoot(root: File): Entry = Entry("Phone", true, root.absolutePath, "", "", root.absolutePath)

/** A file's type from its extension (any case); unknown or none is any type. */
fun mimeOf(name: String, lookup: (String) -> String?): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    if (ext.isEmpty()) return "*/*"
    return lookup(ext) ?: "*/*"
}

/** The phone storage's document id for a path below it, as the Files app knows it; the name is kept as it is. */
fun externalDocId(relativePath: String): String = "primary:$relativePath"
