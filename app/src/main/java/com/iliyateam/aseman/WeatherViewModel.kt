package com.iliyateam.aseman

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import com.iliyateam.aseman.data.GeoResult
import com.iliyateam.aseman.data.AirApi
import com.iliyateam.aseman.data.AirCurrent
import com.iliyateam.aseman.data.WeatherApi
import com.iliyateam.aseman.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WeatherViewModel : ViewModel() {
    private fun isValidCoordinates(
        lat: Double,
        lon: Double
    ): Boolean {
        return lat.isFinite() &&
                lon.isFinite() &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
    }

    sealed class State {
        data object Loading : State()
        data class Success(
            val data: WeatherResponse,
            val city: String,
            val lat: Double,
            val lon: Double,
            val updated: Long,
            val air: AirCurrent? = null
        ) : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state

    var prefs: Prefs? = null
    private var appCtx: Context? = null
    private var last: Triple<Double, Double, String>? = null

    private var initialized = false

    fun init(ctx: Context) {
        appCtx = ctx
        if (initialized) return
        initialized = true
        CityDb.init(ctx)
        viewModelScope.launch {
            val saved = ctx.dataStore.data.first()[Prefs.KEY_LAST]
            val p = saved?.split("|")
            if (p != null && p.size == 3 && p[0].toDoubleOrNull() != null) {
                load(p[0].toDouble(), p[1].toDouble(), p[2])
            } else {
                load(35.6892, 51.3890, if (prefs?.lang == "en") "Tehran" else "تهران")
            }
        }
    }

    fun load(
        lat: Double,
        lon: Double,
        city: String,
        silent: Boolean = false
    ) {
        if (!isValidCoordinates(lat, lon)) {
            _state.value = State.Error(
                prefs?.t("net_err")
                    ?: "مختصات مکان نامعتبر است"
            )
            return
        }

        if (city.isBlank()) {
            _state.value = State.Error(
                prefs?.t("net_err")
                    ?: "نام شهر نامعتبر است"
            )
            return
        }

        last = Triple(lat, lon, city)

        if (silent && loadJob?.isActive == true) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _state.value = State.Loading
            val ctx = appCtx
            val repo = ctx?.let { com.iliyateam.aseman.data.WeatherRepository.getInstance(it) }
            try {
                val tempUnit = if (prefs?.uTemp == "f") "fahrenheit" else "celsius"
                val windSpeedUnit = when (prefs?.uWind) {
                    "ms" -> "ms"
                    "mph" -> "mph"
                    else -> "kmh"
                }

                val result = repo?.fetchWeather(lat, lon, tempUnit, windSpeedUnit)
                    ?: throw IllegalStateException("Repository not initialized")

                _state.value = State.Success(
                    result.weather,
                    city,
                    lat,
                    lon,
                    System.currentTimeMillis(),
                    result.air
                )
                repo.saveCache(city, lat, lon, result.weather, result.air)
                syncWidget(city, result.weather)
            } catch (e: Exception) {
                if (silent && _state.value is State.Success) return@launch
                val cached = repo?.getCachedWeather(city, lat, lon)
                if (cached != null) {
                    _state.value = State.Success(
                        cached.weather,
                        cached.city,
                        cached.lat,
                        cached.lon,
                        System.currentTimeMillis(),
                        cached.air
                    )
                    syncWidget(cached.city, cached.weather)
                } else {
                    _state.value = State.Error(prefs?.t("net_err") ?: "خطا در دریافت اطلاعات")
                }
            }
        }
    }

    private fun syncWidget(city: String, w: WeatherResponse) {
        val ctx = appCtx ?: return
        try {
            val isFa = prefs?.lang == "fa"
            val c = w.current
            val todayMax = w.daily.max.firstOrNull()?.toInt() ?: c.temp.toInt()
            val todayMin = w.daily.min.firstOrNull()?.toInt() ?: c.temp.toInt()
            val feels = c.feels.toInt()
            val desc = descOf(c.code, isFa)
            val emoji = weatherEmoji(c.code, c.isDay == 1)
            val windUnit = prefs?.windLabel() ?: "km/h"
            val pop = w.hourly.precipitationProbability.firstOrNull() ?: 0
            val humidity = w.hourly.humidity.firstOrNull() ?: c.humidity

            val maxStr = "▲ ${todayMax}°"
            val minStr = "▼ ${todayMin}°"
            val tempStr = "${c.temp.toInt()}°"
            val highLowStr = "▲ ${todayMax}°  ▼ ${todayMin}°"
            val feelsStr = if (isFa) "حس واقعی: ${feels}°" else "Feels: ${feels}°"
            val chip1Str = if (pop > 0) "🌧️ $pop%" else (if (isFa) "🌡️ حس ${feels}°" else "🌡️ ${feels}°")
            val chip2Str = "💧 $humidity%"
            val chip3Str = "💨 ${c.wind.toInt()} $windUnit"
            val popHumidityStr = if (pop > 0) {
                if (isFa) "🌧️ بارش: ${pop}٪  •  💧 ${humidity}٪" else "🌧️ Rain: $pop%  •  💧 $humidity%"
            } else {
                if (isFa) "💧 رطوبت: ${humidity}٪" else "💧 Humidity: $humidity%"
            }
            val detailsStr = if (pop > 0) "🌧️ $pop% • 💨 ${c.wind.toInt()} $windUnit" else "💧 $humidity% • 💨 ${c.wind.toInt()} $windUnit"

            val sp = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val line = "${c.temp.toInt()}° | $desc | ${dayLabel(iso, true)} • ${dayLabel(iso, false)}"

            sp.edit()
                .putString("city", cityDisplayName(city, isFa))
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
                .putString("line", line)
                .putString("emoji", emoji)
                .putLong("updated_at", System.currentTimeMillis())
                .putString("last_error", "")
                .apply()

            WidgetRenderer.refresh(ctx)
        } catch (_: Exception) {
        }
    }

    fun retry() { last?.let { load(it.first, it.second, it.third) } }

    fun autoRefresh() { last?.let { load(it.first, it.second, it.third, silent = true) } }

    @SuppressLint("MissingPermission")
    fun useGps(ctx: Context, onFail: (String) -> Unit) {
        val fused =
            LocationServices.getFusedLocationProviderClient(ctx)

        viewModelScope.launch {
            try {
                var l =
                    fused.lastLocation.await()

                if (l == null) {
                    l = withTimeoutOrNull(10_000L) {
                        fused.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            null
                        ).await()
                    }
                }

                if (l == null) {
                    onFail(
                        prefs?.t("gps_err")
                            ?: "دسترسی به موقعیت ممکن نیست"
                    )
                    return@launch
                }

                if (
                    !isValidCoordinates(
                        l.latitude,
                        l.longitude
                    )
                ) {
                    onFail(
                        prefs?.t("gps_err")
                            ?: "مختصات موقعیت نامعتبر است"
                    )
                    return@launch
                }

                load(
                    l.latitude,
                    l.longitude,
                    prefs?.t("my_location")
                        ?: "موقعیت من"
                )

            } catch (e: Exception) {
                onFail(
                    prefs?.t("gps_err")
                        ?: "دسترسی به موقعیت ممکن نیست"
                )
            }
        }
    }

    /* ---------- جستجو ---------- */
    private val _results = MutableStateFlow<List<GeoResult>>(emptyList())
    private val _vpnHint = MutableStateFlow(false)
    val vpnHint = _vpnHint
    val results: StateFlow<List<GeoResult>> = _results

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    fun search(q: String, lang: String) {
        if (q.trim().length < 2) { _results.value = emptyList(); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val local = CityDb.search(q).map {
                GeoResult(if (lang == "fa") it.fa else it.en, it.lat, it.lon)
            }
            _results.value = local
            _vpnHint.value = false
            try {
                val online = withTimeoutOrNull(4000) {
                    WeatherApi.instance.searchCity(q.trim(), 8, lang).results
                }
                if (online == null) {
                    if (local.isEmpty()) _vpnHint.value = true
                } else {
                    _results.value = (local + online).distinctBy { "${it.latitude},${it.longitude}" }
                }
            } catch (_: Exception) {
                if (local.isEmpty()) _vpnHint.value = true
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _results.value = emptyList()
        _vpnHint.value = false
    }

    /* ---------- علاقه‌مندی‌ها ---------- */
    private val _favs = MutableStateFlow<List<Triple<Double, Double, String>>>(emptyList())
    val favs: StateFlow<List<Triple<Double, Double, String>>> = _favs

    fun loadFavs(ctx: Context) {
        appCtx = ctx
        viewModelScope.launch {
            _favs.value = parseFavs(ctx.dataStore.data.first()[Prefs.KEY_FAVS])
        }
    }

    fun toggleFav(lat: Double, lon: Double, name: String, onMessage: (String) -> Unit) {
        val ctx = appCtx ?: return
        viewModelScope.launch {
            val cur = parseFavs(ctx.dataStore.data.first()[Prefs.KEY_FAVS]).toMutableList()
            val exists = cur.find { key(it.first, it.second) == key(lat, lon) }
            if (exists != null) {
                cur.remove(exists)
                onMessage(prefs?.t("removed") ?: "Removed")
            } else {
                cur.add(Triple(lat, lon, name))
                onMessage(prefs?.t("added") ?: "Added ❤️")
            }
            ctx.dataStore.edit {
                it[Prefs.KEY_FAVS] = cur.joinToString(";") { c -> "${c.first},${c.second},${c.third}" }
            }
            _favs.value = cur
        }
    }

    fun isFav(lat: Double, lon: Double): Boolean =
        _favs.value.any { key(it.first, it.second) == key(lat, lon) }

    /* ---------- شهرهای پیش‌فرض ---------- */
    private val _hidden = MutableStateFlow<List<String>>(emptyList())
    val hidden: StateFlow<List<String>> = _hidden

    fun loadHidden(ctx: Context) {
        viewModelScope.launch {
            _hidden.value = (ctx.dataStore.data.first()[Prefs.KEY_HIDDEN] ?: "")
                .split(",").filter { it.isNotBlank() }
        }
    }

    fun hideDefault(name: String) {
        val ctx = appCtx ?: return
        viewModelScope.launch {
            val cur = _hidden.value.toMutableList()
            if (!cur.contains(name)) cur.add(name)
            ctx.dataStore.edit { it[Prefs.KEY_HIDDEN] = cur.joinToString(",") }
            _hidden.value = cur
        }
    }

    fun restoreDefaults() {
        val ctx = appCtx ?: return
        viewModelScope.launch {
            ctx.dataStore.edit { it[Prefs.KEY_HIDDEN] = "" }
            _hidden.value = emptyList()
        }
    }

    private fun key(lat: Double, lon: Double) =
        String.format(Locale.US, "%.3f|%.3f", lat, lon)

    private fun parseFavs(s: String?): List<Triple<Double, Double, String>> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split(";").mapNotNull { part ->
            val p = part.split(",")
            if (p.size >= 3) {
                val la = p[0].toDoubleOrNull() ?: return@mapNotNull null
                val lo = p[1].toDoubleOrNull() ?: return@mapNotNull null
                Triple(la, lo, p.drop(2).joinToString(","))
            } else null
        }
    }

}