package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TagOrderTest {
    private val l = Layout(listOf(Page()), emptyList(), tags = listOf(AppTag("Games", setOf("g/G")), AppTag("Work", setOf("w/W")), AppTag("News", emptySet())))
    private fun names(x: Layout) = x.tags.map { it.name }

    @Test fun aTagMovesUpAndDown() {
        assertEquals(listOf("Work", "Games", "News"), names(l.withTagMoved("Work", -1)))
        assertEquals(listOf("Games", "News", "Work"), names(l.withTagMoved("Work", +1)))
    }

    @Test fun aTagKeepsItsAppsWhenMoved() = assertEquals(setOf("w/W"), l.withTagMoved("Work", -1).appsTagged("Work"))

    @Test fun theEndsDoNotMovePast() {
        assertSame(l, l.withTagMoved("Games", -1))
        assertSame(l, l.withTagMoved("News", +1))
        assertFalse(l.canMoveTag("Games", -1))
        assertFalse(l.canMoveTag("News", +1))
        assertTrue(l.canMoveTag("Work", -1) && l.canMoveTag("Work", +1))
    }

    @Test fun anUnknownTagChangesNothing() = assertSame(l, l.withTagMoved("Music", -1))

    @Test fun theOrderIsSaved() = assertEquals(listOf("Work", "Games", "News"), names(LayoutJson.decode(LayoutJson.encode(l.withTagMoved("Work", -1)))!!))

    @Test fun startAgainKeepsTheTags() {
        val fresh = DefaultLayout.build(listOf("a/A"), emptyList())
        val again = l.startedAgainAs(fresh)
        assertEquals(l.tags, again.tags)
        assertEquals(fresh.pages, again.pages)
    }
}
