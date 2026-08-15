package com.iliyateam.aseman

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.RemoteViews
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.iliyateam.aseman.ui.modern.weatherLottieAsset

object WidgetRenderer {

    fun renderLottie(context: Context, code: Int, isDay: Boolean): Bitmap? {
        return try {
            val assetPath = weatherLottieAsset(code, isDay)
            val composition = LottieCompositionFactory.fromAssetSync(context, assetPath).value ?: return null
            val drawable = LottieDrawable().apply {
                setComposition(composition)
                progress = 0.45f
            }
            val density = context.resources.displayMetrics.density
            val size = (90 * density).toInt().coerceAtLeast(180)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun refresh(context: Context) {
        try {
            val appContext = context.applicationContext
            val mgr = AppWidgetManager.getInstance(appContext)
            val ids = mgr.getAppWidgetIds(
                ComponentName(appContext, WeatherWidgetProvider::class.java)
            )

            if (ids.isEmpty()) return

            val sp = appContext.getSharedPreferences("widget", Context.MODE_PRIVATE)

            val city = sp.getString("city", "آسمان") ?: "آسمان"
            val line = sp.getString("line", "…") ?: "…"
            val emoji = sp.getString("emoji", "☁️") ?: "☁️"
            val weatherCode = sp.getInt("code", 0)
            val isDay = sp.getInt("is_day", 1) == 1
            val updatedAt = sp.getLong("updated_at", 0L)
            val dark = sp.getString("dark", "1") == "1"
            val solid = sp.getString("bg", "trans") == "solid"

            val temp = sp.getString("temp", "")?.takeIf { it.isNotBlank() }
                ?: if (line.contains("°")) line.substringBefore("°").trim() + "°" else "—°"

            val desc = sp.getString("desc", "")?.takeIf { it.isNotBlank() }
                ?: line.split("|").getOrNull(1)?.trim() ?: "…"

            val maxStr = sp.getString("max_temp", "")?.takeIf { it.isNotBlank() }
                ?: "▲ —°"

            val minStr = sp.getString("min_temp", "")?.takeIf { it.isNotBlank() }
                ?: "▼ —°"

            val chip1Str = sp.getString("chip_1", "")?.takeIf { it.isNotBlank() }
                ?: "🌧️ ۰٪"

            val chip2Str = sp.getString("chip_2", "")?.takeIf { it.isNotBlank() }
                ?: "💧 —٪"

            val chip3Str = sp.getString("chip_3", "")?.takeIf { it.isNotBlank() }
                ?: "💨 — km/h"

            val updatedText = if (updatedAt == 0L) {
                "—"
            } else {
                val clock = java.text.SimpleDateFormat(
                    "HH:mm",
                    java.util.Locale.US
                ).format(java.util.Date(updatedAt)).faDigits(true)
                "بروزرسانی: $clock"
            }

            val lottieBitmap = renderLottie(appContext, weatherCode, isDay)

            val refreshIntent = PendingIntent.getBroadcast(
                appContext,
                1,
                Intent(appContext, WeatherWidgetProvider::class.java).apply {
                    action = WeatherWidgetProvider.ACTION_REFRESH
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val openIntent = PendingIntent.getActivity(
                appContext,
                0,
                Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            for (id in ids) {
                val views = RemoteViews(appContext.packageName, R.layout.widget_weather).apply {
                    // Background
                    setInt(
                        R.id.w_root,
                        "setBackgroundResource",
                        when {
                            !solid -> R.drawable.widget_bg
                            dark -> R.drawable.widget_bg_solid
                            else -> R.drawable.widget_bg_solid_light
                        }
                    )

                    val chipBg = if (solid && !dark) R.drawable.widget_chip_bg_light else R.drawable.widget_chip_bg
                    setInt(R.id.w_chip_1_root, "setBackgroundResource", chipBg)
                    setInt(R.id.w_chip_2_root, "setBackgroundResource", chipBg)
                    setInt(R.id.w_chip_3_root, "setBackgroundResource", chipBg)

                    // Text & Elements Colors
                    if (solid && !dark) {
                        // Light Theme (Clean Slate with rich accents)
                        setTextColor(R.id.w_city, 0xFF0F172A.toInt())
                        setTextColor(R.id.w_updated, 0xFF64748B.toInt())
                        setTextColor(R.id.w_temp, 0xFF0F172A.toInt())
                        setTextColor(R.id.w_desc, 0xFF334155.toInt())
                        setTextColor(R.id.w_max, 0xFFEA580C.toInt()) // Warm Orange
                        setTextColor(R.id.w_min, 0xFF0284C7.toInt()) // Cool Sky Blue
                        setTextColor(R.id.w_chip_1_text, 0xFF0369A1.toInt()) // Sky
                        setTextColor(R.id.w_chip_2_text, 0xFF1D4ED8.toInt()) // Blue
                        setTextColor(R.id.w_chip_3_text, 0xFF047857.toInt()) // Emerald
                        setInt(R.id.w_refresh, "setColorFilter", 0xFF334155.toInt())
                    } else {
                        // Dark / Frosted Glass Theme (High Contrast Accents)
                        setTextColor(R.id.w_city, 0xFFFFFFFF.toInt())
                        setTextColor(R.id.w_updated, 0xFF94A3B8.toInt())
                        setTextColor(R.id.w_temp, 0xFFFFFFFF.toInt())
                        setTextColor(R.id.w_desc, 0xFFE2E8F0.toInt())
                        setTextColor(R.id.w_max, 0xFFFB923C.toInt()) // Warm Orange/Coral
                        setTextColor(R.id.w_min, 0xFF38BDF8.toInt()) // Cool Sky Blue
                        setTextColor(R.id.w_chip_1_text, 0xFF7DD3FC.toInt()) // Sky
                        setTextColor(R.id.w_chip_2_text, 0xFF93C5FD.toInt()) // Blue
                        setTextColor(R.id.w_chip_3_text, 0xFFA7F3D0.toInt()) // Mint
                        setInt(R.id.w_refresh, "setColorFilter", 0xFFE2E8F0.toInt())
                    }

                    // Content
                    setTextViewText(R.id.w_city, city.faDigits(true))
                    setTextViewText(R.id.w_temp, temp.faDigits(true))
                    setTextViewText(R.id.w_desc, desc)
                    setTextViewText(R.id.w_max, maxStr.faDigits(true))
                    setTextViewText(R.id.w_min, minStr.faDigits(true))
                    setTextViewText(R.id.w_chip_1_text, chip1Str.faDigits(true))
                    setTextViewText(R.id.w_chip_2_text, chip2Str.faDigits(true))
                    setTextViewText(R.id.w_chip_3_text, chip3Str.faDigits(true))
                    setTextViewText(R.id.w_updated, updatedText)

                    // Lottie Icon rendering vs Emoji fallback
                    if (lottieBitmap != null) {
                        setImageViewBitmap(R.id.w_icon, lottieBitmap)
                        setViewVisibility(R.id.w_icon, View.VISIBLE)
                        setViewVisibility(R.id.w_emoji, View.GONE)
                    } else {
                        setViewVisibility(R.id.w_icon, View.GONE)
                        setViewVisibility(R.id.w_emoji, View.VISIBLE)
                        setTextViewText(R.id.w_emoji, emoji)
                    }

                    // Click Actions
                    setOnClickPendingIntent(R.id.w_root, openIntent)
                    setOnClickPendingIntent(R.id.w_refresh, refreshIntent)
                }

                mgr.updateAppWidget(id, views)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetRenderer", "Failed to refresh widgets", e)
        }
    }
}