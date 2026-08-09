package com.iliyateam.aseman

import android.content.Context

data class LocalCity(val fa: String, val en: String, val lat: Double, val lon: Double)

object CityDb {

    private var list: List<LocalCity> = emptyList()

    fun init(ctx: Context) {
        if (list.isNotEmpty()) return
        list = try {
            ctx.assets.open("cities.csv").bufferedReader().readLines().mapNotNull { line ->
                val p = line.split(",")
                if (p.size >= 4) {
                    val la = p[2].toDoubleOrNull() ?: return@mapNotNull null
                    val lo = p[3].toDoubleOrNull() ?: return@mapNotNull null
                    LocalCity(p[0], p[1], la, lo)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun search(q: String): List<LocalCity> {
        val t = q.trim()
        if (t.isEmpty()) return emptyList()
        return list.filter { it.fa.contains(t) || it.en.contains(t, true) }.take(10)
    }
}