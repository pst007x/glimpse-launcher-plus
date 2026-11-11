package com.glimpsemedia.glimpselauncher.model

/**
 * The folder menu's badge line: "Add a badge" or "Change the badge" for a folder while the folder-icon look is on
 * ([folderStyle] 1), and nothing otherwise (the badge only shows on the folder picture).
 */
fun badgeMenuLine(item: Item, folderStyle: Int, badge: TextIcon?): String? = when {
    item !is Item.Folder || folderStyle != 1 -> null
    badge == null -> "Add a badge"
    else -> "Change the badge"
}

/** How a colour badge is saved in [Layout.folderBadges]: this, then the subject's [TextIcon] name. */
const val COLOUR_BADGE = "color:"
