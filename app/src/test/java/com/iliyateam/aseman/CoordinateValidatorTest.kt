package com.iliyateam.aseman

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateValidatorTest {

    @Test
    fun validCoordinates_areAccepted() {
        assertTrue(
            CoordinateValidator.isValid(
                35.6892,
                51.3890
            )
        )
    }

    @Test
    fun minimumLatitude_isAccepted() {
        assertTrue(
            CoordinateValidator.isValid(
                -90.0,
                0.0
            )
        )
    }

    @Test
    fun maximumLatitude_isAccepted() {
        assertTrue(
            CoordinateValidator.isValid(
                90.0,
                0.0
            )
        )
    }

    @Test
    fun minimumLongitude_isAccepted() {
        assertTrue(
            CoordinateValidator.isValid(
                0.0,
                -180.0
            )
        )
    }

    @Test
    fun maximumLongitude_isAccepted() {
        assertTrue(
            CoordinateValidator.isValid(
                0.0,
                180.0
            )
        )
    }

    @Test
    fun latitudeAboveRange_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                90.0001,
                51.0
            )
        )
    }

    @Test
    fun latitudeBelowRange_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                -90.0001,
                51.0
            )
        )
    }

    @Test
    fun longitudeAboveRange_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                35.0,
                180.0001
            )
        )
    }

    @Test
    fun longitudeBelowRange_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                35.0,
                -180.0001
            )
        )
    }

    @Test
    fun nanLatitude_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                Double.NaN,
                51.0
            )
        )
    }

    @Test
    fun nanLongitude_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                35.0,
                Double.NaN
            )
        )
    }

    @Test
    fun infiniteLatitude_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                Double.POSITIVE_INFINITY,
                51.0
            )
        )
    }

    @Test
    fun infiniteLongitude_isRejected() {
        assertFalse(
            CoordinateValidator.isValid(
                35.0,
                Double.NEGATIVE_INFINITY
            )
        )
    }
}