package com.iliyateam.aseman.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class WeatherResponse(
    @SerializedName("current")
    val current: Current,


    @SerializedName("hourly")
val hourly: Hourly,

@SerializedName("daily")
val daily: Daily


)

data class Current(
    @SerializedName("time")
    val time: String,


    @SerializedName("temperature_2m")
val temp: Float,

@SerializedName("relative_humidity_2m")
val humidity: Int,

@SerializedName("apparent_temperature")
val feels: Float,

@SerializedName("weather_code")
val code: Int,

@SerializedName("cloud_cover")
val clouds: Int,

@SerializedName("pressure_msl")
val pressure: Float,

@SerializedName("wind_speed_10m")
val wind: Float,

@SerializedName("wind_direction_10m")
val windDirection: Int,

@SerializedName("wind_gusts_10m")
val windGusts: Float,

@SerializedName("precipitation")
val precipitation: Float,

@SerializedName("rain")
val rain: Float,

@SerializedName("showers")
val showers: Float,

@SerializedName("snowfall")
val snowfall: Float,

@SerializedName("visibility")
val visibility: Float,

@SerializedName("uv_index")
val uvIndex: Float,

@SerializedName("is_day")
val isDay: Int


)

data class Hourly(
    @SerializedName("time")
    val time: List<String>,


    @SerializedName("temperature_2m")
val temp: List<Double>,

@SerializedName("relative_humidity_2m")
val humidity: List<Int>,

@SerializedName("apparent_temperature")
val feels: List<Double>,

@SerializedName("weather_code")
val code: List<Int>,

@SerializedName("precipitation_probability")
val precipitationProbability: List<Int>,

@SerializedName("precipitation")
val precipitation: List<Double>,

@SerializedName("rain")
val rain: List<Double>,

@SerializedName("showers")
val showers: List<Double>,

@SerializedName("snowfall")
val snowfall: List<Double>,

@SerializedName("cloud_cover")
val cloudCover: List<Int>,

@SerializedName("wind_speed_10m")
val wind: List<Double>,

@SerializedName("wind_direction_10m")
val windDirection: List<Int>,

@SerializedName("wind_gusts_10m")
val windGusts: List<Double>,

@SerializedName("visibility")
val visibility: List<Double>,

@SerializedName("uv_index")
val uvIndex: List<Double>,

@SerializedName("is_day")
val isDay: List<Int>


)

data class Daily(
    @SerializedName("time")
    val time: List<String>,


    @SerializedName("weather_code")
val code: List<Int>,

@SerializedName("temperature_2m_max")
val max: List<Float>,

@SerializedName("temperature_2m_min")
val min: List<Float>,

@SerializedName("apparent_temperature_max")
val feelsMax: List<Float>,

@SerializedName("apparent_temperature_min")
val feelsMin: List<Float>,

@SerializedName("sunrise")
val sunrise: List<String>,

@SerializedName("sunset")
val sunset: List<String>,

@SerializedName("daylight_duration")
val daylightDuration: List<Double>,

@SerializedName("sunshine_duration")
val sunshineDuration: List<Double>,

@SerializedName("uv_index_max")
val uvIndexMax: List<Float>,

@SerializedName("precipitation_sum")
val precipitationSum: List<Float>,

@SerializedName("rain_sum")
val rainSum: List<Float>,

@SerializedName("showers_sum")
val showersSum: List<Float>,

@SerializedName("snowfall_sum")
val snowfallSum: List<Float>,

@SerializedName("precipitation_hours")
val precipitationHours: List<Float>,

@SerializedName("precipitation_probability_max")
val precipitationProbabilityMax: List<Int>,

@SerializedName("wind_speed_10m_max")
val windSpeedMax: List<Float>,

@SerializedName("wind_gusts_10m_max")
val windGustsMax: List<Float>,

@SerializedName("wind_direction_10m_dominant")
val dominantWindDirection: List<Int>


)

