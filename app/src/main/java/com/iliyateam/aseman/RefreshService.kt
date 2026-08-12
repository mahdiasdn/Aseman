package com.iliyateam.aseman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent

import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import com.iliyateam.aseman.data.WeatherApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RefreshService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        try {
            startForeground(100, placeholder())
        } catch (_: Exception) { }
        scope.launch {
            try {
                val pd = applicationContext.dataStore.data.first()
                val saved = pd[Prefs.KEY_NOTIF_CITY]
                    ?.takeIf { it.isNotBlank() }
                    ?: pd[Prefs.KEY_LAST]
                val lang = pd[Prefs.KEY_LANG] ?: "fa"
                val p = saved?.split("|")
                if (p != null && p.size == 3) {
                    val tempUnit = if (pd[Prefs.KEY_UTEMP] == "f") "fahrenheit" else "celsius"
                    val windUnit = pd[Prefs.KEY_UWIND] ?: "kmh"
                    val w = WeatherApi.instance.getWeather(
                        p[0].toDouble(), p[1].toDouble(),
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

                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(101, buildNotification(text, city, emoji))

                    val mode = pd[Prefs.KEY_MODE] ?: "auto"
                    val sysDark = (resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                            android.content.res.Configuration.UI_MODE_NIGHT_YES
                    val darkNow = mode == "dark" || mode == "amoled" || (mode == "auto" && sysDark)

                    getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
                        .putString("city", city)
                        .putString("line", text)
                        .putString("emoji", weatherEmoji(c.code, c.isDay == 1))
                        .putString("dark", if (darkNow) "1" else "0")
                        .putLong("updated_at", System.currentTimeMillis())
                        .apply()

                    refreshWidgets()
                }
            } catch (_: Exception) {
            } finally {
                getSharedPreferences("widget", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("refresh_in_progress", false)
                    .apply()

                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("weather", "آب‌وهوا", NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) }
            )
        }
    }

    private fun placeholder() =
        NotificationCompat.Builder(this, "weather")
            .setSmallIcon(R.drawable.aseman_icon)
            .setContentTitle("آسمان")
            .setContentText("در حال بروزرسانی…")
            .setOngoing(true)
            .setSilent(true)
            .build()


    private fun buildNotification(text: String, city: String, emoji: String) =
        NotificationCompat.Builder(this, "weather")
            .setSmallIcon(R.drawable.aseman_icon)
            .setContentTitle(city.ifBlank { "آسمان" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setAutoCancel(false)
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

    private fun refreshWidgets() {
        try {
            val mgr = android.appwidget.AppWidgetManager.getInstance(this)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(this, WeatherWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val sp = getSharedPreferences("widget", Context.MODE_PRIVATE)
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
                this, 1,
                Intent(this, WeatherWidgetProvider::class.java)
                    .setAction(WeatherWidgetProvider.ACTION_REFRESH),
                PendingIntent.FLAG_IMMUTABLE
            )
            val openIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            for (id in ids) {
                val views = android.widget.RemoteViews(packageName, R.layout.widget_weather).apply {
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