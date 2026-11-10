package com.glimpsemedia.glimpselauncher.data

import androidx.activity.ComponentActivity
import com.glimpsemedia.glimpselauncher.model.Entry
import com.glimpsemedia.glimpselauncher.model.WalkResult

/** The Play edition has no whole-phone search: Search uses the picked folders only. */
fun allFilesFor(@Suppress("UNUSED_PARAMETER") activity: ComponentActivity): AllFiles = AllFilesNone

object AllFilesNone : AllFiles {
    override val available = false
    override val usesSettingsPage = false
    override val permissionVersion = 0
    override fun hasPermission() = false
    override fun request() {}
    override fun onResume() {}
    override fun last(): WalkResult? = null
    override suspend fun current() = WalkResult(emptyList(), false, emptySet())
    override fun opened() {}
    override fun closed() {}
    override fun owns(entry: Entry) = false
    override fun open(entry: Entry) = false
    override fun openFolder(entry: Entry) = false
    override suspend fun childrenOf(entry: Entry): List<Entry>? = null
}
