package com.glimpsemedia.glimpselauncher.model

/** The app's name in each edition: Play's Glimpse Launcher, or Glimpse Launcher Plus (outside Google Play). */
fun appName(plus: Boolean): String = if (plus) "Glimpse Launcher Plus" else "Glimpse Launcher"

/** Where Glimpse Launcher Plus finds its newest version (it is not on Google Play, so it does not update itself). */
const val PLUS_PAGE = "https://glimpse-media.com/glimpse/plus"
