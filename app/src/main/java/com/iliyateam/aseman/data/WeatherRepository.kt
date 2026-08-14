package com.iliyateam.aseman.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.iliyateam.aseman.Prefs
import com.iliyateam.aseman.dataStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

data class WeatherResult(
    val weather: WeatherResponse,
    val air: AirCurrent?
)

data class CachedWeather(
    val city: String,
    val lat: Double,
    val lon: Double,
    val weather: WeatherResponse,
    val air: AirCurrent?
)

class WeatherRepository private constructor(private val context: Context) {

    private val appContext = context.applicationContext

    suspend fun fetchWeather(
        lat: Double,
        lon: Double,
        tempUnit: String = "celsius",
        windSpeedUnit: String = "kmh"
    ): WeatherResult = coroutineScope {
        val weatherDeferred = async {
            WeatherApi.instance.getWeather(
                lat = lat,
                lon = lon,
                temperatureUnit = tempUnit,
                windSpeedUnit = windSpeedUnit
            )
        }

        val airDeferred = async {
            try {
                AirApi.instance.getAir(lat, lon).current
            } catch (_: Exception) {
                null
            }
        }

        val weather = weatherDeferred.await()
        val air = airDeferred.await()

        WeatherResult(weather, air)
    }

    suspend fun getCachedWeather(city: String, lat: Double, lon: Double): CachedWeather? {
        return try {
            val cachedJson = appContext.dataStore.data.first()[Prefs.KEY_CACHE] ?: return null
            val bundle = Gson().fromJson(cachedJson, CachedWeather::class.java) ?: return null
            if (bundle.city == city &&
                kotlin.math.abs(bundle.lat - lat) < 0.01 &&
                kotlin.math.abs(bundle.lon - lon) < 0.01
            ) {
                bundle
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCache(
        city: String,
        lat: Double,
        lon: Double,
        weather: WeatherResponse,
        air: AirCurrent?
    ) {
        try {
            val bundle = CachedWeather(city, lat, lon, weather, air)
            val json = Gson().toJson(bundle)
            appContext.dataStore.edit { prefs ->
                prefs[Prefs.KEY_LAST] = "$lat|$lon|$city"
                prefs[Prefs.KEY_CACHE] = json
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        @Volatile
        private var instance: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return instance ?: synchronized(this) {
                instance ?: WeatherRepository(context).also { instance = it }
            }
        }
    }
}
