package com.iliyateam.aseman

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

val Context.dataStore by preferencesDataStore(name = "settings")

class Prefs(context: Context) {

    private val context =
        context.applicationContext

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    var lang by mutableStateOf("fa"); private set
    var mode by mutableStateOf("auto"); private set
    var accent by mutableStateOf("auto"); private set
    var font by mutableStateOf("default"); private set
    var fontScale by mutableFloatStateOf(1f); private set
    var uTemp by mutableStateOf("c"); private set
    var uWind by mutableStateOf("kmh"); private set
    var refresh by mutableStateOf("30"); private set
    var alerts by mutableStateOf("1"); private set
    var notifCity by mutableStateOf(""); private set
    var widgetBg by mutableStateOf("trans"); private set

    suspend fun load() {
        val p = context.dataStore.data.first()

        lang = p[KEY_LANG] ?: "fa"
        mode = p[KEY_MODE] ?: "auto"
        accent = p[KEY_ACCENT] ?: "auto"
        font = p[KEY_FONT] ?: "default"
        fontScale = p[KEY_SCALE] ?: 1f
        uTemp = p[KEY_UTEMP] ?: "c"
        uWind = p[KEY_UWIND] ?: "kmh"
        refresh = p[KEY_REFRESH] ?: "30"
        alerts = p[KEY_ALERTS] ?: "1"
        notifCity = p[KEY_NOTIF_CITY] ?: ""
        widgetBg = p[KEY_WIDGET_BG] ?: "trans"

        applyLocale()
    }

    fun changeLang(v: String) {
        lang = v
        save(KEY_LANG, v)
        applyLocale()
    }

    fun changeMode(v: String) {
        mode = v
        save(KEY_MODE, v)
    }

    fun changeAccent(v: String) {
        accent = v
        save(KEY_ACCENT, v)
    }

    fun changeFont(v: String) {
        font = v
        save(KEY_FONT, v)
    }

    fun changeScale(v: Float) {
        fontScale = v
        saveF(KEY_SCALE, v)
    }

    fun changeUTemp(v: String) {
        uTemp = v
        save(KEY_UTEMP, v)
    }

    fun changeUWind(v: String) {
        uWind = v
        save(KEY_UWIND, v)
    }

    fun changeRefresh(v: String) {
        refresh = v
        save(KEY_REFRESH, v)
    }

    fun changeAlerts(v: String) {
        alerts = v
        save(KEY_ALERTS, v)
    }

    fun changeNotifCity(v: String) {
        notifCity = v
        save(KEY_NOTIF_CITY, v)
    }

    fun changeWidgetBg(v: String) {
        widgetBg = v
        save(KEY_WIDGET_BG, v)
    }

    fun tempSuffix(): String =
        if (uTemp == "f") "°F" else "°C"

    fun windLabel(): String =
        when (uWind) {
            "ms" -> "m/s"
            "mph" -> "mph"
            else -> "km/h"
        }

    private fun save(
        key: Preferences.Key<String>,
        v: String
    ) = scope.launch {
        context.dataStore.edit {
            it[key] = v
        }
    }

    private fun saveF(
        key: Preferences.Key<Float>,
        v: Float
    ) = scope.launch {
        context.dataStore.edit {
            it[key] = v
        }
    }

    fun applyLocale() {
        val loc =
            if (lang == "fa") {
                Locale("fa", "IR")
            } else {
                Locale.ENGLISH
            }

        Locale.setDefault(loc)

        val res = context.resources
        val conf = res.configuration

        conf.setLocale(loc)
        conf.setLayoutDirection(loc)

        res.updateConfiguration(
            conf,
            res.displayMetrics
        )
    }

    fun close() {
        scope.cancel()
    }

