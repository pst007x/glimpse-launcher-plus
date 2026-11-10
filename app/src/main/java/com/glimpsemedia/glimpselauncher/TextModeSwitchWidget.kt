package com.glimpsemedia.glimpselauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.glimpsemedia.glimpselauncher.data.Settings
import com.glimpsemedia.glimpselauncher.data.SettingsStore
import com.glimpsemedia.glimpselauncher.ui.switchLook

/** Tapping the Text mode switch sends this to the launcher, which flips text mode (locked or not). */
const val TOGGLE_TEXT_MODE = "com.glimpsemedia.glimpselauncher.TOGGLE_TEXT_MODE"

/** "Text mode switch": a pill showing Text mode: On or Off; a tap switches between text mode and the tiles. */
class TextModeSwitchWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        try { manager.updateAppWidget(ids, views(context, SettingsStore(context).load())) } catch (_: Exception) { }
    }

    companion object {
        private fun views(context: Context, s: Settings): RemoteViews {
            val look = switchLook(s)
            val tap = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).setAction(TOGGLE_TEXT_MODE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return RemoteViews(context.packageName, R.layout.text_mode_switch).apply {
                setInt(R.id.switch_pill, "setColorFilter", look.back)
                setInt(R.id.switch_icon, "setColorFilter", look.ink)
                setTextViewText(R.id.switch_label, look.label)
                setTextColor(R.id.switch_label, look.ink)
                setContentDescription(R.id.switch_root, look.label)
                setOnClickPendingIntent(R.id.switch_root, tap)
            }
        }

        /** Redraws every placed switch, after the mode or the theme colours change. */
        fun refresh(context: Context, s: Settings) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, TextModeSwitchWidget::class.java))
                if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context, s))
            } catch (_: Exception) { }
        }
    }
}
