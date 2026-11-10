package com.glimpsemedia.glimpselauncher.model

/** The person the Emergency button rings. */
data class Contact(val name: String, val number: String) {
    val isUsable: Boolean get() = number.count { it.isDigit() } >= 3
}
