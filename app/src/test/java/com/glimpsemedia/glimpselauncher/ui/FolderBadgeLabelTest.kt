package com.glimpsemedia.glimpselauncher.ui

import com.glimpsemedia.glimpselauncher.model.TextIcon
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderBadgeLabelTest {
    @Test fun aBadgedFolderSaysItsBadge() {
        assertEquals("Games, folder, Games", tileContentDescription("Games", 0, false, folderBadgeSpoken(TextIcon.Games)))
        assertEquals("Holiday, 2 new, folder, Travel, has a hold action", tileContentDescription("Holiday", 2, true, folderBadgeSpoken(TextIcon.Travel)))
        assertEquals("Phone", tileContentDescription("Phone", 0, false, null))
    }
}
