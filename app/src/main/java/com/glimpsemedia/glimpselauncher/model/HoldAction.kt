package com.glimpsemedia.glimpselauncher.model

/** What holding a tile does while the screens are locked. One per tile; never on the Emergency tile or widgets. */
sealed interface HoldAction {
    data class OpenApp(val key: String) : HoldAction
    data class Web(val url: String) : HoldAction
    data class Call(val number: String) : HoldAction
    data class Text(val number: String) : HoldAction
    data class Phone(val what: PhoneAction) : HoldAction
    /** Slides the notes screen in. */
    data object Notes : HoldAction
}

/** The phone's own things a hold action can open or switch, in the order the menu lists them. */
enum class PhoneAction(val title: String) {
    Torch("Torch (on/off)"), Camera("Camera"), Alarms("Alarms"), Timer("Timer"),
    Wifi("Wi-Fi settings"), Bluetooth("Bluetooth settings"), Sound("Sound settings"),
    Display("Display settings"), Battery("Battery settings"), Location("Location settings"),
    DoNotDisturb("Do not disturb settings"),
}

/** The name a screen tile's hold action is kept under (its colour key), or null for tiles that never have one. */
val Item.holdKey: String?
    get() = when (this) {
        is Item.App, is Item.Folder, Item.AllApps, Item.Search -> colorKey
        Item.Emergency, is Item.Widget, Item.Covered -> null
    }

/** What holding the bottom bar's All apps button (locked) does: open Search, or its own hold action. */
sealed interface DockAllHold {
    data object OpenSearch : DockAllHold
    data class Action(val action: HoldAction) : DockAllHold
}

/**
 * The bar's All apps hold: with Hold All apps to search on, it opens Search and its own action ([stored]) waits,
 * untouched, for the switch to go off. Otherwise its own action, or nothing.
 */
fun dockAllAppsHold(holdToSearch: Boolean, stored: HoldAction?): DockAllHold? = when {
    holdToSearch -> DockAllHold.OpenSearch
    stored != null -> DockAllHold.Action(stored)
    else -> null
}

/** The name a bottom-bar icon's hold action is kept under; null is the bar's All apps button. */
fun dockHoldKey(appKey: String?): String = "dock:" + (appKey ?: "all")

/**
 * A web address for a hold action: `http://` and `https://` addresses as typed, a bare one with `https://` added.
 * Anything else (other kinds of link, spaces, no proper host) gives null.
 */
fun webAddress(input: String): String? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val lower = text.lowercase()
    val url = when {
        // Browsers match the scheme in lower case only, so "HTTPS://" is written "https://".
        lower.startsWith("http://") -> "http://" + text.substring(7)
        lower.startsWith("https://") -> "https://" + text.substring(8)
        ':' in text -> return null
        else -> "https://$text"
    }
    val host = try { java.net.URI(url).host } catch (_: Exception) { null }
    return url.takeIf { host != null && '.' in host }
}

/**
 * A number to call or text: its digits with a leading + kept, or null with fewer than 3 digits. Pauses and
 * extensions (, ; p w) are refused: dropping them would join the extension on and ring a different number.
 */
fun dialable(input: String): String? {
    val text = input.trim()
    if (text.any { it in ",;pPwW" }) return null
    val digits = text.filter { it.isDigit() }
    if (digits.length < 3) return null
    return (if (text.startsWith("+")) "+" else "") + digits
}

/** The action in a few words, for the menu ("Now: Open Maps"). */
fun HoldAction.describe(appLabel: (String) -> String?): String = when (this) {
    is HoldAction.OpenApp -> appLabel(key)?.let { "Open $it" } ?: "Open an app that is no longer on the phone"
    is HoldAction.Web -> "Open $url"
    is HoldAction.Call -> "Call $number"
    is HoldAction.Text -> "Text $number"
    is HoldAction.Phone -> what.title
    HoldAction.Notes -> "Open the notes screen"
}

/** The short note shown after the action runs; [torchNowOn] is the torch's state after switching it. */
fun HoldAction.note(appLabel: String?, torchNowOn: Boolean): String = when (this) {
    is HoldAction.OpenApp -> "Opening " + (appLabel ?: "the app")
    is HoldAction.Web -> "Opening " + (try { java.net.URI(url).host } catch (_: Exception) { null } ?: url)
    is HoldAction.Call -> "Calling $number"
    is HoldAction.Text -> "Text to $number"
    is HoldAction.Phone -> if (what == PhoneAction.Torch) (if (torchNowOn) "Torch on" else "Torch off") else what.title
    HoldAction.Notes -> "Notes"
}
