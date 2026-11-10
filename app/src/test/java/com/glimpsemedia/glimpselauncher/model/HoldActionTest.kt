package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldActionTest {
    @Test fun webAddresses() {
        assertEquals("https://example.com", webAddress("example.com"))
        assertEquals("https://example.com/a?b=1", webAddress("  example.com/a?b=1 "))
        assertEquals("http://x.org", webAddress("http://x.org"))
        assertEquals("https://Example.com", webAddress("HTTPS://Example.com"))
        assertNull(webAddress(""))
        assertNull(webAddress("   "))
        assertNull(webAddress("two words.com"))
        assertNull(webAddress("ftp://x.org"))
        assertNull(webAddress("javascript:alert(1)"))
        assertNull(webAddress("intent://x#Intent;end"))
        assertNull(webAddress("nodot"))
    }

    @Test fun numbers() {
        assertEquals("+441234567", dialable(" +44 (123) 45-67 "))
        assertEquals("999", dialable("999"))
        assertNull(dialable("12"))
        assertNull(dialable("abc"))
    }

    @Test fun keys() {
        assertEquals("a/M", Item.App("a/M").holdKey)
        assertEquals("f:7", Item.Folder("7", "F", listOf("a/M")).holdKey)
        assertEquals("all", Item.AllApps.holdKey)
        assertNull(Item.Emergency.holdKey)
        assertNull(Item.Widget(1, "p/W", 1, 1).holdKey)
        assertEquals("dock:a/M", dockHoldKey("a/M"))
        assertEquals("dock:all", dockHoldKey(null))
    }

    @Test fun descriptions() {
        val label = { k: String -> if (k == "maps/M") "Maps" else null }
        assertEquals("Open Maps", HoldAction.OpenApp("maps/M").describe(label))
        assertEquals("Open an app that is no longer on the phone", HoldAction.OpenApp("gone/M").describe(label))
        assertEquals("Open https://example.com", HoldAction.Web("https://example.com").describe(label))
        assertEquals("Call 999", HoldAction.Call("999").describe(label))
        assertEquals("Text 999", HoldAction.Text("999").describe(label))
        assertEquals("Torch (on/off)", HoldAction.Phone(PhoneAction.Torch).describe(label))
    }

    @Test fun actionGoesWithItsTargetApp() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M")))), listOf("b/M"))
            .withHoldAction("a/M", HoldAction.OpenApp("t/M"))
            .withHoldAction("dock:b/M", HoldAction.Phone(PhoneAction.Torch))
        val after = l.withoutMissing(setOf("a/M", "b/M"))
        assertEquals(mapOf("dock:b/M" to HoldAction.Phone(PhoneAction.Torch)), after.holdActions)
    }

    @Test fun actionGoesWithItsTile() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M")))), listOf("b/M"))
            .withHoldAction("a/M", HoldAction.Call("999"))
            .withHoldAction("dock:b/M", HoldAction.Call("999"))
        assertEquals(emptyMap<String, HoldAction>(), l.withoutMissing(emptySet()).withoutStrayHolds().holdActions)
    }

    @Test fun strayActionsGo() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M"), Item.AllApps))), emptyList())
            .withHoldAction("a/M", HoldAction.Call("999"))
            .withHoldAction("all", HoldAction.Phone(PhoneAction.Camera))
            .withHoldAction("dock:all", HoldAction.Phone(PhoneAction.Timer))
            .withHoldAction("f:gone", HoldAction.Phone(PhoneAction.Wifi))
        assertEquals(setOf("a/M", "all", "dock:all"), l.withoutStrayHolds().holdActions.keys)
        assertEquals(setOf("all", "dock:all"), l.removeFromPage(0, 0).withoutStrayHolds().holdActions.keys)
    }

    @Test fun gridSwitchKeepsActions() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M")))), emptyList()).withHoldAction("a/M", HoldAction.Web("https://x.org"))
        assertEquals(l.holdActions, l.withColumns(4).withColumns(2).holdActions)
    }

    @Test fun savedAndReadBack() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M"), Item.AllApps))), listOf("b/M"))
            .withHoldAction("a/M", HoldAction.OpenApp("t/M"))
            .withHoldAction("all", HoldAction.Web("https://x.org"))
            .withHoldAction("dock:b/M", HoldAction.Call("+44123"))
            .withHoldAction("dock:all", HoldAction.Text("999"))
            .withHoldAction("f:1", HoldAction.Phone(PhoneAction.DoNotDisturb))
        assertEquals(l.holdActions, LayoutJson.decode(LayoutJson.encode(l))!!.holdActions)
    }

    @Test fun oldSaveAndUnknownActionsRead() {
        assertEquals(emptyMap<String, HoldAction>(), LayoutJson.decode("""{"v":1,"pages":[[]],"dock":[]}""")!!.holdActions)
        val odd = """{"v":1,"pages":[[]],"dock":[],"hold":{"a/M":{"t":"rocket"},"b/M":{"t":"phone","p":"Laser"},"c/M":{"t":"web","u":"https://x.org"}}}"""
        assertEquals(mapOf("c/M" to HoldAction.Web("https://x.org")), LayoutJson.decode(odd)!!.holdActions)
    }

    @Test fun notesAfterRunning() {
        assertEquals("Opening Maps", HoldAction.OpenApp("maps/M").note("Maps", torchNowOn = false))
        assertEquals("Opening example.com", HoldAction.Web("https://example.com/a?b=1").note(null, torchNowOn = false))
        assertEquals("Calling 999", HoldAction.Call("999").note(null, torchNowOn = false))
        assertEquals("Text to 999", HoldAction.Text("999").note(null, torchNowOn = false))
        assertEquals("Torch on", HoldAction.Phone(PhoneAction.Torch).note(null, torchNowOn = true))
        assertEquals("Torch off", HoldAction.Phone(PhoneAction.Torch).note(null, torchNowOn = false))
        assertEquals("Wi-Fi settings", HoldAction.Phone(PhoneAction.Wifi).note(null, torchNowOn = false))
    }

    @Test fun capitalisedSchemesAreLowered() {
        assertEquals("https://bbc.co.uk/News", webAddress("Https://bbc.co.uk/News"))
        assertEquals("http://x.org", webAddress("HTTP://x.org"))
    }

    @Test fun numbersWithPausesOrExtensionsAreRefused() {
        assertNull(dialable("0123 456,789"))
        assertNull(dialable("0123456;789"))
        assertNull(dialable("0123456p789"))
        assertNull(dialable("0123456w789"))
    }

    @Test fun notesActionSavedAndDescribed() {
        val l = Layout(listOf(Page(listOf(Item.App("a/M")))), emptyList()).withHoldAction("a/M", HoldAction.Notes)
        assertEquals(l.holdActions, LayoutJson.decode(LayoutJson.encode(l))!!.holdActions)
        assertEquals("Open the notes screen", HoldAction.Notes.describe { null })
        assertEquals("Notes", HoldAction.Notes.note(null, false))
    }

    @Test fun holdSwitchPausesButKeepsAction() {
        val web = HoldAction.Web("https://example.com")
        assertEquals(DockAllHold.OpenSearch, dockAllAppsHold(holdToSearch = true, stored = web))
        assertEquals(DockAllHold.Action(web), dockAllAppsHold(holdToSearch = false, stored = web))
    }

    @Test fun holdSwitchOffNoAction() {
        assertEquals(null, dockAllAppsHold(holdToSearch = false, stored = null))
        assertEquals(DockAllHold.OpenSearch, dockAllAppsHold(holdToSearch = true, stored = null))
    }
}
