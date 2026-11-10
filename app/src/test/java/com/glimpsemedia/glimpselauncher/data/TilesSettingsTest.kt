package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TilesSettingsTest {
    @Test fun shapeFromStoredKeepsDefaultAndCircle() {
        assertEquals(SHAPE_DEFAULT, shapeFromStored(0)); assertEquals(SHAPE_CIRCLE, shapeFromStored(2))
        // The old Square looked like the default; it reads as Default now.
        assertEquals(SHAPE_DEFAULT, shapeFromStored(1))
        assertEquals(SHAPE_DEFAULT, shapeFromStored(3)); assertEquals(SHAPE_DEFAULT, shapeFromStored(-1))
    }

    @Test fun tileAndEmptyPlaceShapesStartAtDefault() {
        assertEquals(SHAPE_DEFAULT, Settings().tileShape)
        assertEquals(SHAPE_DEFAULT, Settings().slotShape)
        assertEquals(SHAPE_DEFAULT, Settings().iconShape)
    }

    @Test fun oldSettingsGetTheDefaults() {
        assertEquals(true, Settings().showTiles)
        assertEquals(0, Settings().iconShape)
        // A backup from before has neither key: they are left out, so the phone's own (default) values stand.
        val backup = mapOf("darkTheme" to PrefValue.B(true))
        val current = mapOf("darkTheme" to PrefValue.B(false), "showTiles" to PrefValue.B(true), "iconShape" to PrefValue.I(0))
        assertEquals(backup, Backup.matchTypes(backup, current))
    }
}
