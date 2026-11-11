package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackRulesTest {
    private fun tag(name: String, vararg a: Pair<String, String>) = Tag(name, a.toMap())

    private val filterTags = sequenceOf(
        tag("resources"),
        tag("iconback", "img1" to "back_a", "img2" to "back_b"),
        tag("iconmask", "img1" to "mask"),
        tag("iconupon", "img1" to "front"),
        tag("scale", "factor" to "0.75"),
        tag("item", "component" to "ComponentInfo{com.whatsapp/com.whatsapp.Main}", "drawable" to "whatsapp"),
        tag("item", "component" to "ComponentInfo{com.spotify.music/.MainActivity}", "drawable" to "spotify"),
        tag("item", "component" to "ComponentInfo{com.whatsapp/com.whatsapp.Other}", "drawable" to "whatsapp_alt"),
        tag("item", "component" to "broken", "drawable" to "x"),
        tag("item", "component" to "ComponentInfo{com.nodrawable/.A}", "drawable" to ""),
        tag("calendar", "component" to "ComponentInfo{com.android.calendar/com.android.calendar.AllInOneActivity}", "prefix" to "calendar_"),
    )
    private val filter = appFilterOf(filterTags)

    @Test fun appFilterReadsItemsAndRules() {
        assertEquals("whatsapp", filter.items["com.whatsapp/com.whatsapp.Main"])
        assertEquals(listOf("back_a", "back_b"), filter.backs)
        assertEquals(listOf("mask"), filter.masks)
        assertEquals(listOf("front"), filter.uppers)
        assertEquals(0.75f, filter.scale, 0.0001f)
        assertEquals("whatsapp", filter.byPackage["com.whatsapp"])   // the first one for a package
    }

    @Test fun leadingDotClassIsExpanded() = assertEquals("spotify", filter.items["com.spotify.music/com.spotify.music.MainActivity"])

    @Test fun malformedLinesSkipped() {
        assertEquals(3, filter.items.size)
        assertFalse(filter.items.keys.any { it.startsWith("com.nodrawable") })
    }

    @Test fun imagesNumberedFromZeroOrOne() {
        val f = appFilterOf(sequenceOf(tag("iconback", "img3" to "d", "img0" to "a", "img1" to "b", "img2" to "c")))
        assertEquals(listOf("a", "b", "c", "d"), f.backs)
    }

    @Test fun scaleMissingOrBadIsOne() {
        assertEquals(1f, appFilterOf(emptySequence()).scale, 0f)
        assertEquals(1f, appFilterOf(sequenceOf(tag("scale", "factor" to "big"))).scale, 0f)
        assertEquals(1f, appFilterOf(sequenceOf(tag("scale", "factor" to "-2"))).scale, 0f)
    }

    @Test fun calendarsRead() = assertEquals("calendar_", filter.calendars["com.android.calendar/com.android.calendar.AllInOneActivity"])

    @Test fun drawableListWithCategories() {
        val list = drawableListOf(sequenceOf(tag("category", "title" to "Social"), tag("item", "drawable" to "whatsapp"),
            tag("category", "title" to "Music"), tag("item", "drawable" to "spotify"), tag("item", "drawable" to "spotify")))
        assertEquals(listOf(DrawableEntry("whatsapp", "Social"), DrawableEntry("spotify", "Music")), list)
    }

    @Test fun drawableListFallsBackToAppfilter() =
        assertEquals(listOf("spotify", "whatsapp", "whatsapp_alt"), drawableListFrom(filter).map { it.name })

    @Test fun calendarNameForDay() = assertEquals("calendar_11", calendarName("calendar_", 11))

    private fun choose(
        choice: IconChoice? = null, pack: String? = "p", installed: Boolean = true, has: Set<String> = emptySet(),
        perAppCopy: String? = null, packCopy: String? = null, component: String = "com.whatsapp/com.whatsapp.Main", f: AppFilter = filter,
    ) = chooseIcon(
        key = "k", component = component, choice = choice, wholePack = pack?.let { WholePack(it, f) },
        resolves = { _, d -> d in has }, perAppCopy = { perAppCopy }, packCopy = { packCopy },
        packInstalled = { installed }, day = 11,
    )

    @Test fun choiceOrder() {
        assertEquals(IconSource.Picture("x.png"), choose(choice = IconChoice.Picture("x.png")))
        assertEquals(IconSource.PackDrawable("q", "custom"), choose(choice = IconChoice.Pack("q", "custom"), has = setOf("custom", "whatsapp")))
        val cal = "com.android.calendar/com.android.calendar.AllInOneActivity"
        assertEquals(IconSource.PackDrawable("p", "calendar_11"), choose(component = cal, has = setOf("calendar_11", "calendar_")))
        assertEquals(IconSource.PackDrawable("p", "calendar_"), choose(component = cal, has = setOf("calendar_")))
        assertEquals(IconSource.PackDrawable("p", "whatsapp"), choose(has = setOf("whatsapp")))
        assertEquals(IconSource.PackDrawable("p", "whatsapp"), choose(component = "com.whatsapp/com.whatsapp.New", has = setOf("whatsapp")))
        assertEquals(IconSource.Fitted("p"), choose(component = "com.other/.X"))
        assertEquals(IconSource.Own, choose(component = "com.other/.X", f = appFilterOf(emptySequence())))
        assertEquals(IconSource.Own, choose(pack = null))
    }

    @Test fun packGoneUsesSavedCopy() {
        assertEquals(IconSource.SavedCopy("pack/k.png"), choose(installed = false, packCopy = "pack/k.png"))
        assertEquals(IconSource.Own, choose(installed = false))
        assertEquals(IconSource.SavedCopy("picked/k.png"), choose(choice = IconChoice.Pack("q", "custom"), installed = false, perAppCopy = "picked/k.png"))
    }

    @Test fun missingDrawableFallsThrough() {
        assertEquals(IconSource.PackDrawable("p", "whatsapp"), choose(choice = IconChoice.Pack("p", "gone"), has = setOf("whatsapp")))
        assertEquals(IconSource.Fitted("p"), choose(has = emptySet()))
    }

    @Test fun backIsStableForAKey() {
        val backs = listOf("a", "b", "c")
        assertEquals(backFor("com.x/.Y", backs), backFor("com.x/.Y", backs))
        assertTrue(backFor("com.x/.Y", backs) in backs)
        assertNull(backFor("k", emptyList()))
    }

    @Test fun applyPackageFromEachExtra() {
        assertEquals("website.leifs.delta", applyPackage("org.adw.launcher.SET_THEME", mapOf("org.adw.launcher.theme.NAME" to "website.leifs.delta")))
        assertEquals("a.b", applyPackage("com.anddoes.launcher.SET_THEME", mapOf("com.anddoes.launcher.THEME_PACKAGE_NAME" to " a.b ")))
        assertEquals("c.d", applyPackage("com.teslacoilsw.launcher.APPLY_ICON_THEME", mapOf("com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE" to "c.d")))
        assertNull(applyPackage("org.adw.launcher.SET_THEME", emptyMap()))
        assertNull(applyPackage("something.else", mapOf("org.adw.launcher.theme.NAME" to "a.b")))
    }

    @Test fun searchIgnoresUnderscoresAndCase() {
        assertTrue(matchesSearch("google_play_store", "Play Store"))
        assertTrue(matchesSearch("whatsapp", "  "))
        assertFalse(matchesSearch("whatsapp", "spot"))
    }

    @Test fun squareCutWideTallSquare() {
        assertArrayEquals(intArrayOf(50, 0, 100), squareCut(200, 100))
        assertArrayEquals(intArrayOf(0, 25, 100), squareCut(100, 150))
        assertArrayEquals(intArrayOf(0, 0, 64), squareCut(64, 64))
    }

    @Test fun iconChoicesRoundTripAndBadJson() {
        val m = mapOf("com.a/.A" to IconChoice.Pack("p", "d"), "glimpse:allapps" to IconChoice.Picture("x.png"))
        assertEquals(m, parseIconChoices(encodeIconChoices(m)))
        assertEquals(emptyMap<String, IconChoice>(), parseIconChoices("not json"))
        assertEquals(emptyMap<String, IconChoice>(), parseIconChoices(null))
        assertEquals(mapOf("ok" to IconChoice.Picture("y.png")), parseIconChoices("""{"ok":{"picture":"y.png"},"bad":{"pack":"p"}}"""))
    }
}
