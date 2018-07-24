package com.thelastpickle.tlpstress.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class HumanReadableConverterTest {

    lateinit var converter: HumanReadableConverter

    @BeforeEach
    fun setUp() {
        converter = HumanReadableConverter()
    }

    @Test
    fun convert() {
        assertThat(converter.convert("5k")).isEqualTo(5000L)
        assertThat(converter.convert("500")).isEqualTo(500L)
        assertThat(converter.convert("5m")).isEqualTo(5000000L)
        assertThat(converter.convert("5b")).isEqualTo(5000000000L)
    }
}