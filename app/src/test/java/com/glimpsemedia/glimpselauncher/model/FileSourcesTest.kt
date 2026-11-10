package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FileSourcesTest {
    @Test fun allFilesPermittedSearchesThePhoneOnly() = assertEquals(FileSources(phone = true, folders = false), fileSources(true, true, true))
    @Test fun revokedPermissionDropsPhone() = assertEquals(FileSources(phone = false, folders = true), fileSources(true, false, true))
    @Test fun offUsesPickedFolders() = assertEquals(FileSources(phone = false, folders = true), fileSources(false, true, true))
    @Test fun nothingOnSearchesNoFiles() = assertFalse(fileSources(false, false, false).any)
    @Test fun allFilesSearchStartsOff() = assertFalse(Settings().allFilesSearch)
}
