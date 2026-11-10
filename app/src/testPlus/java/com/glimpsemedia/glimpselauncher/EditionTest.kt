package com.glimpsemedia.glimpselauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditionTest {
    @Test fun plusIsThePlusEdition() {
        assertTrue(BuildConfig.PLUS)
        assertEquals("com.glimpsemedia.glimpselauncherplus", BuildConfig.APPLICATION_ID)
    }
}
