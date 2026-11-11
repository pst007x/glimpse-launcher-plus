package com.glimpsemedia.glimpselauncher.model

import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.TextModeStore
import com.glimpsemedia.glimpselauncher.data.withTextModeToggled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TextModeTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun startingLayout() {
        val t = startingTextLayout("dial/D", "sms/S", "gal/G", "web/W")
        assertEquals(listOf("dial/D", "sms/S", "gal/G", "web/W"), t.apps)
        assertEquals(1, t.folders.size)
        assertEquals("Settings", t.folders[0].name); assertEquals(TextIcon.Folder, t.folders[0].icon)
        assertEquals(listOf(SYSTEM_SETTINGS, GLIMPSE_SETTINGS), t.folders[0].entries)
    }

    @Test fun startingLayoutSkipsMissingApps() {
        assertEquals(listOf("dial/D", "web/W"), startingTextLayout("dial/D", null, null, "web/W").apps)
        val none = startingTextLayout(null, null, null, null)
        assertEquals(emptyList<String>(), none.apps); assertEquals(1, none.folders.size)
    }

    @Test fun listEditing() {
        val t = TextLayout().withApp("a/A").withApp("b/B").withApp("a/A").withApp("c/C")
        assertEquals(listOf("a/A", "b/B", "c/C"), t.apps)
        assertEquals(listOf("b/B", "a/A", "c/C"), t.appMoved("a/A", 1).apps)
        assertEquals(t, t.appMoved("a/A", -1))
        assertEquals(listOf("a/A", "c/C"), t.withoutApp("b/B").apps)
    }

    @Test fun folderEditing() {
        val f = TextFolder("f1", "Music", TextIcon.Music, emptyList()).withEntry("m/M").withEntry("p/P").withEntry("m/M")
        assertEquals(listOf("m/M", "p/P"), f.entries)
        assertEquals(listOf("p/P", "m/M"), f.entryMoved("p/P", -1).entries)
        assertEquals(listOf("p/P"), f.withoutEntry("m/M").entries)
        val t = TextLayout().withFolder(f).withFolder(TextFolder("f2", "Work", TextIcon.Work, emptyList()))
        assertEquals(listOf("f2", "f1"), t.folderMoved("f2", -1).folders.map { it.id })
        assertEquals("Tunes", t.withFolder(f.copy(name = "Tunes")).folders.first { it.id == "f1" }.name)
        assertEquals(listOf("f2"), t.withoutFolder("f1").folders.map { it.id })
    }

    @Test fun uninstalledAppsGoEverywhere() {
        val t = TextLayout(listOf("a/A", "gone/G"), listOf(TextFolder("f", "F", TextIcon.Star, listOf("gone/G", GLIMPSE_SETTINGS, SYSTEM_SETTINGS, "a/A"))))
        val after = t.withoutMissing(setOf("a/A"))
        assertEquals(listOf("a/A"), after.apps)
        assertEquals(listOf(GLIMPSE_SETTINGS, SYSTEM_SETTINGS, "a/A"), after.folders[0].entries)
    }

    @Test fun firstItemIsDrawnLowest() { assertEquals(listOf("c", "b", "a"), drawnBottomUp(listOf("a", "b", "c"))) }

    @Test fun glimpseSettingsWhileLocked() {
        assertEquals(SettingsTap.Open, glimpseSettingsTap(unlocked = true, hasPin = true))
        assertEquals(SettingsTap.Open, glimpseSettingsTap(unlocked = true, hasPin = false))
        assertEquals(SettingsTap.AskPin, glimpseSettingsTap(unlocked = false, hasPin = true))
        assertEquals(SettingsTap.HoldPadlock, glimpseSettingsTap(unlocked = false, hasPin = false))
    }

    @Test fun iconsReadByName() {
        assertEquals(TextIcon.Music, textIconOf("Music")); assertEquals(TextIcon.Folder, textIconOf("Rocket")); assertEquals(TextIcon.Folder, textIconOf(null))
        assertEquals(41, TextIcon.entries.size)
    }

    @Test fun theNewIconsAreReadByName() {
        for (n in listOf("Phone", "Email", "Camera", "Calendar", "Maps", "Weather", "Food", "Fitness", "Sport", "Kids", "School", "Books", "Podcasts", "TV", "Notes", "Home", "Car", "Cloud", "Documents", "Pets"))
            assertEquals(n, textIconOf(n).name)
    }

    @Test fun theFirstIconsKeepTheirPlaces() {
        // Saved by name, but the pickers list them in this order: the new ones come after the first 21.
        assertEquals(TextIcon.Star, TextIcon.entries[20])
    }

    @Test fun savedAndReadBack() {
        val s = TextModeStore(tmp.root)
        assertNull(s.load())
        val t = startingTextLayout("dial/D", null, "gal/G", null).withFolder(TextFolder("f9", "Odd \"name\" ♪", TextIcon.AI, listOf("x/X")))
        s.save(t); assertEquals(t, TextModeStore(tmp.root).load())
    }

    @Test fun unreadableFileIsKeptAside() {
        File(tmp.root, "textmode.json").writeText("{ nope")
        assertEquals(TextLayout(), TextModeStore(tmp.root).load())
        assertTrue(File(tmp.root, "textmode.json.bad").exists())
    }

    @Test fun badEntriesAndDuplicateFolderIdsAreMended() {
        val text = """{"v":1,"apps":["a/A","junk","glimpse:settings"],"folders":[{"id":"x","n":"One","i":"Rocket","e":["b/B","??"]},{"id":"x","n":"Two","i":"Star","e":[]}]}"""
        val t = TextModeJson.decode(text)!!
        assertEquals(listOf("a/A", GLIMPSE_SETTINGS), t.apps)
        assertEquals(listOf("b/B"), t.folders[0].entries)
        assertEquals(TextIcon.Folder, t.folders[0].icon)
        assertEquals(2, t.folders.map { it.id }.toSet().size)
    }

    @Test fun oldSettingsReadTheDefaults() {
        val s = Settings()
        assertEquals(listOf(false, true, false, 0, true), listOf(s.textMode, s.textClock, s.textListRight, s.textStep, s.showBar))
    }

    @Test fun widgetEditing() {
        val t = TextLayout().withWidget(TextWidget(5, "a/A", 1)).withWidget(TextWidget(7, "b/B", 0))
        assertEquals(listOf(7, 5), t.widgetMoved(7, -1).widgets.map { it.id })
        assertEquals(3, t.widgetResized(5, 9).widgets.first { it.id == 5 }.height)
        assertEquals(listOf(7), t.withoutWidget(5).widgets.map { it.id })
        assertEquals(2, t.withWidget(TextWidget(5, "a/A", 2)).widgets.size)
    }

    @Test fun startingHeightIsTheNearest() {
        assertEquals(0, textWidgetHeightFor(40)); assertEquals(1, textWidgetHeightFor(180))
        assertEquals(2, textWidgetHeightFor(260)); assertEquals(3, textWidgetHeightFor(900))
    }

    @Test fun widgetsSavedAndOldFilesReadNone() {
        val t = TextLayout(listOf("a/A")).withWidget(TextWidget(12, "p.k/p.k.W", 3))
        assertEquals(t, TextModeJson.decode(TextModeJson.encode(t)))
        assertEquals(emptyList<TextWidget>(), TextModeJson.decode("""{"v":1,"apps":[],"folders":[]}""")!!.widgets)
        assertEquals(1, TextModeJson.decode("""{"v":1,"apps":[],"folders":[],"w":[{"id":3,"p":"x/Y","h":8}]}""")!!.widgets[0].height)
    }

    @Test fun goneWidgetsAreDropped() {
        val t = TextLayout().withWidget(TextWidget(1, "a/A", 1)).withWidget(TextWidget(2, "b/B", 1))
        assertEquals(listOf(2), t.widgetsGone { it == 1 })
    }

    @Test fun settingsListMatchesTheScreen() {
        assertEquals(listOf("c", "b", "a"), shownInScreenOrder(listOf("a", "b", "c")))
        val t = TextLayout(listOf("a/A", "b/B", "c/C"))
        assertEquals(listOf("b/B", "a/A", "c/C"), t.appMoved("a/A", onScreenMove(-1)).apps)
    }

    @Test fun theSwitchFlipsOnlyTextMode() {
        val s = Settings(textClock = false, textStep = 0, showBar = false)
        assertEquals(s.copy(textMode = true), s.withTextModeToggled())
        assertEquals(s, s.withTextModeToggled().withTextModeToggled())
    }

    @Test fun aFolderKeepsItsOwnColour() {
        val t = TextLayout(folders = listOf(TextFolder("f", "F", TextIcon.Star, emptyList(), color = 0xFFFF40A0.toInt())))
        assertEquals(t, TextModeJson.decode(TextModeJson.encode(t)))
        assertEquals(0, TextModeJson.decode("""{"v":1,"apps":[],"folders":[{"id":"f","n":"F","i":"Star","e":[]}]}""")!!.folders[0].color)
    }

    @Test fun searchIsBuiltInEntry() {
        assertTrue(isBuiltInEntry(GLIMPSE_SEARCH))
        assertEquals("glimpse:search", GLIMPSE_SEARCH)
    }

    @Test fun keepKeepsSearchEntry() {
        val t = TextLayout(apps = listOf(GLIMPSE_SEARCH, "gone/App"))
        assertEquals(listOf(GLIMPSE_SEARCH), t.withoutMissing(emptySet()).apps)
        assertEquals(listOf(GLIMPSE_SEARCH), TextModeJson.decode(TextModeJson.encode(TextLayout(apps = listOf(GLIMPSE_SEARCH))))!!.apps)
    }
}
