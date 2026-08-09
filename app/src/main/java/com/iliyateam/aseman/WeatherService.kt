package com.iliyateam.aseman

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.edit
import com.iliyateam.aseman.data.WeatherApi
import com.iliyateam.aseman.data.WeatherResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeatherService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopsStarted = false
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(101, lastKnownNotification())
        scheduleSelfWake()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!loopsStarted) {
            loopsStarted = true
            startLoops()
        }
        scope.launch {
            var tries = 0
            while (tries < 3) {
                val ok = try { update(); true } catch (_: Exception) { false }
                if (ok) break
                tries++
                delay(10_000)
            }
        }
        return START_STICKY
    }

    private fun startLoops() {
        scope.launch {
            while (true) {
                delay(nextDelay())
                try { update() } catch (_: Exception) { }
            }
        }
        scope.launch {
            applicationContext.dataStore.data.debounce(2000).collect {
                try { update() } catch (_: Exception) { }
            }
        }
    }

    private suspend fun nextDelay(): Long {
        val mins = (applicationContext.dataStore.data.first()[Prefs.KEY_REFRESH] ?: "30")
            .toIntOrNull() ?: 30
        return (if (mins > 0) mins else 60) * 60_000L
    }

    private suspend fun update() {
        val pd = applicationContext.dataStore.data.first()
        val saved = pd[Prefs.KEY_NOTIF_CITY] ?: pd[Prefs.KEY_LAST] ?: return
        val lang = pd[Prefs.KEY_LANG] ?: "fa"
        val p = saved.split("|")
        if (p.size < 3) return
        val w = WeatherApi.instance.getWeather(p[0].toDouble(), p[1].toDouble())
        val c = w.current
        val emoji = emojiOf(c.code, c.isDay == 1)
        val text = try {
            val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            "${c.temp.toInt()}° | ${descOf(c.code, lang == "fa")} | ${dayLabel(iso, true)} • ${dayLabel(iso, false)}"
        } catch (_: Exception) {
            "${c.temp.toInt()}° | ${descOf(c.code, lang == "fa")}"
        }
        val city = cityDisplayName(p[2], lang == "fa")
        val n = buildNotification(text, city, emoji)
        startForeground(101, n)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(101, n)

        if (pd[Prefs.KEY_ALERTS] != "0") checkAlerts(w, lang)

        /* وضعیت dark/light برای ویجت */
        val mode = pd[Prefs.KEY_MODE] ?: "auto"
        val sysDark = (applicationContext.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val darkNow = mode == "dark" || mode == "amoled" || (mode == "auto" && sysDark)

        /* ذخیره برای ویجت */
        applicationContext.getSharedPreferences("widget", MODE_PRIVATE).edit()
            .putString("city", city)
            .putString("line", text)
            .putString("emoji", weatherEmoji(c.code, c.isDay == 1))
            .putString("dark", if (darkNow) "1" else "0")
            .putLong("updated_at", System.currentTimeMillis())
            .apply()

        /* اصرار به رفرش ویجت */
        val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(
                android.content.ComponentName(applicationContext, WeatherWidgetProvider::class.java)
            )
        if (ids.isNotEmpty()) {
            applicationContext.sendBroadcast(
                android.content.Intent(applicationContext, WeatherWidgetProvider::class.java)
                    .setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("weather", "آب‌وهوا", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            )
            nm.createNotificationChannel(
                NotificationChannel("alerts", "هشدارها", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    /* --- بیدارکنندهٔ AlarmManager --- */
    private fun scheduleSelfWake() {
        try {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            val i = Intent(this, WakeReceiver::class.java)
            val pi = PendingIntent.getBroadcast(this, 999, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 15 * 60_000L,
                15 * 60_000L,
                pi
            )
        } catch (_: Exception) { }
    }

    class WakeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, WeatherService::class.java)
                )
            } catch (_: Exception) { }
        }
    }

    private fun emojiIcon(emoji: String): IconCompat {
        val size = 96
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = 72f
        p.textAlign = Paint.Align.CENTER
        val r = Rect()
        p.getTextBounds(emoji, 0, emoji.length, r)
        c.drawText(emoji, size / 2f, size / 2f - r.exactCenterY(), p)
        return IconCompat.createWithBitmap(b)
    }
    private fun lastKnownNotification(): android.app.Notification {
        val sp = getSharedPreferences("widget", MODE_PRIVATE)
        val city = sp.getString("city", "") ?: ""
        val line = sp.getString("line", "") ?: ""
        val emoji = sp.getString("emoji", "☁️") ?: "☁️"
        return if (line.isEmpty())
            buildNotification("در حال به‌روزرسانی…", "آسمان", "☁️")
        else
            buildNotification(line, city, emoji)
    }
    private fun buildNotification(text: String, city: String, emoji: String) =
        NotificationCompat.Builder(this, "weather")
            .setSmallIcon(emojiIcon(emoji))
            .setContentTitle(city.ifBlank { "آسمان" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private suspend fun checkAlerts(w: WeatherResponse, lang: String) {
        try {
            val nowHour = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US).format(java.util.Date())
            val idx = maxOf(0, w.hourly.time.indexOf(nowHour))
            val next = w.hourly.code.subList(idx, minOf(w.hourly.code.size, idx + 3))
            val type = when {
                next.any { it in 95..99 } -> "storm"
                next.any { it in 71..77 || it in 85..86 } -> "snow"
                next.any { it in 51..67 || it in 80..82 } -> "rain"
                else -> null
            } ?: return
            val last = (applicationContext.dataStore.data.first()[Prefs.KEY_ALERT_LAST] ?: "0").toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (now - last < 6 * 3600_000L) return
            applicationContext.dataStore.edit { it[Prefs.KEY_ALERT_LAST] = "$now" }
            val msg = when (type) {
                "storm" -> if (lang == "fa") "رعدوبرق در راه است ⛈️" else "Thunderstorm on the way ⛈️"
                "snow" -> if (lang == "fa") "بارش برف در راه است ❄️" else "Snow on the way ❄️"
                else -> if (lang == "fa") "تا ساعاتی دیگر باران می‌بارد ☔" else "Rain expected soon ☔"
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(202, alertNotification(msg))
        } catch (_: Exception) { }
    }

    private fun alertNotification(msg: String) =
        NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(R.drawable.aseman_icon)
            .setContentTitle("آسمان")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}