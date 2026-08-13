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
            startForeground(
                NOTIFICATION_ID,
                placeholder()
            )
        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start foreground service",
                e
            )

            saveError(
                "ForegroundStart: ${e.javaClass.simpleName}: ${e.message}"
            )

            stopSelfResult(startId)
            return START_NOT_STICKY
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

                val w =
                    WeatherApi.instance.getWeather(
                        lat,
                        lon,
                        timezone = "auto",
                        temperatureUnit = tempUnit,
                        windSpeedUnit = windUnit
                    )

                val c =
                    w.current

                val emoji =
                    emojiOf(
                        c.code,
                        c.isDay == 1
                    )

                val text =
                    try {

                        val iso =
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd",
                                java.util.Locale.US
                            ).format(
                                java.util.Date()
                            )

                        "${c.temp.toInt()}° | " +
                                "${descOf(c.code, lang == "fa")} | " +
                                "${dayLabel(iso, true)} • " +
                                dayLabel(iso, false)

                    } catch (e: Exception) {

                        Log.w(
                            TAG,
                            "Failed to build weather text",
                            e
                        )

                        "${c.temp.toInt()}° | " +
                                descOf(
                                    c.code,
                                    lang == "fa"
                                )
                    }

                val city =
                    cityDisplayName(
                        p[2],
                        lang == "fa"
                    )

                val nm =
                    getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                try {

                    nm.notify(
                        WEATHER_NOTIFICATION_ID,
                        buildNotification(
                            text,
                            city,
                            emoji
                        )
                    )

                } catch (e: Exception) {

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

                getSharedPreferences(
                    "widget",
                    Context.MODE_PRIVATE
                ).edit()
                    .putString(
                        "city",
                        city
                    )
                    .putString(
                        "line",
                        text
                    )
                    .putString(
                        "emoji",
                        weatherEmoji(
                            c.code,
                            c.isDay == 1
                        )
                    )
                    .putString(
                        "dark",
                        if (darkNow) "1" else "0"
                    )
                    .putLong(
                        "updated_at",
                        System.currentTimeMillis()
                    )
                    .putString(
                        "last_error",
                        ""
                    )
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

                stopSelfResult(startId)
            }
        }

        return START_NOT_STICKY
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
                    "آب‌وهوا",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    private fun placeholder() =
        NotificationCompat.Builder(
            this,
            "weather"
        )
            .setSmallIcon(
                R.drawable.aseman_icon
            )
            .setContentTitle(
                "آسمان"
            )
            .setContentText(
                "در حال بروزرسانی…"
            )
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun buildNotification(
        text: String,
        city: String,
        emoji: String
    ) =
        NotificationCompat.Builder(
            this,
            "weather"
        )
            .setSmallIcon(
                R.drawable.aseman_icon
            )
            .setContentTitle(
                city.ifBlank {
                    "آسمان"
                }
            )
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(
                        this,
                        MainActivity::class.java
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()


}


