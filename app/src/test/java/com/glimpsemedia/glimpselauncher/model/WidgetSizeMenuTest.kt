package com.glimpsemedia.glimpselauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeMenuTest {
    @Test fun theBigGridKeepsItsShortList() {
        assertEquals(WidgetSizeMenu.List(listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 2 to 3, 2 to 4)), widgetSizeMenu(2, PAGE_ROWS))
    }

    @Test fun compactGridsAskAcrossAndDownSeparately() {
        assertEquals(WidgetSizeMenu.AcrossAndDown(5, 6), widgetSizeMenu(5, 6))   // 11 buttons, not 30
        assertEquals(WidgetSizeMenu.AcrossAndDown(4, 4), widgetSizeMenu(4, 4))
    }

    @Test fun pickingAcrossKeepsTheHeight() {
        assertEquals(3 to 2, sizeWithAcross(Item.Widget(1, "p", 1, 2), 3))
        assertEquals(1 to 5, sizeWithDown(Item.Widget(1, "p", 1, 2), 5))
    }
}
