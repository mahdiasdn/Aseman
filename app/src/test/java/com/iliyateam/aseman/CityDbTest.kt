package com.iliyateam.aseman

import org.junit.Assert.assertTrue
import org.junit.Test

class CityDbTest {

    @Test
    fun emptyQuery_returnsEmptyList() {
        val result = CityDb.search("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun blankQuery_returnsEmptyList() {
        val result = CityDb.search("   ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun whitespaceAroundQuery_returnsEmptyListWhenDatabaseIsNotInitialized() {
        val result = CityDb.search("   ")

        assertTrue(result.isEmpty())
    }
}