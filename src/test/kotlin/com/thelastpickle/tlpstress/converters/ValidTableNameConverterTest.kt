package com.thelastpickle.tlpstress.converters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

import kotlin.test.assertFailsWith

internal class ValidTableNameConverterTest {

    lateinit var converter: ValidTableNameConverter

    @BeforeEach
    fun setUp() {
        converter = ValidTableNameConverter()
    }

    @Test
    fun convert() {
        assertThat(converter.convert("-test1")).isEqualTo("_test1")
        assertThat(converter.convert("# -test ")).isEqualTo("___test_")
        assertThat(converter.convert("-test-1-a")).isEqualTo("_test_1_a")
    }
}