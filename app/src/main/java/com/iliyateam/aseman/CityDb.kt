package com.iliyateam.aseman

import android.content.Context

data class LocalCity(
    val fa: String,
    val en: String,
    val lat: Double,
    val lon: Double
)

object CityDb {

    private var list: List<LocalCity> = emptyList()

    @Synchronized
    fun init(ctx: Context) {
        if (list.isNotEmpty()) return

        list = try {
            ctx.applicationContext
                .assets
                .open("cities.csv")
                .bufferedReader()
                .useLines { lines ->
                    lines.mapNotNull { line ->

                        val p = line.split(",")

                        if (p.size < 4) {
                            return@mapNotNull null
                        }

                        val lat =
                            p[2].toDoubleOrNull()
                                ?: return@mapNotNull null

                        val lon =
                            p[3].toDoubleOrNull()
                                ?: return@mapNotNull null

                        LocalCity(
                            fa = p[0],
                            en = p[1],
                            lat = lat,
                            lon = lon
                        )
                    }.toList()
                }

        } catch (_: Exception) {
            emptyList()
        }
    }

    // برای جاهایی که CityDb قبلاً initialize شده است
    fun search(q: String): List<LocalCity> {
        return searchInternal(q)
    }

    // برای Settings: اگر هنوز initialize نشده باشد، خودش initialize می‌کند
    fun search(
        ctx: Context,
        q: String
    ): List<LocalCity> {
        if (list.isEmpty()) {
            init(ctx)
        }

        return searchInternal(q)
    }

    fun defaultCities(ctx: Context? = null): List<LocalCity> {
        if (list.isEmpty() && ctx != null) {
            init(ctx)
        }
        return list.take(8)
    }

    fun getDisplayName(name: String, fa: Boolean): String {
        val t = name.trim()
        if (t.isEmpty()) return name

        if (t == "موقعیت من" || t.equals("My location", ignoreCase = true) || t.equals("My Location", ignoreCase = true)) {
            return if (fa) "موقعیت من" else "My Location"
        }

        val city = list.find {
            it.fa.equals(t, ignoreCase = true) ||
            it.en.equals(t, ignoreCase = true) ||
            normalizeText(it.fa) == normalizeText(t)
        }

        if (city != null) {
            return if (fa) city.fa else city.en
        }

        val fallback = CITY_EN[t]
        if (!fa && fallback != null) return fallback

        val reverseFallback = CITY_EN.entries.find { it.value.equals(t, ignoreCase = true) }?.key
        if (fa && reverseFallback != null) return reverseFallback

        return t
    }

    private fun normalizeText(s: String): String {
        return s.replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
            .replace('\u200C', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    private fun searchInternal(q: String): List<LocalCity> {
        val t = q.trim()
        if (t.isEmpty()) {
            return emptyList()
        }

        val normQ = normalizeText(t)

        return list
            .filter {
                normalizeText(it.fa).contains(normQ) ||
                        it.en.contains(
                            t,
                            ignoreCase = true
                        )
            }
            .take(12)
    }
}