package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.WordOfDay

/**
 * Home-screen widget: "Word of the day" with its picture, the Kannada word and the English meaning.
 * Tapping it opens the app, which then says the word out loud. It refreshes a few times a day, so
 * the word changes each morning.
 */
class WordWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, views(context)) }
    }

    companion object {
        const val EXTRA_SAY_WORD = "com.example.SAY_WORD"

        fun views(context: Context): RemoteViews {
            val word = WordOfDay.today()
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_SAY_WORD, word.kannada)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val tap = PendingIntent.getActivity(
                context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return RemoteViews(context.packageName, R.layout.widget_word).apply {
                setTextViewText(R.id.widget_picture, word.picture)
                setTextViewText(R.id.widget_kannada, word.kannada)
                setTextViewText(R.id.widget_english, word.english)
                setOnClickPendingIntent(R.id.widget_root, tap)
            }
        }

        /** Redraws every widget now (e.g. when the app opens on a new day). */
        fun refreshAll(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, WordWidgetProvider::class.java))
                if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, views(context)) }
            } catch (e: Exception) {
            }
        }
    }
}
