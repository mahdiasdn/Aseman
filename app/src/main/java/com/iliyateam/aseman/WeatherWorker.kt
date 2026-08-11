package com.iliyateam.aseman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iliyateam.aseman.data.WeatherApi
import com.iliyateam.aseman.data.WeatherResponse
import kotlinx.coroutines.flow.first

class WeatherWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
        val pd = ctx.applicationContext.dataStore.data.first()
        val saved = pd[Prefs.KEY_NOTIF_CITY] ?: pd[Prefs.KEY_LAST] ?: return Result.success()
        val lang = pd[Prefs.KEY_LANG] ?: "fa"
        val p = saved.split("|")
        if (p.size < 3) return Result.success()

        val tempUnit = if (pd[Prefs.KEY_UTEMP] == "f") "fahrenheit" else "celsius"
        val windUnit = pd[Prefs.KEY_UWIND] ?: "kmh"

            val w = WeatherApi.instance.getWeather(
                p[0].toDouble(),
                p[1].toDouble(),
                timezone = "UTC",
                temperatureUnit = tempUnit,
                windSpeedUnit = windUnit
            )
        val c = w.current
        val emoji = emojiOf(c.code, c.isDay == 1)
        val text = try {
            val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            "${c.temp.toInt()}° | ${descOf(c.code, lang == "fa")} | ${dayLabel(iso, true)} • ${dayLabel(iso, false)}"
        } catch (_: Exception) {
            "${c.temp.toInt()}° | ${descOf(c.code, lang == "fa")}"
        }
        val city = cityDisplayName(p[2], lang == "fa")

        createChannel()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(101, buildNotification(text, city, emoji))

        if (pd[Prefs.KEY_ALERTS] != "0") checkAlerts(w, lang)

        val mode = pd[Prefs.KEY_MODE] ?: "auto"
        val sysDark = (ctx.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val darkNow = mode == "dark" || mode == "amoled" || (mode == "auto" && sysDark)

        ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
            .putString("city", city)
            .putString("line", text)
            .putString("emoji", weatherEmoji(c.code, c.isDay == 1))
            .putString("dark", if (darkNow) "1" else "0")
            .putLong("updated_at", System.currentTimeMillis())
            .apply()

        val ids = android.appwidget.AppWidgetManager.getInstance(ctx)
            .getAppWidgetIds(android.content.ComponentName(ctx, WeatherWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            ctx.sendBroadcast(
                Intent(ctx, WeatherWidgetProvider::class.java)
                    .setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("weather", "آب‌وهوا", NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) }
            )
            nm.createNotificationChannel(
                NotificationChannel("alerts", "هشدارها", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun emojiIcon(emoji: String): IconCompat {
        val size = 96
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = 72f
        p.textAlign = Paint.Align.CENTER
        val r = Rect()
        p.getTextBounds(emoji, 0, emoji.length, r)
        cv.drawText(emoji, size / 2f, size / 2f - r.exactCenterY(), p)
        return IconCompat.createWithBitmap(b)
    }

    private fun buildNotification(text: String, city: String, emoji: String) =
        NotificationCompat.Builder(ctx, "weather")
            .setSmallIcon(emojiIcon(emoji))
            .setContentTitle(city.ifBlank { "آسمان" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx, 0,
                    Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private suspend fun checkAlerts(w: WeatherResponse, lang: String) {
        try {
            val nowHour = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
                .format(java.util.Date())
            val idx = maxOf(0, w.hourly.time.indexOf(nowHour))
            val next = w.hourly.code.subList(idx, minOf(w.hourly.code.size, idx + 3))
            val type = when {
                next.any { it in 95..99 } -> "storm"
                next.any { it in 71..77 || it in 85..86 } -> "snow"
                next.any { it in 51..67 || it in 80..82 } -> "rain"
                else -> null
            } ?: return
            val last = (ctx.applicationContext.dataStore.data.first()[Prefs.KEY_ALERT_LAST] ?: "0")
                .toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (now - last < 6 * 3600_000L) return
            ctx.applicationContext.dataStore.edit { it[Prefs.KEY_ALERT_LAST] = "$now" }
            val msg = when (type) {
                "storm" -> if (lang == "fa") "رعدوبرق در راه است ⛈️" else "Thunderstorm on the way ⛈️"
                "snow" -> if (lang == "fa") "بارش برف در راه است ❄️" else "Snow on the way ❄️"
                else -> if (lang == "fa") "تا ساعاتی دیگر باران می‌بارد ☔" else "Rain expected soon ☔"
            }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(202, alertNotification(msg))
        } catch (_: Exception) { }
    }

    private fun alertNotification(msg: String) =
        NotificationCompat.Builder(ctx, "alerts")
            .setSmallIcon(R.drawable.aseman_icon)
            .setContentTitle("آسمان")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()
}