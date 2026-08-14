package com.iliyateam.aseman

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.iliyateam.aseman.data.WeatherResponse

data class HourItem(val time: String, val temp: Double, val code: Int, val isDay: Boolean)

fun next24(d: WeatherResponse): List<HourItem> {
    val now = d.current.time.take(13) + ":00"
    val start = d.hourly.time.indexOfFirst { it >= now }.coerceAtLeast(0)
    return (start until minOf(start + 24, d.hourly.time.size)).map { i ->
        HourItem(d.hourly.time[i].takeLast(5), d.hourly.temp[i], d.hourly.code[i], d.hourly.isDay[i] == 1)
    }
}

fun String.faDigits(): String =
    if (java.util.Locale.getDefault().language == "fa")
        map { c -> if (c in '0'..'9') ('۰'.code + (c - '0')).toChar() else c }.joinToString("")
    else this

fun descFa(code: Int): String = descOf(code, true)

fun descOf(code: Int, fa: Boolean): String = when (code) {
    0 -> if (fa) "صاف" else "Clear"
    1 -> if (fa) "عمدتاً صاف" else "Mainly clear"
    2 -> if (fa) "نیمه‌ابری" else "Partly cloudy"
    3 -> if (fa) "ابری" else "Overcast"
    45, 48 -> if (fa) "مه" else "Fog"
    in 51..57 -> if (fa) "نم‌نم باران" else "Drizzle"
    in 61..67 -> if (fa) "باران" else "Rain"
    in 71..77, 85, 86 -> if (fa) "برف" else "Snow"
    in 80..82 -> if (fa) "رگبار" else "Showers"
    95, 96, 99 -> if (fa) "رعدوبرق" else "Thunderstorm"
    else -> if (fa) "ابری" else "Cloudy"
}

fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.Bedtime
    2 -> Icons.Outlined.CloudQueue
    3 -> Icons.Outlined.Cloud
    45, 48 -> Icons.Outlined.Dehaze
    in 51..67, in 80..82 -> Icons.Outlined.Grain
    in 71..77, 85, 86 -> Icons.Outlined.AcUnit
    95, 96, 99 -> Icons.Outlined.FlashOn
    else -> Icons.Outlined.Cloud
}

fun emojiOf(code: Int, isDay: Boolean = true): String = when (code) {
    0, 1 -> if (isDay) "☀️" else "🌙"
    2 -> if (isDay) "🌤️" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..67, in 80..82 -> "🌧️"
    in 71..77, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "☁️"
}

private val CITY_EN = mapOf(
    "تهران" to "Tehran", "مشهد" to "Mashhad", "اصفهان" to "Isfahan",
    "شیراز" to "Shiraz", "تبریز" to "Tabriz", "رشت" to "Rasht",
    "موقعیت من" to "My location"
)

fun cityDisplayName(name: String, fa: Boolean): String =
    if (fa) name else CITY_EN[name] ?: name

fun formatDateTime(ts: Long): String {
    val f = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
    return f.format(java.util.Date(ts)).faDigits()
}

/* ---------- تاریخ شمسی ---------- */
fun dayLabel(isoDate: String, fa: Boolean): String = try {
    val p = isoDate.split("-")
    if (fa) {
        val res = jalaliDate(p[0].toInt(), p[1].toInt(), p[2].toInt())
        val months = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        "${months[res.second - 1]} ${res.third}".faDigits()
    } else {
        val enMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "${enMonths[p[1].toInt() - 1]} ${p[2].toInt()}"
    }
} catch (e: Exception) {
    isoDate
}

fun jalaliDate(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = if (gy <= 1600) 0 else 979
    val gy2 = gy - (if (gy <= 1600) 621 else 1600)
    val gy3 = if (gm > 2) gy2 + 1 else gy2
    var days = 365 * gy2 + (gy3 + 3) / 4 - (gy3 + 99) / 100 + (gy3 + 399) / 400 - 80 + gd + gdm[gm - 1]
    jy += 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + (if (days < 186) days % 31 else (days - 186) % 30)
    return Triple(jy, jm, jd)
}

/* ---------- کیفیت هوا ---------- */
fun aqiInfo(aqi: Int, fa: Boolean): Pair<String, Long> = when {
    aqi <= 50 -> (if (fa) "پاک" else "Good") to 0xFF4CAF50
    aqi <= 100 -> (if (fa) "متوسط" else "Moderate") to 0xFFC0CA33
    aqi <= 150 -> (if (fa) "ناسالم برای گروه های حساس" else "Unhealthy for sensitive") to 0xFFFF9800
    aqi <= 200 -> (if (fa) "ناسالم" else "Unhealthy") to 0xFFF44336
    aqi <= 300 -> (if (fa) "بسیار ناسالم" else "Very unhealthy") to 0xFF9C27B0
    else -> (if (fa) "خطرناک" else "Hazardous") to 0xFF880E4F
}

