package com.glimpsemedia.glimpselauncher.model

import kotlin.math.abs

/** The built-in icon pack: WithIcons Dock (Evergrow, MIT) recoloured into the Glimpse palette. */
const val GLIMPSE_COLOUR = "glimpse:colour"

/** The Glimpse palette (the app icon's colours). */
const val GC_CYAN = 0xFF00BEFF.toInt()
const val GC_PINK = 0xFFFF40A0.toInt()
const val GC_YELLOW = 0xFFFFC400.toInt()
const val GC_GREEN = 0xFF00D68F.toInt()

/**
 * The phone's main apps that get a Glimpse Colour drawing, in the order that decides when an app is several (a phone
 * app that is also the contacts app gets the phone). [drawable] is the name after `gc_` in res/drawable.
 */
enum class GcRole(val drawable: String) {
    PHONE("phone"), MESSAGES("message_circle"), CAMERA("camera"), GALLERY("image"), CALENDAR("calendar"), CLOCK("alarm_clock"),
    CALCULATOR("calculator"), CONTACTS("user_circle"), EMAIL("mail"), BROWSER("globe"), MAPS("map"),
    MUSIC("music_note"), FILES("folder"), STORE("store"), SETTINGS("settings"),
}

/** The drawing for an app that is one or more of the main apps, or null (it keeps its own logo, in Glimpse colours). */
fun gcDrawableFor(roles: Set<GcRole>): String? = GcRole.entries.firstOrNull { it in roles }?.drawable

/** The built-in tiles' Glimpse Colour drawings. */
val GC_BUILT_IN = mapOf("glimpse:allapps" to "grid_2x2", "glimpse:search" to "search", "glimpse:emergency" to "siren")

/** The colour folder badges: each mono badge subject's Glimpse Colour drawing. */
val GC_BADGES = mapOf(
    "Folder" to "folder", "Files" to "files", "Images" to "images", "Messages" to "messages", "Videos" to "video_camera",
    "Work" to "briefcase", "Film" to "clapperboard", "Shopping" to "shopping_bag", "Music" to "music_note", "Travel" to "plane",
    "Search" to "search", "Health" to "heart_pulse", "AI" to "sparkles", "Money" to "wallet", "Security" to "shield_check",
    "Settings" to "settings", "Games" to "gamepad", "Social" to "users", "News" to "newspaper", "Tools" to "wrench",
    "Star" to "star",
    "Phone" to "phone",
    "Email" to "mail",
    "Camera" to "camera",
    "Calendar" to "calendar",
    "Maps" to "map_pin",
    "Weather" to "cloud_sun",
    "Food" to "utensils",
    "Fitness" to "dumbbell",
    "Sport" to "football",
    "Kids" to "teddy_bear",
    "School" to "graduation_cap",
    "Books" to "book_open",
    "Podcasts" to "podcast",
    "TV" to "tv",
    "Notes" to "notebook",
    "Home" to "home",
    "Car" to "car",
    "Cloud" to "cloud",
    "Documents" to "file_text",
    "Pets" to "paw_print",
)

/** Every Glimpse Colour drawing (all of [GC_CATEGORIES]). */
val GC_ALL: List<String> get() = GC_CATEGORIES.flatMap { it.second }.distinct()

private val BY_KEY = listOf(GC_CYAN, GC_PINK, GC_YELLOW, GC_GREEN)

/**
 * The Glimpse colour of an app shown with its own logo: from its Android category (`ApplicationInfo.category`: 0 game,
 * 1 audio, 2 video, 3 image, 4 social, 5 news, 6 maps, 7 productivity, 8 accessibility), else from Glimpse's topics,
 * else fixed by its key so it never changes.
 */
fun gcColourFor(category: Int, topics: Set<Topic>, key: String): Int = when (category) {
    4 -> GC_PINK
    1, 2, 8 -> GC_CYAN
    0, 3 -> GC_YELLOW
    5, 6, 7 -> GC_GREEN
    else -> when {
        topics.any { it == Topic.SOCIAL || it == Topic.MESSAGES || it == Topic.PHONE } -> GC_PINK
        topics.any { it == Topic.VIDEO || it == Topic.MUSIC || it == Topic.INTERNET || it == Topic.EMAIL || it == Topic.ACCESSIBILITY } -> GC_CYAN
        topics.any { it == Topic.PHOTOS || it == Topic.GAMES } -> GC_YELLOW
        topics.any { it == Topic.MAPS || it == Topic.NEWS || it == Topic.OFFICE } -> GC_GREEN
        else -> BY_KEY[abs(key.hashCode() % BY_KEY.size)]
    }
}

/** WithIcons' licence, shown in full in About (MIT asks for the notice with every copy). */
const val WITHICONS_LICENCE = "WithIcons by Evergrow — github.com/withevergrow/withicons\n\nMIT License\n\nCopyright (c) Evergrow\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."
