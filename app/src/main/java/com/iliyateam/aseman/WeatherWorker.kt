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
import java.io.IOException
import retrofit2.HttpException
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

    /* برای Expedited Work — اندروید به آن امتیاز foreground می‌دهد */
    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val sp = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)

        return androidx.work.ForegroundInfo(
            100,
            buildNotification(
                sp.getString("line", "…") ?: "…",
                sp.getString("city", "آسمان") ?: "آسمان",
                sp.getString("emoji", "☁️") ?: "☁️"
            )
        )
    }
    override suspend fun doWork(): Result {
        return try {
            val pd = ctx.applicationContext.dataStore.data.first()
            val saved = pd[Prefs.KEY_NOTIF_CITY]
                ?.takeIf { it.isNotBlank() }
                ?: pd[Prefs.KEY_LAST]
                ?: return Result.success()
            val lang = pd[Prefs.KEY_LANG] ?: "fa"
            val p = saved.split("|")
            if (p.size < 3) return Result.success()

            val tempUnit = if (pd[Prefs.KEY_UTEMP] == "f") "fahrenheit" else "celsius"
            val windUnit = pd[Prefs.KEY_UWIND] ?: "kmh"

            val w = WeatherApi.instance.getWeather(
                p[0].toDouble(),
                p[1].toDouble(),
                timezone = "auto",
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
                .putString("last_error", "")
                .apply()

            refreshWidgets()
            Result.success()
        } catch (e: Exception) {
            ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
                .putString("last_error", "${e.javaClass.simpleName}: ${e.message}")
                .apply()

            refreshWidgets()

            when {
                e is IOException -> Result.retry()

                e is HttpException && (
                        e.code() == 408 ||
                                e.code() == 429 ||
                                e.code() >= 500
                        ) -> Result.retry()

                else -> Result.failure()
            }
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

            val nowHour = w.current.time.take(13) + ":00"

            val idx = w.hourly.time.indexOf(nowHour)
            if (idx < 0) return

            val next = w.hourly.code.subList(
                idx,
                minOf(w.hourly.code.size, idx + 3)
            )
            val type = when {
                next.any { it in 95..99 } -> "storm"
                next.any { it in 71..77 || it in 85..86 } -> "snow"
                next.any { it in 51..67 || it in 80..82 } -> "rain"
                else -> null
            } ?: return
            val savedLast = ctx.applicationContext.dataStore.data.first()[Prefs.KEY_ALERT_LAST]
            val parts = savedLast?.split("|")

            val lastType = parts?.getOrNull(0)
            val lastTime = parts?.getOrNull(1)?.toLongOrNull() ?: 0L

            val now = System.currentTimeMillis()

            if (lastType == type && now - lastTime < 6 * 3600_000L) {
                return
            }

            ctx.applicationContext.dataStore.edit {
                it[Prefs.KEY_ALERT_LAST] = "$type|$now"
            }
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

    /* آپدیت مستقیم ویجت — بدون broadcast */
    private fun refreshWidgets() {
        try {
            val mgr = android.appwidget.AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(ctx, WeatherWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val sp = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val city = sp.getString("city", "آسمان") ?: "آسمان"
            val line = sp.getString("line", "…") ?: "…"
            val emoji = sp.getString("emoji", "☁️") ?: "☁️"
            val updatedAt = sp.getLong("updated_at", 0L)
            val dark = sp.getString("dark", "1") == "1"
            val solid = sp.getString("bg", "trans") == "solid"

            val updatedText = if (updatedAt == 0L) "—" else {
                val clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                    .format(java.util.Date(updatedAt))
                "بروزرسانی: $clock"
            }

            val refreshIntent = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(ctx, WeatherWidgetProvider::class.java)
                    .setAction(WeatherWidgetProvider.ACTION_REFRESH),
                PendingIntent.FLAG_IMMUTABLE
            )
            val openIntent = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            for (id in ids) {
                val views = android.widget.RemoteViews(ctx.packageName, R.layout.widget_weather).apply {
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
                    setOnClickPendingIntent(R.id.w_root, openIntent)
                    setOnClickPendingIntent(R.id.w_refresh, refreshIntent)
                }
                mgr.updateAppWidget(id, views)
            }
        } catch (_: Exception) { }
    }
}