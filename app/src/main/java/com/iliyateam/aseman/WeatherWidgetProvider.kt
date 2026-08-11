package com.iliyateam.aseman

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class WeatherWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.iliyateam.aseman.REFRESH_WIDGET"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, mgr, id, false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))

            /* ۱) وضعیت «در حال بروزرسانی» را نشان بده */
            for (id in ids) update(context, mgr, id, true)


            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    context, Intent(context, RefreshService::class.java)
                )
            } catch (_: Exception) { }


        }
    }

    private fun update(context: Context, mgr: AppWidgetManager, id: Int, refreshing: Boolean) {
        val sp = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
        val city = sp.getString("city", "آسمان") ?: "آسمان"
        val line = sp.getString("line", "…") ?: "…"
        val emoji = if (refreshing) "🔄" else (sp.getString("emoji", "☁️") ?: "☁️")
        val updatedAt = sp.getLong("updated_at", 0L)

        val updatedText = when {
            refreshing -> "در حال بروزرسانی…"
            updatedAt == 0L -> "—"
            else -> {
                val clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                    .format(java.util.Date(updatedAt))
                "بروزرسانی: $clock"
            }
        }

        val dark = sp.getString("dark", "1") == "1"
        val solid = sp.getString("bg", "trans") == "solid"

        val views = RemoteViews(context.packageName, R.layout.widget_weather).apply {
            setInt(
                R.id.w_root, "setBackgroundResource",
                when {
                    !solid -> R.drawable.widget_bg
                    dark -> R.drawable.widget_bg_solid
                    else -> R.drawable.widget_bg_solid_light
                }
            )
            setTextColor(R.id.w_city, if (solid && !dark) 0xFF212121.toInt() else 0xFFFFFFFF.toInt())
            setTextColor(R.id.w_line, if (solid && !dark) 0xFF616161.toInt() else 0xFFE0E0E0.toInt())
            setTextColor(R.id.w_brand, if (solid && !dark) 0xFF3E7CB1.toInt() else 0xFF90CAF9.toInt())
            setTextColor(R.id.w_updated, if (solid && !dark) 0xFF757575.toInt() else 0xFFB0BEC5.toInt())

            setTextViewText(R.id.w_city, city)
            setTextViewText(R.id.w_line, line)
            setTextViewText(R.id.w_emoji, emoji)
            setTextViewText(R.id.w_updated, updatedText)

            setOnClickPendingIntent(
                R.id.w_root,
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setOnClickPendingIntent(
                R.id.w_refresh,
                PendingIntent.getBroadcast(
                    context, 1,
                    Intent(context, WeatherWidgetProvider::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        mgr.updateAppWidget(id, views)
    }
}