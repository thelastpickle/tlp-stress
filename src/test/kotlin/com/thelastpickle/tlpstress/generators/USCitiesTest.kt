package com.thelastpickle.tlpstress.generators

import com.thelastpickle.tlpstress.generators.functions.USCities
import org.junit.jupiter.api.Test

internal class USCitiesTest {

    @Test
    fun getText() {
        val cities = USCities()
        for(i in 0..100000)
            cities.getText()
    }
}