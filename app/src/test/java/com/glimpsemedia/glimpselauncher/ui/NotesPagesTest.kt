package com.glimpsemedia.glimpselauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesPagesTest {
    @Test fun pageMapping() {
        assertEquals(0, homePageOf(1))
        assertEquals(-1, homePageOf(NOTES_PAGE))
        assertEquals(1, pagerIndexOf(0))
    }
}
