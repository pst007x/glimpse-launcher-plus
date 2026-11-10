package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TileShadeTest {
    @Test fun savedAndReadBack() {
        val a = Item.App("a/M")
        val l = Layout(listOf(Page(listOf(a))), emptyList()).withTileColor(a, 0xFF336699.toInt()).withTileShade(a, 20)
            .withIconColor(a, 0xFF112233.toInt()).withIconShade(a, -15)
        val back = LayoutJson.decode(LayoutJson.encode(l))!!
        assertEquals(mapOf("a/M" to 20), back.tileShades)
        assertEquals(mapOf("a/M" to -15), back.iconShades)
    }

    @Test fun tileShadeGoesWithItsColour() {
        val a = Item.App("a/M")
        val l = Layout(listOf(Page(listOf(a))), emptyList()).withTileColor(a, 0xFF336699.toInt()).withTileShade(a, 20)
        assertEquals(emptyMap<String, Int>(), l.withTileColor(a, 0).tileShades)
        assertEquals(emptyMap<String, Int>(), l.removeFromPage(0, 0).withoutStrayColors().tileShades)
        assertEquals(emptyMap<String, Int>(), l.withTileShade(a, 0).tileShades)
    }

    @Test fun oldLayoutHasNoShades() {
        val l = LayoutJson.decode("""{"v":1,"pages":[[{"t":"app","k":"a/M"}]],"dock":[]}""")!!
        assertEquals(emptyMap<String, Int>(), l.tileShades)
        assertEquals(emptyMap<String, Int>(), l.iconShades)
    }

    @Test fun readShadesAreClamped() {
        val l = LayoutJson.decode("""{"v":1,"pages":[[{"t":"app","k":"a/M"}]],"dock":[],"tileShade":{"a/M":90}}""")!!
        assertEquals(mapOf("a/M" to 50), l.tileShades)
    }
}
