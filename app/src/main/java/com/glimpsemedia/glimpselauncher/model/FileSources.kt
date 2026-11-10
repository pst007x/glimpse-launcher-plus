package com.glimpsemedia.glimpselauncher.model

/** Where Search looks for file and folder names: the whole phone ([phone], Plus only) or the picked folders. */
data class FileSources(val phone: Boolean, val folders: Boolean) {
    val any get() = phone || folders
}

/**
 * All files and folders, when it is on and Android allows it, covers the whole phone, so the picked folders are not
 * needed; without the permission (taken away later) Search falls back to the picked folders, if they are on.
 */
fun fileSources(allFilesOn: Boolean, permitted: Boolean, foldersOn: Boolean): FileSources {
    val phone = allFilesOn && permitted
    return FileSources(phone = phone, folders = foldersOn && !phone)
}
