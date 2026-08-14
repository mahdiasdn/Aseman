package com.iliyateam.aseman

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.iliyateam.aseman.data.WeatherApi
import com.iliyateam.aseman.data.WeatherResponse

import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import kotlinx.coroutines.flow.first

import retrofit2.HttpException

class WeatherWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {


    companion object {
        const val KEY_TEMP_UNIT = "temp_unit"
        const val KEY_WIND_UNIT = "wind_unit"
        const val KEY_WIDGET_ONLY = "widget_only"

        private val WEATHER_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val sp = ctx.getSharedPreferences(
            "widget",
            Context.MODE_PRIVATE
        )

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
            val pd = ctx.applicationContext
                .dataStore
                .data
                .first()

            val widgetOnly = inputData.getBoolean(
                KEY_WIDGET_ONLY,
                false
            )

            val saved = pd[Prefs.KEY_NOTIF_CITY]
                ?.takeIf { it.isNotBlank() }
                ?: pd[Prefs.KEY_LAST]
                ?: return Result.success()

            val lang = pd[Prefs.KEY_LANG] ?: "fa"

            val p = saved.split("|")

            if (p.size < 3) {
                return Result.success()
            }

            val lat = p[0].toDoubleOrNull()
                ?: return Result.failure()

            val lon = p[1].toDoubleOrNull()
                ?: return Result.failure()

            if (
                !lat.isFinite() ||
                !lon.isFinite() ||
                lat !in -90.0..90.0 ||
                lon !in -180.0..180.0
            ) {
                return Result.failure()
            }

            if (p[2].isBlank()) {
                return Result.failure()
            }

            val tempPref =
                inputData.getString(KEY_TEMP_UNIT)
                    ?: pd[Prefs.KEY_UTEMP]
                    ?: "c"

            val windPref =
                inputData.getString(KEY_WIND_UNIT)
                    ?: pd[Prefs.KEY_UWIND]
                    ?: "kmh"

            val tempUnit =
                if (tempPref == "f") {
                    "fahrenheit"
                } else {
                    "celsius"
                }

            val windUnit = windPref

            val repo = com.iliyateam.aseman.data.WeatherRepository.getInstance(ctx)
            val result = repo.fetchWeather(
                lat = lat,
                lon = lon,
                tempUnit = tempUnit,
                windSpeedUnit = windUnit
            )
            val w = result.weather
            repo.saveCache(p[2], lat, lon, w, result.air)

            val c = w.current

            val emoji = emojiOf(
                c.code,
                c.isDay == 1
            )

            val text = try {
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

            } catch (_: Exception) {
                "${c.temp.toInt()}° | " +
                        descOf(
                            c.code,
                            lang == "fa"
                        )
            }

            val city = cityDisplayName(
                p[2],
                lang == "fa"
            )

            createChannel()

            if (
                !widgetOnly &&
                notificationsEnabled()
            ) {
                val nm =
                    ctx.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                nm.notify(
                    101,
                    buildNotification(
                        text,
                        city,
                        emoji
                    )
                )

                if (pd[Prefs.KEY_ALERTS] != "0") {
                    checkAlerts(
                        w,
                        lang
                    )
                }
            }

            val mode =
                pd[Prefs.KEY_MODE] ?: "auto"

            val sysDark =
                (
                        ctx.resources.configuration.uiMode
                                and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        ) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

            val darkNow =
                mode == "dark" ||
                        mode == "amoled" ||
                        (
                                mode == "auto" &&
                                        sysDark
                                )

            ctx.getSharedPreferences(
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

            WidgetRenderer.refresh(ctx)

            Result.success()

        } catch (e: Exception) {
            ctx.getSharedPreferences(
                "widget",
                Context.MODE_PRIVATE
            ).edit()
                .putString(
                    "last_error",
                    "${e.javaClass.simpleName}: ${e.message}"
                )
                .apply()

            WidgetRenderer.refresh(ctx)

            when {
                e is IOException ->
                    Result.retry()

                e is HttpException &&
                        (
                                e.code() == 408 ||
                                        e.code() == 429 ||
                                        e.code() >= 500
                                ) ->
                    Result.retry()

                else ->
                    Result.failure()
            }

        } finally {
            ctx.getSharedPreferences(
                "widget",
                Context.MODE_PRIVATE
            ).edit()
                .putBoolean(
                    "refresh_in_progress",
                    false
                )
                .apply()
        }
    }

