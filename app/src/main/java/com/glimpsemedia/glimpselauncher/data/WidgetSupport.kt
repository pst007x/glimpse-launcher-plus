package com.glimpsemedia.glimpselauncher.data

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle

/** A widget the phone offers: what it is called, its picture and the least room it wants, in dp. */
data class WidgetChoice(
    val provider: ComponentName,
    val label: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val icon: Drawable?,
    /** The app the widget comes from, when its name differs from the widget's. */
    val appLabel: String = "",
)

/**
 * The phone's widgets: lists them, asks the phone's permission to use one (and lets the widget ask its own set-up
 * questions), makes the view that shows it, and forgets it again. A launcher is what holds a widget, so this is
 * the "widget host".
 */
class WidgetSupport(private val activity: Activity) {
    private val manager = AppWidgetManager.getInstance(activity)
    private val host = AppWidgetHost(activity, HOST_ID)
    private var pendingDone: ((Int?) -> Unit)? = null
    private var pendingId = -1
    private var pendingProvider: ComponentName? = null

    fun start() = try { host.startListening() } catch (_: Exception) { }
    fun stop() = try { host.stopListening() } catch (_: Exception) { }

    /** Every widget on the phone, A to Z. */
    fun choices(): List<WidgetChoice> = try {
        manager.installedProviders
            .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
            .map {
                val density = activity.resources.displayMetrics.density
                val label = it.loadLabel(activity.packageManager)
                // What the widget wants: its smallest size, or (Android 12 and newer) the squares it asks for
                var width = (it.minWidth / density).toInt()
                var height = (it.minHeight / density).toInt()
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    width = maxOf(width, it.targetCellWidth * 74)
                    height = maxOf(height, it.targetCellHeight * 74)
                }
                val app = try { activity.packageManager.getApplicationLabel(activity.packageManager.getApplicationInfo(it.provider.packageName, 0)).toString() } catch (_: Exception) { "" }
                WidgetChoice(
                    it.provider, label, width, height,
                    try { it.loadPreviewImage(activity, 0) ?: it.loadIcon(activity, 0) } catch (_: Exception) { null },
                    if (app.equals(label, ignoreCase = true)) "" else app,
                )
            }
            .sortedBy { it.label.lowercase() }
    } catch (_: Exception) {
        emptyList()
    }

    fun info(id: Int): AppWidgetProviderInfo? = try { manager.getAppWidgetInfo(id) } catch (_: Exception) { null }

    /** The view that shows widget [id], or null if the phone no longer has it. */
    fun createView(id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        return try { host.createView(activity, id, info).also { it.setAppWidget(id, info); it.setPadding(0, 0, 0, 0) } } catch (_: Exception) { null }
    }

    /**
     * Tells the widget how much room it has, in dp, so it can choose its layout, and has it draw itself again.
     * Sent as ONE complete update (category, sizes, min and max together) each time its view is made, which the
     * phone always passes on to the widget: separate updates each restart the widget's drawing, and some widgets
     * (Samsung's Glance widgets: Calendar, Reminder) fail on an in-between state; with no update at all, a widget
     * that once failed keeps showing "Can't show content".
     */
    fun resize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        try {
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    // Many widgets (Samsung's, Google's) need the list of sizes.
                    putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, arrayListOf(android.util.SizeF(widthDp.toFloat(), heightDp.toFloat())))
                }
            }
            manager.updateAppWidgetOptions(view.appWidgetId, options)
        } catch (_: Exception) { }
    }

    fun delete(id: Int) {
        try { host.deleteAppWidgetId(id) } catch (_: Exception) { }
    }

    /**
     * Starts adding [provider]: the phone asks the person to allow it, the widget may ask its own questions, then
     * [done] gets the widget's number, or null if it was not added.
     */
    fun add(provider: ComponentName, done: (Int?) -> Unit) {
        if (pendingDone != null) return
        val id = try { host.allocateAppWidgetId() } catch (_: Exception) { done(null); return }
        pendingDone = done
        pendingId = id
        pendingProvider = provider
        if (try { manager.bindAppWidgetIdIfAllowed(id, provider) } catch (_: Exception) { false }) {
            configureOrFinish()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            try { activity.startActivityForResult(intent, REQUEST_BIND) } catch (_: Exception) { finish(false) }
        }
    }

    private fun configureOrFinish() {
        val info = info(pendingId)
        if (info?.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, pendingId, 0, REQUEST_CONFIGURE, null)
            } catch (_: Exception) { finish(false) }
        } else finish(info != null)
    }

    private fun finish(ok: Boolean) {
        val done = pendingDone
        val id = pendingId
        pendingDone = null
        pendingId = -1
        pendingProvider = null
        if (!ok) delete(id)
        done?.invoke(if (ok) id else null)
    }

    /** Called from the activity's result: true if this was one of ours. */
    fun handleResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_BIND && requestCode != REQUEST_CONFIGURE) return false
        if (pendingDone == null) return true
        when {
            // Some widgets' own set-up screens (Samsung's clocks) answer "cancelled" even after Save: a widget the
            // phone still has is kept; the person can remove it from its menu.
            requestCode == REQUEST_CONFIGURE && resultCode != Activity.RESULT_OK -> finish(info(pendingId) != null)
            resultCode != Activity.RESULT_OK -> finish(false)
            requestCode == REQUEST_BIND -> configureOrFinish()
            else -> finish(true)
        }
        return true
    }

    companion object {
        private const val HOST_ID = 4242
        const val REQUEST_BIND = 7101
        const val REQUEST_CONFIGURE = 7102
    }
}
