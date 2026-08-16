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
        val sp = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
        val city = sp.getString("city", "آسمان") ?: "آسمان"
        val temp = sp.getString("temp", "")?.replace("°", "")?.toIntOrNull() ?: 20
        val desc = sp.getString("desc", "…") ?: "…"
        val emoji = sp.getString("emoji", "☁️") ?: "☁️"

        createChannel()
        val notif = buildNotification(
            city = city,
            temp = temp,
            desc = desc,
            todayMax = temp,
            todayMin = temp,
            feels = temp,
            pop = 0,
            humidity = 0,
            windSpeed = 0,
            windUnit = "km/h",
            uvIndex = 0.0,
            emoji = emoji,
            isFa = true
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.work.ForegroundInfo(
                100,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            androidx.work.ForegroundInfo(
                100,
                notif
            )
        }
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
            val todayMax = w.daily.max.firstOrNull()?.toInt() ?: c.temp.toInt()
            val todayMin = w.daily.min.firstOrNull()?.toInt() ?: c.temp.toInt()
            val feels = c.feels.toInt()
            val isFa = lang == "fa"
            val desc = descOf(c.code, isFa)
            val emoji = weatherEmoji(c.code, c.isDay == 1)
            val pop = w.hourly.precipitationProbability.firstOrNull() ?: 0
            val humidity = w.hourly.humidity.firstOrNull() ?: c.humidity
            val uvIndex = w.daily.uvIndexMax.firstOrNull()?.toDouble() ?: 0.0
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

            } catch (_: Exception) {
                "${c.temp.toInt()}° | $desc"
            }

            val city = cityDisplayName(
                p[2],
                isFa
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

            ctx.getSharedPreferences(
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
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val refreshIntent = PendingIntent.getBroadcast(
            ctx,
            1,
            Intent(ctx, WeatherWidgetProvider::class.java).apply {
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
            android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.aseman_icon)
        } catch (_: Exception) {
            null
        }

        val notif = NotificationCompat.Builder(ctx, "weather")
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
            .addAction(refreshAction)
            .addAction(openAction)
            .build()

        notif.flags = notif.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
        return notif
    }

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
    ): android.app.Notification {
        val largeIconBitmap = try {
            android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.aseman_icon)
        } catch (_: Exception) {
            null
        }
        return NotificationCompat.Builder(
            ctx,
            "alerts"
        )
            .setSmallIcon(
                R.drawable.ic_stat_aseman
            )
            .apply {
                if (largeIconBitmap != null) {
                    setLargeIcon(largeIconBitmap)
                }
            }
            .setContentTitle(
                "آسمان"
            )
            .setContentText(msg)
            .setAutoCancel(true)
            .build()
    }


}


