package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IconStoreKeyTest {
    @Test fun cacheKeyCarriesVersion() {
        val a = iconCacheName("website.leifs.delta", 120, "com.whatsapp/com.whatsapp.Main", null)
        assertEquals("website.leifs.delta/120/com.whatsapp_com.whatsapp.Main.png", a)
        assertNotEquals(a, iconCacheName("website.leifs.delta", 121, "com.whatsapp/com.whatsapp.Main", null))
    }

    @Test fun cacheKeyCarriesDayForCalendars() =
        assertEquals("p/1/com.cal_X-11.png", iconCacheName("p", 1, "com.cal/X", 11))

    @Test fun iconPackStartsAsNone() = assertEquals("", Settings().iconPack)
}