    companion object {
        val KEY_LANG =
            stringPreferencesKey("lang")

        val KEY_MODE =
            stringPreferencesKey("mode")

        val KEY_ACCENT =
            stringPreferencesKey("accent")

        val KEY_FONT =
            stringPreferencesKey("font")

        val KEY_SCALE =
            floatPreferencesKey("scale")

        val KEY_LAST =
            stringPreferencesKey("last_city")

        val KEY_FAVS =
            stringPreferencesKey("favs")

        val KEY_CACHE =
            stringPreferencesKey("cache")

        val KEY_HIDDEN =
            stringPreferencesKey("hidden")

        val KEY_UTEMP =
            stringPreferencesKey("utemp")

        val KEY_UWIND =
            stringPreferencesKey("uwind")

        val KEY_REFRESH =
            stringPreferencesKey("refresh")

        val KEY_ALERTS =
            stringPreferencesKey("alerts")

        val KEY_ALERT_LAST =
            stringPreferencesKey("alert_last")

        val KEY_NOTIF_CITY =
            stringPreferencesKey("notif_city")

        val KEY_WIDGET_BG =
            stringPreferencesKey("widget_bg")
    }
}

private val FA = mapOf(
    "tab_weather" to "آب‌وهوا",
    "tab_cities" to "شهرها",
    "tab_settings" to "تنظیمات",
    "search_ph" to "جستجوی شهرهای ایران و جهان…",
    "no_result" to "نتیجه‌ای یافت نشد",
    "now" to "اکنون",
    "today" to "امروز",
    "tomorrow" to "فردا",
    "feels" to "حس واقعی",
    "high" to "بیشینه",
    "low" to "کمینه",
    "next24" to "۲۴ ساعت آینده",
    "next7" to "۷ روز آینده",
    "wind" to "باد",
    "humidity" to "رطوبت",
    "pressure" to "فشار",
    "clouds" to "ابر",
    "language" to "زبان",
    "font" to "فونت",
    "font_size" to "اندازهٔ متن",
    "mode" to "حالت پوسته",
    "accent" to "رنگ برنامه",
    "auto" to "خودکار",
    "light" to "روشن",
    "dark" to "تیره",
    "amoled" to "مشکی خالص (AMOLED)",
    "default_font" to "پیش‌فرض",
    "vazir_font" to "وزیرمتن",
    "favorites" to "شهرهای مورد علاقه",
    "no_fav" to "هنوز شهری اضافه نکرده‌ای؛ از صفحهٔ آب‌وهوا روی ♥ بزن",
    "my_location" to "موقعیت من",
    "gps_err" to "دسترسی به موقعیت ممکن نیست",
    "net_err" to "خطا در دریافت اطلاعات؛ اینترنت را بررسی کنید",
    "retry" to "تلاش دوباره",
    "added" to "به علاقه‌مندی‌ها اضافه شد ❤️",
    "removed" to "از علاقه‌مندی‌ها حذف شد",
    "dir" to "جهت",
    "all_cities" to "شهرهای پیش‌فرض",
    "restore" to "بازیابی شهرهای پیش‌فرض",
    "offline" to "بدون اینترنت؛ آخرین دادهٔ ذخیره‌شده نمایش داده می‌شود",
    "hide" to "حذف",
    "units" to "واحدهای اندازه‌گیری",
    "temp_unit" to "واحد دما",
    "wind_unit" to "واحد باد",
    "about" to "درباره",
    "exit" to "خروج",
    "about_text" to "داده‌های هوا از وب‌سایت رایگان Open-Meteo دریافت می‌شوند.",
    "telegram" to "کانال تلگرام آسمان",
    "size_s" to "کوچک",
    "size_m" to "معمولی",
    "size_l" to "بزرگ",
    "size_xl" to "خیلی بزرگ",
    "updated" to "آخرین به‌روزرسانی",
    "refresh" to "به‌روزرسانی خودکار",
    "off" to "خاموش",
    "min15" to "هر ۱۵ دقیقه",
    "min30" to "هر ۳۰ دقیقه",
    "min60" to "هر ۱ ساعت",
    "aqi" to "کیفیت هوا",
    "sunrise" to "طلوع",
    "sunset" to "غروب",
    "pm25" to "ذرات معلق PM2.5",
    "on" to "روشن",
    "alerts" to "هشدارهای هوشمند",
    "vpn_hint" to "این شهر در فهرست آفلاین نیست؛ برای جستجوی جهانی فیلترشکن را روشن کنید",
    "autostart" to "اجرای خودکار (Autostart)",
    "autostart_desc" to "بازکردن مدیریت اجرای خودکار این اپ",
    "notif_city" to "شهر ویجت و اعلان",
    "follow_app" to "همراه با شهر اپ",
    "bg_keep" to "زنده‌ماندن در پس‌زمینه",
    "bg_keep_desc" to "بازکردن تنظیمات محدودیت باتری این اپ",
    "widget_bg" to "پس‌زمینهٔ ویجت",
    "trans" to "شفاف",
    "solid" to "یکدست",
    "about_app" to "درباره آسمان",
    "developer" to "توسعه‌دهنده",
    "rate_myket" to "امتیاز و نظر در مایکت",
    "version" to "نسخه",
    "editor_line" to "ویرایش و عرضه: [MightyMahdi]",
    "github" to "سورس‌کد در گیت‌هاب",
)