data class GeoResult(
    @SerializedName("name")
    val name: String,


    @SerializedName("latitude")
val latitude: Double,

@SerializedName("longitude")
val longitude: Double


)

data class GeoResponse(
    @SerializedName("results")
    val results: List<GeoResult>?
)

interface WeatherApi {


    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude")
        lat: Double,

        @Query("longitude")
        lon: Double,

        @Query("current")
        current: String =
            "temperature_2m," +
                    "relative_humidity_2m," +
                    "apparent_temperature," +
                    "weather_code," +
                    "cloud_cover," +
                    "pressure_msl," +
                    "wind_speed_10m," +
                    "wind_direction_10m," +
                    "wind_gusts_10m," +
                    "precipitation," +
                    "rain," +
                    "showers," +
                    "snowfall," +
                    "visibility," +
                    "uv_index," +
                    "is_day",

        @Query("hourly")
        hourly: String =
            "temperature_2m," +
                    "relative_humidity_2m," +
                    "apparent_temperature," +
                    "weather_code," +
                    "precipitation_probability," +
                    "precipitation," +
                    "rain," +
                    "showers," +
                    "snowfall," +
                    "cloud_cover," +
                    "wind_speed_10m," +
                    "wind_direction_10m," +
                    "wind_gusts_10m," +
                    "visibility," +
                    "uv_index," +
                    "is_day",

        @Query("daily")
        daily: String =
            "weather_code," +
                    "temperature_2m_max," +
                    "temperature_2m_min," +
                    "apparent_temperature_max," +
                    "apparent_temperature_min," +
                    "sunrise," +
                    "sunset," +
                    "daylight_duration," +
                    "sunshine_duration," +
                    "uv_index_max," +
                    "precipitation_sum," +
                    "rain_sum," +
                    "showers_sum," +
                    "snowfall_sum," +
                    "precipitation_hours," +
                    "precipitation_probability_max," +
                    "wind_speed_10m_max," +
                    "wind_gusts_10m_max," +
                    "wind_direction_10m_dominant",

        @Query("timezone")
        timezone: String = "auto",

        @Query("temperature_unit")
        temperatureUnit: String = "celsius",

        @Query("wind_speed_unit")
        windSpeedUnit: String = "kmh"
    ): WeatherResponse

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchCity(
        @Query("name")
        name: String,

        @Query("count")
        count: Int = 8,

        @Query("language")
        language: String = "fa"
    ): GeoResponse

    companion object {

        val instance: WeatherApi by lazy {

            val client =
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(
                        8,
                        TimeUnit.SECONDS
                    )
                    .readTimeout(
                        12,
                        TimeUnit.SECONDS
                    )
                    .writeTimeout(
                        12,
                        TimeUnit.SECONDS
                    )
                    .build()

            Retrofit.Builder()
                .baseUrl(
                    "https://api.open-meteo.com/"
                )
                .client(client)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(
                    WeatherApi::class.java
                )
        }
    }


}

/* ---------- کیفیت هوا ---------- */

data class AirCurrent(
    @SerializedName("us_aqi")
    val usAqi: Float?,


    @SerializedName("pm2_5")
val pm25: Float?


)

data class AirResponse(
    @SerializedName("current")
    val current: AirCurrent?
)

interface AirApi {


    @GET("v1/air-quality")
    suspend fun getAir(
        @Query("latitude")
        lat: Double,

        @Query("longitude")
        lon: Double,

        @Query("current")
        current: String =
            "us_aqi,pm2_5"
    ): AirResponse

    companion object {

        val instance: AirApi by lazy {

            val client =
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(
                        8,
                        TimeUnit.SECONDS
                    )
                    .readTimeout(
                        12,
                        TimeUnit.SECONDS
                    )
                    .writeTimeout(
                        12,
                        TimeUnit.SECONDS
                    )
                    .build()

            Retrofit.Builder()
                .baseUrl(
                    "https://air-quality-api.open-meteo.com/"
                )
                .client(client)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(
                    AirApi::class.java
                )
        }
    }


}
