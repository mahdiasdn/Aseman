package com.iliyateam.aseman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

import androidx.core.app.NotificationCompat

import com.iliyateam.aseman.data.WeatherApi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RefreshService : Service() {

    companion object {
        private const val TAG = "RefreshService"
        private const val NOTIFICATION_ID = 100
        private const val WEATHER_NOTIFICATION_ID = 101

        // Fired via the notification's deleteIntent when the user swipes it away.
        // Android 13+ always allows swiping a foreground-service notification, so
        // we can't block the swipe itself — instead we catch it and instantly
        // re-post the notification so it comes right back.
        const val ACTION_REPOST_NOTIFICATION =
            "com.iliyateam.aseman.ACTION_REPOST_NOTIFICATION"
    }

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        createChannel()

        try {
            val initialNotif = initialNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    WEATHER_NOTIFICATION_ID,
                    initialNotif,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    WEATHER_NOTIFICATION_ID,
                    initialNotif
                )
            }
        } catch (e: Throwable) {

            Log.e(
                TAG,
                "Failed to start foreground service",
                e
            )

            saveError(
                "ForegroundStart: ${e.javaClass.simpleName}: ${e.message}"
            )

            return START_STICKY
        }

        // The notification was just swiped away and we've already re-posted it
        // above (via initialNotification() + startForeground()). Nothing else
        // to do — no need to hit the network again just because it was dismissed.
        if (intent?.action == ACTION_REPOST_NOTIFICATION) {
            return START_STICKY
        }

        scope.launch {

            try {

                val pd =
                    applicationContext
                        .dataStore
                        .data
                        .first()

                val saved =
                    pd[Prefs.KEY_NOTIF_CITY]
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: pd[Prefs.KEY_LAST]

                if (saved.isNullOrBlank()) {

                    Log.w(
                        TAG,
                        "No saved city available for refresh"
                    )

                    saveError(
                        "No city configured"
                    )

                    return@launch
                }

                val lang =
                    pd[Prefs.KEY_LANG] ?: "fa"

                val p =
                    saved.split("|")

                if (p.size != 3) {

                    Log.e(
                        TAG,
                        "Invalid saved city format"
                    )

                    saveError(
                        "Invalid city data"
                    )

                    return@launch
                }

                val lat =
                    p[0].toDoubleOrNull()

                val lon =
                    p[1].toDoubleOrNull()

                if (
                    lat == null ||
                    lon == null ||
                    !lat.isFinite() ||
                    !lon.isFinite() ||
                    lat !in -90.0..90.0 ||
                    lon !in -180.0..180.0 ||
                    p[2].isBlank()
                ) {

                    Log.e(
                        TAG,
                        "Invalid city coordinates or name"
                    )

                    saveError(
                        "Invalid city coordinates"
                    )

                    return@launch
                }

                val tempUnit =
                    if (pd[Prefs.KEY_UTEMP] == "f") {
                        "fahrenheit"
                    } else {
                        "celsius"
                    }

                val windUnit =
                    pd[Prefs.KEY_UWIND] ?: "kmh"

                Log.d(
                    TAG,
                    "Refreshing weather for ${p[2]} ($lat, $lon)"
                )

                val repo = com.iliyateam.aseman.data.WeatherRepository.getInstance(applicationContext)
                val result = repo.fetchWeather(
                    lat = lat,
                    lon = lon,
                    tempUnit = tempUnit,
                    windSpeedUnit = windUnit
                )

                val w = result.weather
                repo.saveCache(p[2], lat, lon, w, result.air)

                val c = w.current
                val todayMax = w.daily.max.firstOrNull()?.toInt() ?: c.temp.toInt()
                val todayMin = w.daily.min.firstOrNull()?.toInt() ?: c.temp.toInt()
                val feels = c.feels.toInt()
                val isFa = lang == "fa"
                val desc = descOf(c.code, isFa)
                val emoji = weatherEmoji(c.code, c.isDay == 1)
                val pop = w.hourly.precipitationProbability.firstOrNull() ?: 0
                val humidity = w.hourly.humidity.firstOrNull() ?: c.humidity
                val uvIndex = w.daily.uvIndexMax.firstOrNull()?.toDouble() ?: 0.0
                val windPref = pd[Prefs.KEY_UWIND] ?: "kmh"
                val windUnitLabel = if (windPref == "mph") "mph" else if (windPref == "ms") "m/s" else "km/h"

                val tempStr = "${c.temp.toInt()}°"
                val highLowStr = "▲ ${todayMax}°  ▼ ${todayMin}°"
                val detailsStr = if (pop > 0) "🌧️ $pop% • 💨 ${c.wind.toInt()} $windUnitLabel" else "💧 $humidity% • 💨 ${c.wind.toInt()} $windUnitLabel"

                val text = try {
                    val iso =
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            java.util.Locale.US
                        ).format(
                            java.util.Date()
                        )

                    "${c.temp.toInt()}° | " +
                            "$desc | " +
                            "${dayLabel(iso, true)} • " +
                            dayLabel(iso, false)

                } catch (e: Exception) {
                    "${c.temp.toInt()}° | $desc"
                }

                val city = cityDisplayName(
                    p[2],
                    isFa
                )

                val nm =
                    getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                try {
                    val notif = buildNotification(
                        city = city,
                        temp = c.temp.toInt(),
                        desc = desc,
                        todayMax = todayMax,
                        todayMin = todayMin,
                        feels = feels,
                        pop = pop,
                        humidity = humidity,
                        windSpeed = c.wind.toInt(),
                        windUnit = windUnitLabel,
                        uvIndex = uvIndex,
                        emoji = emoji,
                        isFa = isFa
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            WEATHER_NOTIFICATION_ID,
                            notif,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(
                            WEATHER_NOTIFICATION_ID,
                            notif
                        )
                    }
                } catch (e: Throwable) {
                    Log.e(
                        TAG,
                        "Failed to post weather notification",
                        e
                    )
                    saveError(
                        "Notification: ${e.javaClass.simpleName}: ${e.message}"
                    )
                }

                val mode =
                    pd[Prefs.KEY_MODE] ?: "auto"

                val sysDark =
                    (
                            resources.configuration.uiMode
                                    and android.content.res.Configuration
                                .UI_MODE_NIGHT_MASK
                            ) ==
                            android.content.res.Configuration
                                .UI_MODE_NIGHT_YES

                val darkNow =
                    mode == "dark" ||
                            mode == "amoled" ||
                            (
                                    mode == "auto" &&
                                            sysDark
                                    )

                val maxStr = "▲ ${todayMax}°"
                val minStr = "▼ ${todayMin}°"
                val feelsStr = if (isFa) "حس واقعی: ${feels}°" else "Feels: ${feels}°"
                val chip1Str = if (pop > 0) "🌧️ $pop%" else (if (isFa) "🌡️ حس ${feels}°" else "🌡️ ${feels}°")
                val chip2Str = "💧 $humidity%"
                val chip3Str = "💨 ${c.wind.toInt()} $windUnitLabel"

                val popHumidityStr = if (pop > 0) {
                    if (isFa) "🌧️ بارش: ${pop}٪  •  💧 ${humidity}٪" else "🌧️ Rain: $pop%  •  💧 $humidity%"
                } else {
                    if (isFa) "💧 رطوبت: ${humidity}٪" else "💧 Humidity: $humidity%"
                }

                getSharedPreferences(
                    "widget",
                    Context.MODE_PRIVATE
                ).edit()
                    .putString("city", city)
                    .putInt("code", c.code)
                    .putInt("is_day", if (c.isDay == 1) 1 else 0)
                    .putString("temp", tempStr)
                    .putString("desc", desc)
                    .putString("max_temp", maxStr)
                    .putString("min_temp", minStr)
                    .putString("chip_1", chip1Str)
                    .putString("chip_2", chip2Str)
                    .putString("chip_3", chip3Str)
                    .putString("high_low", highLowStr)
                    .putString("feels", feelsStr)
                    .putString("pop_humidity", popHumidityStr)
                    .putString("wind", chip3Str)
                    .putString("details", detailsStr)
                    .putString("line", text)
                    .putString("emoji", emoji)
                    .putString("dark", if (darkNow) "1" else "0")
                    .putLong("updated_at", System.currentTimeMillis())
                    .putString("last_error", "")
                    .apply()

                WidgetRenderer.refresh(this@RefreshService)

                Log.d(
                    TAG,
                    "Weather refresh completed successfully"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Weather refresh failed",
                    e
                )

                saveError(
                    "${e.javaClass.simpleName}: ${e.message}"
                )

                WidgetRenderer.refresh(this@RefreshService)

            } finally {

                getSharedPreferences(
                    "widget",
                    Context.MODE_PRIVATE
                )
                    .edit()
                    .putBoolean(
                        "refresh_in_progress",
                        false
                    )
                    .apply()

                Log.d(
                    TAG,
                    "Refresh service finished"
                )
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {

        scope.cancel()

        Log.d(
            TAG,
            "RefreshService destroyed"
        )

        super.onDestroy()
    }

    private fun saveError(
        message: String
    ) {
        getSharedPreferences(
            "widget",
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                "last_error",
                message
            )
            .apply()
    }

    private fun createChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val nm =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(
                    "weather",
                    "آبوهوا",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    private fun initialNotification(): android.app.Notification {
        val sp = getSharedPreferences("widget", Context.MODE_PRIVATE)
        val city = sp.getString("city", "") ?: ""
        val temp = sp.getString("temp", "")?.replace("°", "")?.toIntOrNull()
        val desc = sp.getString("desc", "") ?: ""
        val emoji = sp.getString("emoji", "☁️") ?: "☁️"
        val maxTemp = sp.getString("max_temp", "")?.replace("▲", "")?.replace("°", "")?.trim()?.toIntOrNull() ?: temp ?: 20
        val minTemp = sp.getString("min_temp", "")?.replace("▼", "")?.replace("°", "")?.trim()?.toIntOrNull() ?: temp ?: 20

        if (city.isNotBlank() && temp != null && desc.isNotBlank()) {
            return buildNotification(
                city = city,
                temp = temp,
                desc = desc,
                todayMax = maxTemp,
                todayMin = minTemp,
                feels = temp,
                pop = 0,
                humidity = 0,
                windSpeed = 0,
                windUnit = "km/h",
                uvIndex = 0.0,
                emoji = emoji,
                isFa = true
            )
        }
        return placeholder()
    }

    private fun repostPendingIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            2,
            Intent(this, RefreshService::class.java).apply {
                action = ACTION_REPOST_NOTIFICATION
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun placeholder() =
        NotificationCompat.Builder(
            this,
            "weather"
        )
            .setSmallIcon(
                R.drawable.ic_stat_aseman
            )
            .setContentTitle(
                "آسمان"
            )
            .setContentText(
                "در حال بروزرسانی…"
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setDeleteIntent(repostPendingIntent())
            .build()
            .also {
                it.flags = it.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
            }

    private fun buildNotification(
        city: String,
        temp: Int,
        desc: String,
        todayMax: Int,
        todayMin: Int,
        feels: Int,
        pop: Int,
        humidity: Int,
        windSpeed: Int,
        windUnit: String,
        uvIndex: Double,
        emoji: String,
        isFa: Boolean
    ): android.app.Notification {
        val clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val fullDate = fullJalaliDateLabel(iso, isFa)

        val title = if (isFa) "$city • ${temp}° $desc".faDigits(true) else "$city • $temp° $desc"
        val summary = "📅 $fullDate"

        val bigText = if (isFa) {
            ("🌡️ بیشینه: ${todayMax}° | کمینه: ${todayMin}° • حس واقعی: ${feels}°\n" +
            "🌧️ احتمال بارش: ${pop}٪ • 💧 رطوبت: ${humidity}٪\n" +
            "💨 سرعت باد: $windSpeed $windUnit • ☀️ شاخص فرابنفش: ${uvIndex.toInt()}\n" +
            "📅 $fullDate (بروزرسانی: $clock)").faDigits(true)
        } else {
            "🌡️ High: $todayMax° | Low: $todayMin° • Feels: $feels°\n" +
            "🌧️ Rain: $pop% • 💧 Humidity: $humidity%\n" +
            "💨 Wind: $windSpeed $windUnit • ☀️ UV Index: ${uvIndex.toInt()}\n" +
            "📅 $fullDate (Updated: $clock)"
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val refreshIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, WeatherWidgetProvider::class.java).apply {
                action = WeatherWidgetProvider.ACTION_REFRESH
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val refreshAction = NotificationCompat.Action.Builder(
            R.drawable.ic_widget_refresh,
            if (isFa) "بروزرسانی" else "Refresh",
            refreshIntent
        ).build()

        val openAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            if (isFa) "مشاهده برنامه" else "Open App",
            openIntent
        ).build()

        val largeIconBitmap = try {
            android.graphics.BitmapFactory.decodeResource(resources, R.drawable.aseman_icon)
        } catch (_: Exception) {
            null
        }

        val notif = NotificationCompat.Builder(this, "weather")
            .setSmallIcon(R.drawable.ic_stat_aseman)
            .apply {
                if (largeIconBitmap != null) {
                    setLargeIcon(largeIconBitmap)
                }
            }
            .setContentTitle(title)
            .setContentText(summary)
            .setSubText(if (isFa) "آسمان" else "Aseman")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setColor(0xFF0284C7.toInt())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(openIntent)
            .setDeleteIntent(repostPendingIntent())
            .addAction(refreshAction)
            .addAction(openAction)
            .build()

        notif.flags = notif.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
        return notif
    }
}
