package com.glimpsemedia.glimpselauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EditionTest {
    @Test fun playIsThePlayEdition() {
        assertFalse(BuildConfig.PLUS)
        assertEquals("com.glimpsemedia.glimpselauncher", BuildConfig.APPLICATION_ID)
    }
}
