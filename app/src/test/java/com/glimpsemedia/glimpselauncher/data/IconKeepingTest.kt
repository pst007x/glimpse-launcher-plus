package com.glimpsemedia.glimpselauncher.data

import com.glimpsemedia.glimpselauncher.model.IconChoice
import com.glimpsemedia.glimpselauncher.model.acceptApply
import com.glimpsemedia.glimpselauncher.model.prunedChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconKeepingTest {
    @Test fun applyRequestRefusesNonPack() {
        val packs = setOf("website.leifs.delta")
        val a = "org.adw.launcher.SET_THEME"; val x = "org.adw.launcher.theme.NAME"
        assertEquals("website.leifs.delta", acceptApply(a, mapOf(x to "website.leifs.delta")) { it in packs })
        assertNull(acceptApply(a, mapOf(x to "com.evil.app")) { it in packs })
        assertNull(acceptApply(a, emptyMap()) { it in packs })
        assertNull(acceptApply("android.intent.action.VIEW", mapOf(x to "website.leifs.delta")) { it in packs })
    }

    @Test fun backupCarriesPickedNotPack() {
        assertTrue(Backup.isAllowedName("icons/picked/pic-com.a_.A-1.png"))
        assertFalse(Backup.isAllowedName("icons/pack/com.a_.A.png"))
        assertFalse(Backup.isAllowedName("icons/picked/../x.png"))
    }

    @Test fun uninstalledAppLosesChoice() {
        val c = mapOf("com.a/.A" to IconChoice.Pack("p", "a"), "com.gone/.G" to IconChoice.Picture("g.png"), "glimpse:allapps" to IconChoice.Picture("x.png"))
        assertEquals(setOf("com.a/.A", "glimpse:allapps"), prunedChoices(c, setOf("com.a/.A")).keys)
    }
}
