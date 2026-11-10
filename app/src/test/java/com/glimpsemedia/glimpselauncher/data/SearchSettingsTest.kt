package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchSettingsTest {
    @Test fun defaultsWhenKeysMissing() {
        val s = Settings()
        assertFalse(s.holdAllAppsToSearch); assertFalse(s.searchFolders); assertFalse(s.searchEngines)
    }

    @Test fun backupLeavesOutFolders() {
        val all = mapOf(
            "holdAllAppsToSearch" to PrefValue.B(true), "searchFolders" to PrefValue.B(true), "searchEngines" to PrefValue.B(true),
            "searchEngineList" to PrefValue.S("[]"), "searchFolderList" to PrefValue.S("[{\"uri\":\"x\",\"name\":\"y\"}]"),
        )
        assertEquals(all - "searchFolderList", Backup.settingsForBackup(all))
    }

    @Test fun backupKeepsAllFilesSearch() {
        val all = mapOf("allFilesSearch" to PrefValue.B(true))
        assertEquals(all, Backup.settingsForBackup(all))
    }
}

class RestoreKeepsTest {
    @Test fun restoreKeepsFolderListAndLastBackup() {
        val current = mapOf("searchFolderList" to PrefValue.S("[x]"), "lastBackupMillis" to PrefValue.L(5), "darkTheme" to PrefValue.B(false))
        val backup = mapOf("darkTheme" to PrefValue.B(true))
        assertEquals(
            mapOf("darkTheme" to PrefValue.B(true), "searchFolderList" to PrefValue.S("[x]"), "lastBackupMillis" to PrefValue.L(5)),
            Backup.restoredPrefs(backup, current),
        )
    }
}
