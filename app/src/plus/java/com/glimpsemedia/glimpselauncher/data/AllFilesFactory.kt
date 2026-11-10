package com.glimpsemedia.glimpselauncher.data

import androidx.activity.ComponentActivity

/** Glimpse Launcher Plus searches every file and folder name on the phone. */
fun allFilesFor(activity: ComponentActivity): AllFiles = AllFilesPhone(activity)
