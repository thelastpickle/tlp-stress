package com.thelastpickle.tlpstress.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import kotlin.test.assertFailsWith

internal class HumanReadableTimeConverterTest {

    lateinit var converter: HumanReadableTimeConverter

    @BeforeEach
    fun setUp() {
        converter = HumanReadableTimeConverter()
    }

    @Test
    fun convert() {
        assertThat(converter.convert("15m")).isEqualTo(15)
        assertThat(converter.convert("1h")).isEqualTo(60)
        assertThat(converter.convert("3h")).isEqualTo(180)
        assertThat(converter.convert("1d 1h")).isEqualTo(1500)
        assertThat(converter.convert("1h 5m")).isEqualTo(65)
        assertThat(converter.convert("3m 120s")).isEqualTo(5)
        assertThat(converter.convert("10m 1d 59s 2h")).isEqualTo(1570)
        assertThat(converter.convert("1d2h3m")).isEqualTo(1563)
    }

    @Test
    fun convertAndFail() {
        assertFailsWith<java.lang.IllegalArgumentException> {val cl = converter.convert("BLAh")}
    }
}