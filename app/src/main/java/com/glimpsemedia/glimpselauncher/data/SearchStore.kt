package com.glimpsemedia.glimpselauncher.data

import android.content.Context
import com.glimpsemedia.glimpselauncher.model.FolderLocation
import com.glimpsemedia.glimpselauncher.model.SearchEngine
import com.glimpsemedia.glimpselauncher.model.SearchListsJson

/** Search's two lists, kept beside the settings: the user's search engines (backed up) and folders (not). */
class SearchStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun text(key: String): String? = runCatching { prefs.getString(key, null) }.getOrNull()

    fun engines(): List<SearchEngine> = SearchListsJson.engines(text("searchEngineList"))
    fun saveEngines(l: List<SearchEngine>) = prefs.edit().putString("searchEngineList", SearchListsJson.encodeEngines(l)).apply()
    fun folders(): List<FolderLocation> = SearchListsJson.folders(text("searchFolderList"))
    fun saveFolders(l: List<FolderLocation>) = prefs.edit().putString("searchFolderList", SearchListsJson.encodeFolders(l)).apply()
}
