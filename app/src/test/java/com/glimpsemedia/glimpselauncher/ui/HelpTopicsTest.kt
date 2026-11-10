package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class HelpTopicsTest {
    @Test fun helpHasSearchTopic() {
        val search = helpTopicList.firstOrNull { it.first == "Search" }
        assertTrue(search != null && "Add folder location(s)" in search.second && "Add search engine(s)" in search.second)
    }

    @Test fun plusHelpMentionsAllFiles() {
        assertTrue("All files and folders" in searchHelpText(true))
        assertFalse("All files and folders" in searchHelpText(false))
        assertTrue(searchHelpText(true).startsWith(searchHelpText(false)))
    }

    @Test fun names() {
        assertEquals("Glimpse Launcher Plus", com.glimpsemedia.glimpselauncher.model.appName(true))
        assertEquals("Glimpse Launcher", com.glimpsemedia.glimpselauncher.model.appName(false))
        assertEquals("https://glimpse-media.com/glimpse/plus", com.glimpsemedia.glimpselauncher.model.PLUS_PAGE)
    }

    @Test fun permissionWordingFitsTheAndroidVersion() {
        assertEquals("Glimpse Launcher Plus needs All files access to see every file and folder name. Android will open its page for this app: switch on Allow access to manage all files, then come back.", allFilesAskText(settingsPage = true))
        assertEquals("Glimpse Launcher Plus needs access to your files to see every file and folder name. Android will ask: choose Allow.", allFilesAskText(settingsPage = false))
        assertEquals("Open Permissions › Files and media › Allow", ALL_FILES_APP_INFO_NOTE)
    }
}