private val EN = mapOf(
    "tab_weather" to "Weather",
    "tab_cities" to "Cities",
    "tab_settings" to "Settings",
    "search_ph" to "Search cities worldwide…",
    "no_result" to "No results",
    "now" to "Now",
    "today" to "Today",
    "tomorrow" to "Tomorrow",
    "feels" to "Feels like",
    "high" to "High",
    "low" to "Low",
    "next24" to "Next 24 hours",
    "next7" to "7-day forecast",
    "wind" to "Wind",
    "humidity" to "Humidity",
    "pressure" to "Pressure",
    "clouds" to "Clouds",
    "language" to "Language",
    "font" to "Font",
    "font_size" to "Text size",
    "mode" to "Theme mode",
    "accent" to "App color",
    "auto" to "Auto",
    "light" to "Light",
    "dark" to "Dark",
    "amoled" to "AMOLED black",
    "default_font" to "Default",
    "vazir_font" to "Vazirmatn",
    "favorites" to "Favorite cities",
    "no_fav" to "Nothing here yet; tap ♥ on the weather page",
    "my_location" to "My location",
    "gps_err" to "Cannot access location",
    "net_err" to "Failed to fetch data; check connection",
    "retry" to "Retry",
    "added" to "Added to favorites ❤️",
    "removed" to "Removed from favorites",
    "dir" to "Direction",
    "all_cities" to "Default cities",
    "restore" to "Restore default cities",
    "offline" to "Offline; showing last saved data",
    "hide" to "Remove",
    "units" to "Units",
    "temp_unit" to "Temperature unit",
    "wind_unit" to "Wind unit",
    "about" to "About",
    "exit" to "Exit",
    "about_text" to "Weather data is provided by the free Open-Meteo website.",
    "telegram" to "Aseman Telegram channel",
    "size_s" to "Small",
    "size_m" to "Normal",
    "size_l" to "Large",
    "size_xl" to "Huge",
    "updated" to "Last update",
    "refresh" to "Auto refresh",
    "off" to "Off",
    "min15" to "Every 15 min",
    "min30" to "Every 30 min",
    "min60" to "Every hour",
    "aqi" to "Air quality",
    "sunrise" to "Sunrise",
    "sunset" to "Sunset",
    "pm25" to "PM2.5 particles",
    "on" to "On",
    "alerts" to "Smart alerts",
    "vpn_hint" to "City not in offline list — enable VPN for worldwide search",
    "autostart" to "Autostart",
    "autostart_desc" to "Open autostart manager for this app",
    "notif_city" to "Widget & notification city",
    "follow_app" to "Same as app city",
    "bg_keep" to "Background survival",
    "bg_keep_desc" to "Open this app's battery restrictions",
    "widget_bg" to "Widget background",
    "trans" to "Translucent",
    "solid" to "Solid",
    "about_app" to "About Aseman",
    "developer" to "Developer",
    "rate_myket" to "Rate on Myket",
    "version" to "Version",
    "editor_line" to "Edited & published by: [MightyMahdi]",
    "github" to "Source on GitHub",
)

fun Prefs.t(key: String): String =
    (if (lang == "fa") FA else EN)[key] ?: key