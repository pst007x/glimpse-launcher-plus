package com.glimpsemedia.glimpselauncher.data

/** The file ending for a note picture of [mime] (from the gallery), so a PNG stays a PNG; anything else is a photo. */
fun pictureExtension(mime: String?): String = when (mime?.lowercase()) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/gif" -> "gif"
    "image/heic" -> "heic"
    "image/heif" -> "heif"
    else -> "jpg"
}

/** The kind of picture a note picture file is, by its ending, for sharing it. */
fun pictureMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "heic" -> "image/heic"
    "heif" -> "image/heif"
    else -> "image/jpeg"
}

/** One kind for several pictures: theirs when they are all the same, else any picture. */
fun picturesMime(names: List<String>): String = names.map { pictureMime(it) }.distinct().singleOrNull() ?: "image/*"

/** The power-of-two shrink that brings a [width] × [height] picture's longer side to at most [maxSide]. */
fun decodeSample(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / sample > maxSide) sample *= 2
    return sample
}
