package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Entry

/**
 * The whole-phone walk's names, packed down: each name once, with the number of its folder, and each folder's path
 * once. The full path and the file type are made again only for the entries asked for (the few results shown, or a
 * file opened), so 200,000 names take about a quarter of the memory full entries would.
 */
class PhoneNames private constructor(
    private val rootPath: String,
    private val lookup: (String) -> String?,
    private val names: Array<String>,
    private val folderOf: IntArray,
    private val isFolder: BooleanArray,
    private val folderPaths: Array<String>,
    /** The few entries that do not follow the usual pattern (the phone's storage itself), kept as they are. */
    private val asIs: Map<Int, Entry>,
) : java.util.AbstractList<Entry>(), java.util.RandomAccess {
    override val size get() = names.size

    override fun get(index: Int): Entry = asIs[index] ?: make(index)

    private fun make(i: Int): Entry {
        val path = folderPaths[folderOf[i]]
        val name = names[i]
        return Entry(name, isFolder[i], idOf(rootPath, path, name), path, if (isFolder[i]) "" else mimeOf(name, lookup), rootPath)
    }

    companion object {
        private fun idOf(root: String, path: String, name: String) = if (path.isEmpty()) "$root/$name" else "$root/$path/$name"

        /** [entries] from a walk of [rootPath], packed down; [lookup] gives a file's type from its extension again. */
        fun of(entries: List<Entry>, rootPath: String, lookup: (String) -> String?): PhoneNames {
            val n = entries.size
            val names = arrayOfNulls<String>(n)
            val folderOf = IntArray(n)
            val isFolder = BooleanArray(n)
            val paths = ArrayList<String>()
            val pathIndex = HashMap<String, Int>()
            val asIs = HashMap<Int, Entry>()
            for ((i, e) in entries.withIndex()) {
                names[i] = e.name
                isFolder[i] = e.isFolder
                folderOf[i] = pathIndex.getOrPut(e.path) { paths += e.path; paths.size - 1 }
                val usual = e.id == idOf(rootPath, e.path, e.name) && e.root == rootPath &&
                    e.mime == (if (e.isFolder) "" else mimeOf(e.name, lookup))
                if (!usual) asIs[i] = e
            }
            @Suppress("UNCHECKED_CAST")
            return PhoneNames(rootPath, lookup, names as Array<String>, folderOf, isFolder, paths.toTypedArray(), asIs)
        }
    }
}
