package com.thelastpickle.tlpstress.converters

import com.datastax.driver.core.ConsistencyLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

import kotlin.test.assertFailsWith

internal class ConsistencyLevelConverterTest {

    lateinit var converter: ConsistencyLevelConverter

    @BeforeEach
    fun setUp() {
        converter = ConsistencyLevelConverter()
    }

    @Test
    fun convert() {
        assertThat(converter.convert("LOCAL_ONE")).isEqualTo(ConsistencyLevel.LOCAL_ONE)
        assertThat(converter.convert("LOCAL_QUORUM")).isEqualTo(ConsistencyLevel.LOCAL_QUORUM)
    }

    @Test
    fun convertAndFail() {
        assertFailsWith<java.lang.IllegalArgumentException> {val cl = converter.convert("LOCAL")}
    }
}