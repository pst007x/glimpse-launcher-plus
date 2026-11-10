package com.glimpsemedia.glimpselauncher.model

/**
 * A file or folder found in a picked folder: its [name], its id in that folder's tree, and [path], the folders
 * above it ("Download/Recipes") for the line under a result. [root] is the id of the picked folder it was found in.
 */
data class Entry(
    val name: String,
    val isFolder: Boolean,
    val id: String,
    val path: String,
    val mime: String = "",
    val root: String = "",
)

/** A folder tree to walk: the entries directly in folder [id]. It may throw (no permission, folder gone). */
interface Tree {
    fun children(id: String): List<Entry>
}

/** What a walk found; [cut] when a limit stopped it early; [failedRoots] are picked folders that could not be read. */
data class WalkResult(val entries: List<Entry>, val cut: Boolean, val failedRoots: Set<String>)

/**
 * Every name in the picked folders [roots] (each listed itself too), level by level: hidden names (starting with ".")
 * left out, no folder read twice, at most [maxEntries] in all and [maxDepth] levels down. A picked folder that cannot
 * be read is noted and the others are still walked; a folder inside one that cannot be read is skipped. The walk
 * stops early, marked [WalkResult.cut], once [cancelled] says so.
 */
fun walk(
    roots: List<Entry>, tree: Tree, maxEntries: Int = 20_000, maxDepth: Int = 12, cancelled: () -> Boolean = { false },
    /** Ids already listed by an earlier walk (a picked folder inside another one): they are not listed again. */
    seen: MutableSet<String> = HashSet(),
): WalkResult {
    val out = ArrayList<Entry>()
    val failed = LinkedHashSet<String>()
    var cut = false
    var tooDeep = false
    for (root in roots) {
        if (cancelled()) { cut = true; break }
        if (!seen.add(root.id)) continue
        val first = try { tree.children(root.id) } catch (_: Exception) { failed += root.id; continue }
        if (out.size >= maxEntries) { cut = true; break }
        out += root.copy(root = root.id)
        // Each queued folder with the children already read (the root's) or to be read (null), and its depth.
        val queue = ArrayDeque<Triple<Entry, List<Entry>?, Int>>()
        queue += Triple(root, first, 0)
        while (queue.isNotEmpty() && !cut) {
            val (folder, known, depth) = queue.removeFirst()
            // A walk that is no longer wanted (the window closed) stops before reading another folder.
            if (known == null && cancelled()) { cut = true; break }
            val kids = known ?: try { tree.children(folder.id) } catch (_: Exception) { continue }
            for (kid in kids) {
                if (kid.name.startsWith(".") || !seen.add(kid.id)) continue
                if (out.size >= maxEntries) { cut = true; break }
                out += kid.copy(root = root.id)
                if (kid.isFolder) {
                    // Too deep: that folder is left out, but the walk goes on with the others.
                    if (depth + 1 < maxDepth) queue += Triple(kid, null, depth + 1) else tooDeep = true
                }
            }
        }
        if (cut) break
    }
    return WalkResult(out, cut || tooDeep, failed)
}
