package com.iliyateam.aseman.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class WeatherResponse(
    @SerializedName("current") val current: Current,
    @SerializedName("hourly") val hourly: Hourly,
    @SerializedName("daily") val daily: Daily
)


    data class Current(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temp: Float,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val feels: Float,
    @SerializedName("weather_code") val code: Int,
    @SerializedName("cloud_cover") val clouds: Int,
    @SerializedName("pressure_msl") val pressure: Float,
    @SerializedName("wind_speed_10m") val wind: Float,
    @SerializedName("is_day") val isDay: Int
)

data class Hourly(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temp: List<Double>,
    @SerializedName("weather_code") val code: List<Int>,
    @SerializedName("is_day") val isDay: List<Int>
)

data class Daily(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val code: List<Int>,
    @SerializedName("temperature_2m_max") val max: List<Float>,
    @SerializedName("temperature_2m_min") val min: List<Float>,
    @SerializedName("sunrise") val sunrise: List<String>,
    @SerializedName("sunset") val sunset: List<String>
)

data class GeoResult(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class GeoResponse(@SerializedName("results") val results: List<GeoResult>?)

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,cloud_cover,pressure_msl,wind_speed_10m,is_day",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh"
    ): WeatherResponse

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "fa"
    ): GeoResponse

    companion object {
        val instance: WeatherApi by lazy {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApi::class.java)
        }
    }
}

/* ---------- کیفیت هوا ---------- */
data class AirCurrent(
    @SerializedName("us_aqi") val usAqi: Float?,
    @SerializedName("pm2_5") val pm25: Float?
)

data class AirResponse(@SerializedName("current") val current: AirCurrent?)

interface AirApi {

    @GET("v1/air-quality")
    suspend fun getAir(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "us_aqi,pm2_5"
    ): AirResponse

    companion object {
        val instance: AirApi by lazy {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl("https://air-quality-api.open-meteo.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AirApi::class.java)
        }
    }
}