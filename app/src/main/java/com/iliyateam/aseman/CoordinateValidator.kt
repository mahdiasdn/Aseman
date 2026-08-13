package com.iliyateam.aseman

object CoordinateValidator {

    fun isValid(
        lat: Double,
        lon: Double
    ): Boolean {
        return lat.isFinite() &&
                lon.isFinite() &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
    }
}