    private fun notificationsEnabled(): Boolean {
        if (
            !NotificationManagerCompat
                .from(ctx)
                .areNotificationsEnabled()
        ) {
            return false
        }

        return if (Build.VERSION.SDK_INT >= 33) {
            androidx.core.app.ActivityCompat
                .checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createChannel() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            val nm =
                ctx.getSystemService(
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

            nm.createNotificationChannel(
                NotificationChannel(
                    "alerts",
                    "هشدارها",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private fun emojiIcon(
        emoji: String
    ): IconCompat {
        val size = 96

        val b = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val cv = Canvas(b)

        val p = Paint(
            Paint.ANTI_ALIAS_FLAG
        )

        p.textSize = 72f
        p.textAlign = Paint.Align.CENTER

        val r = Rect()

        p.getTextBounds(
            emoji,
            0,
            emoji.length,
            r
        )

        cv.drawText(
            emoji,
            size / 2f,
            size / 2f - r.exactCenterY(),
            p
        )

        return IconCompat.createWithBitmap(b)
    }

    private fun buildNotification(
        text: String,
        city: String,
        emoji: String
    ) =
        NotificationCompat.Builder(
            ctx,
            "weather"
        )
            .setSmallIcon(
                emojiIcon(emoji)
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
                    ctx,
                    0,
                    Intent(
                        ctx,
                        MainActivity::class.java
                    ),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private suspend fun checkAlerts(
        w: WeatherResponse,
        lang: String
    ) {
        try {
            val currentTime =
                parseWeatherTime(
                    w.current.time
                ) ?: return

            val idx =
                findNearestHourIndex(
                    w.hourly.time,
                    currentTime
                )

            if (idx < 0) {
                return
            }

            val next =
                w.hourly.code.subList(
                    idx,
                    minOf(
                        w.hourly.code.size,
                        idx + 3
                    )
                )

            val type =
                when {
                    next.any {
                        it in 95..99
                    } ->
                        "storm"

                    next.any {
                        it in 71..77 ||
                                it in 85..86
                    } ->
                        "snow"

                    next.any {
                        it in 51..67 ||
                                it in 80..82
                    } ->
                        "rain"

                    else ->
                        null
                } ?: return

            val savedLast =
                ctx.applicationContext
                    .dataStore
                    .data
                    .first()[Prefs.KEY_ALERT_LAST]

            val parts =
                savedLast?.split("|")

            val lastType =
                parts?.getOrNull(0)

            val lastTime =
                parts
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L

            val now =
                System.currentTimeMillis()

            if (
                lastType == type &&
                now - lastTime < 6 * 3600_000L
            ) {
                return
            }

            ctx.applicationContext
                .dataStore
                .edit {
                    it[Prefs.KEY_ALERT_LAST] =
                        "$type|$now"
                }

            val msg =
                when (type) {
                    "storm" ->
                        if (lang == "fa") {
                            "رعدوبرق در راه است ⛈️"
                        } else {
                            "Thunderstorm on the way ⛈️"
                        }

                    "snow" ->
                        if (lang == "fa") {
                            "بارش برف در راه است ❄️"
                        } else {
                            "Snow on the way ❄️"
                        }

                    else ->
                        if (lang == "fa") {
                            "تا ساعاتی دیگر باران می‌بارد ☔"
                        } else {
                            "Rain expected soon ☔"
                        }
                }

            if (!notificationsEnabled()) {
                return
            }

            val nm =
                ctx.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            nm.notify(
                202,
                alertNotification(msg)
            )

        } catch (_: Exception) {
        }
    }

    private fun parseWeatherTime(
        value: String?
    ): LocalDateTime? {
        if (value.isNullOrBlank()) {
            return null
        }

        val normalized =
            value
                .trim()
                .replace("Z", "")
                .let {
                    if (it.length >= 16) {
                        it.substring(0, 16)
                    } else {
                        it
                    }
                }

        return try {
            LocalDateTime.parse(
                normalized,
                WEATHER_TIME_FORMATTER
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findNearestHourIndex(
        times: List<String>,
        currentTime: LocalDateTime
    ): Int {
        var bestIndex = -1
        var bestDistance = Long.MAX_VALUE

        for (i in times.indices) {
            val hour =
                parseWeatherTime(
                    times[i]
                ) ?: continue

            val distance =
                kotlin.math.abs(
                    Duration.between(
                        currentTime,
                        hour
                    ).toMinutes()
                )

            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
            }
        }

        return if (
            bestIndex >= 0 &&
            bestDistance <= 90
        ) {
            bestIndex
        } else {
            -1
        }
    }

    private fun alertNotification(
        msg: String
    ) =
        NotificationCompat.Builder(
            ctx,
            "alerts"
        )
            .setSmallIcon(
                R.drawable.aseman_icon
            )
            .setContentTitle(
                "آسمان"
            )
            .setContentText(msg)
            .setAutoCancel(true)
            .build()


}


