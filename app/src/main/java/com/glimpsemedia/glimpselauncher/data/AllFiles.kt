package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.WalkResult

/**
 * Search of every file and folder name on the phone: only Glimpse Launcher Plus has it ([available]); the Play
 * edition's stand-in does nothing. Made with the activity, before it starts ([allFilesFor], one per edition).
 */
interface AllFiles {
    val available: Boolean
    /** Android 11 and later: the permission is switched on in Android's own settings page, not asked for. */
    val usesSettingsPage: Boolean
    /** Goes up when Android's answer may have changed (back from its settings page, or the permission question). */
    val permissionVersion: Int
    fun hasPermission(): Boolean
    /** Android 11 and later: opens Android's All files access page for this app; 8 to 10: asks for file access. */
    fun request()
    fun onResume()
    /** The last whole-phone result at once, or null before the first walk. */
    fun last(): WalkResult?
    /** The names on the phone: the kept result when under a minute old, else a new walk (stopped with its caller). */
    suspend fun current(): WalkResult
    /** The search window opened or closed: some time after it closes the names are dropped to free memory. */
    fun opened()
    fun closed()
    /** Whether [entry] was found by the whole-phone search (not in a picked folder). */
    fun owns(entry: Entry): Boolean
    fun open(entry: Entry): Boolean
    fun openFolder(entry: Entry): Boolean
    suspend fun childrenOf(entry: Entry): List<Entry>?
}
