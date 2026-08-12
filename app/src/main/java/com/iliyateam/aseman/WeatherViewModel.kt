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

    fun load(lat: Double, lon: Double, city: String, silent: Boolean = false) {
        last = Triple(lat, lon, city)

        if (silent && loadJob?.isActive == true) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _state.value = State.Loading
            try {
                val weatherDeferred = async {
                    WeatherApi.instance.getWeather(
                        lat,
                        lon,
                        temperatureUnit = if (prefs?.uTemp == "f") "fahrenheit" else "celsius",
                        windSpeedUnit = when (prefs?.uWind) {
                            "ms" -> "ms"
                            "mph" -> "mph"
                            else -> "kmh"
                        }
                    )
                }

                val airDeferred = async {
                    try {
                        AirApi.instance.getAir(lat, lon).current
                    } catch (_: Exception) {
                        null
                    }
                }

                val w = weatherDeferred.await()
                val air = airDeferred.await()

                _state.value = State.Success(
                    w,
                    city,
                    lat,
                    lon,
                    System.currentTimeMillis(),
                    air
                )
                appCtx?.dataStore?.edit {
                    it[Prefs.KEY_LAST] = "$lat|$lon|$city"
                    it[Prefs.KEY_CACHE] = Gson().toJson(CacheBundle(city, lat, lon, w, air))
                }
            } catch (e: Exception) {
                if (silent && _state.value is State.Success) return@launch
                val cached = appCtx?.dataStore?.data?.first()?.get(Prefs.KEY_CACHE)
                val b = cached?.let {
                    try { Gson().fromJson(it, CacheBundle::class.java) } catch (_: Exception) { null }
                }
                if (
                    b != null &&
                    b.city == city &&
                    kotlin.math.abs(b.lat - lat) < 0.01 &&
                    kotlin.math.abs(b.lon - lon) < 0.01
                ) {
                    _state.value = State.Success(
                        b.weather,
                        b.city,
                        b.lat,
                        b.lon,
                        System.currentTimeMillis(),
                        b.air
                    )
                } else {
                    _state.value = State.Error(prefs?.t("net_err") ?: "خطا در دریافت اطلاعات")
                }
            }
        }
    }

    private data class CacheBundle(
        val city: String,
        val lat: Double,
        val lon: Double,
        val weather: WeatherResponse,
        val air: AirCurrent?
    )

    fun retry() { last?.let { load(it.first, it.second, it.third) } }

    fun autoRefresh() { last?.let { load(it.first, it.second, it.third, silent = true) } }

    @SuppressLint("MissingPermission")
    fun useGps(ctx: Context, onFail: (String) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        viewModelScope.launch {
            try {
                var l = fused.lastLocation.await()

                if (l == null) {
                    l = withTimeoutOrNull(10_000L) {
                        fused.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            null
                        ).await()
                    }
                }
                if (l != null) load(l.latitude, l.longitude, prefs?.t("my_location") ?: "موقعیت من")
                else onFail(prefs?.t("gps_err") ?: "دسترسی به موقعیت ممکن نیست")
            } catch (e: Exception) {
                onFail(prefs?.t("gps_err") ?: "دسترسی به موقعیت ممکن نیست")
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