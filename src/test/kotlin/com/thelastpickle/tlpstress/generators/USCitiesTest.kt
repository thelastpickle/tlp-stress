package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class USCitiesTest {

    @Test
    fun getText() {
        val cities = USCities()
        for(i in 0..100000)
            cities.getText()
    }
}