/* ---------- طلوع/غروب ---------- */
fun timeMinutes(iso: String): Int = try {
    val t = iso.substring(11, 16)
    t.substring(0, 2).toInt() * 60 + t.substring(3, 5).toInt()
} catch (e: Exception) {
    0
}

fun clockOf(iso: String): String = try {
    iso.substring(11, 16).faDigits()
} catch (e: Exception) {
    iso
}

/* ---------- پس‌زمینهٔ زنده ---------- */
fun skyColor(code: Int, isDay: Boolean, dark: Boolean, amoled: Boolean): Long {
    if (amoled) return 0xFF000000
    return if (dark) when {
        !isDay -> 0xFF0B1026
        code in 51..67 || code in 80..82 || code >= 95 -> 0xFF1F2A33
        code in 71..77 || code in 85..86 -> 0xFF2A343C
        else -> 0xFF0E2A47
    } else when {
        !isDay -> 0xFF3949AB
        code in 51..67 || code in 80..82 || code >= 95 -> 0xFFB0BEC5
        code in 71..77 || code in 85..86 -> 0xFFE3F2FD
        else -> 0xFFBBDEFB
    }
}
/* ---------- بازکردن صفحهٔ اجرای خودکار و باتری متناسب با هر برند گوشی ---------- */
fun openAutoStart(ctx: android.content.Context) {
    val manufacturer = android.os.Build.MANUFACTURER.lowercase()
    val brandIntents = mutableListOf<android.content.Intent>()

    when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
            brandIntents.add(android.content.Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
            brandIntents.add(android.content.Intent("miui.intent.action.OP_AUTO_START").addCategory(android.content.Intent.CATEGORY_DEFAULT))
            brandIntents.add(android.content.Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
        }
        manufacturer.contains("samsung") -> {
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
        }
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
            brandIntents.add(android.content.Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
        }
        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
            brandIntents.add(android.content.Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
        }
        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
            brandIntents.add(android.content.Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"))
        }
    }

    // Fallbacks
    brandIntents.add(android.content.Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
    brandIntents.add(android.content.Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
    brandIntents.add(android.content.Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"))
    brandIntents.add(android.content.Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
    brandIntents.add(android.content.Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))

    for (intent in brandIntents) {
        try {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            return
        } catch (_: Exception) { }
    }
    openAppInfo(ctx)
}

fun openBatterySettings(ctx: android.content.Context) {
    val manufacturer = android.os.Build.MANUFACTURER.lowercase()
    val brandIntents = mutableListOf<android.content.Intent>()

    when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
            brandIntents.add(
                android.content.Intent().setClassName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                    .putExtra("package_name", ctx.packageName)
                    .putExtra("package_label", ctx.applicationInfo.loadLabel(ctx.packageManager))
            )
            brandIntents.add(android.content.Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
        }
        manufacturer.contains("samsung") -> {
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            brandIntents.add(android.content.Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"))
        }
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
            brandIntents.add(android.content.Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"))
        }
    }

    // Standard Android Battery Optimization Intent
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        brandIntents.add(
            android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(android.net.Uri.parse("package:" + ctx.packageName))
        )
        brandIntents.add(
            android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    }

    brandIntents.add(
        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:" + ctx.packageName))
    )

    for (intent in brandIntents) {
        try {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            return
        } catch (_: Exception) { }
    }
    openAppInfo(ctx)
}

private fun openAppInfo(ctx: android.content.Context) {
    try {
        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:" + ctx.packageName))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            .let { ctx.startActivity(it) }
    } catch (_: Exception) { }
}

fun weatherEmoji(code: Int, isDay: Boolean): String = when {
    code == 0 -> if (isDay) "☀️" else "🌙"
    code == 1 -> if (isDay) "🌤️" else "🌙"
    code == 2 -> "⛅"
    code == 3 -> "☁️"
    code in 45..48 -> "🌫️"
    code in 51..57 || code in 61..67 || code in 80..82 -> "🌧️"
    code in 71..77 || code in 85..86 -> "❄️"
    code in 95..99 -> "⛈️"
    else -> "🌦️"
}

fun openMyket(ctx: android.content.Context) {
    val pkg = ctx.packageName
    try {
        ctx.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("myket://details?id=$pkg"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        try {
            ctx.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://myket.ir/app/$pkg"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) { }
    }
}

fun openUrl(ctx: android.content.Context, url: String) {
    try {
        ctx.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) { }
}