package com.glimpsemedia.glimpselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotePicturesTest {
    @Test fun aPictureKeepsItsOwnKind() {
        assertEquals("png", pictureExtension("image/png"))
        assertEquals("webp", pictureExtension("image/webp"))
        assertEquals("gif", pictureExtension("image/gif"))
        assertEquals("heic", pictureExtension("image/heic"))
        assertEquals("jpg", pictureExtension("image/jpeg"))
        assertEquals("jpg", pictureExtension(null))
        assertEquals("jpg", pictureExtension("application/x-what"))
    }

    @Test fun sharingSaysTheRightKind() {
        assertEquals("image/png", pictureMime("a.png"))
        assertEquals("image/jpeg", pictureMime("a.jpg"))
        assertEquals("image/jpeg", pictureMime("old-no-dot"))
        assertEquals("image/jpeg", picturesMime(listOf("a.jpg", "b.jpg")))
        assertEquals("image/*", picturesMime(listOf("a.jpg", "b.png")))
    }

    @Test fun aTwelveMegapixelPhotoIsNeverOpenedAtFullSize() {
        assertEquals(2, decodeSample(4000, 3000, 2048))         // 2000 × 1500, about 12 MB, not 48
        assertEquals(16, decodeSample(4000, 3000, 256))
        assertEquals(1, decodeSample(1000, 800, 2048))          // small pictures stay as they are
        assertEquals(1, decodeSample(0, 0, 256))                // an unreadable picture
    }

    @Test fun theDecodedSideIsAtMostTheSizeAskedFor() {
        for ((w, h) in listOf(4000 to 3000, 4096 to 4096, 2049 to 10, 9000 to 12000)) {
            val s = decodeSample(w, h, 2048)
            assert(maxOf(w, h) / s <= 2048) { "$w×$h → $s" }
        }
    }
}
