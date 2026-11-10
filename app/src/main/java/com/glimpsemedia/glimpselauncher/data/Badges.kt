package com.glimpsemedia.glimpselauncher.data

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat

/** The number of waiting notifications for each app, shown as a number on its tile. */
object Badges {
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())

    fun forPackage(pkg: String): Int = counts[pkg] ?: 0

    /** Whether the person has given this launcher notification access in the phone's settings. */
    fun allowed(context: Context): Boolean = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
}

/**
 * Reads only how many notifications each app has waiting (the number the app gives, or one for each); the words in
 * them are never read, stored or sent. It only runs if the person turns on notification access for this launcher.
 */
class BadgeService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = try { activeNotifications } catch (_: Exception) { null } ?: return
        val counts = HashMap<String, Int>()
        for (n in list) {
            val note = n.notification
            if (!n.isClearable || (note.flags and Notification.FLAG_GROUP_SUMMARY) != 0) continue
            counts[n.packageName] = (counts[n.packageName] ?: 0) + maxOf(1, note.number)
        }
        Badges.counts = counts
    }

    override fun onListenerDisconnected() { Badges.counts = emptyMap() }
}
