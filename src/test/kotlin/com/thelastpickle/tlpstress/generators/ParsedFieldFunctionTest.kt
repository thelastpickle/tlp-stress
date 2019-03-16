package com.thelastpickle.tlpstress.generators


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ParsedFieldFunctionTest {
    @Test
    fun noArgumentParseTest() {
        val parsed = ParsedFieldFunction("book()")
        assertThat(parsed.name).isEqualTo("book")
        assertThat(parsed.args).hasSize(0)
    }

    @Test
    fun singleArgumentTest() {
        val parsed = ParsedFieldFunction("random(10)")
        assertThat(parsed.name).isEqualTo("random")
        assertThat(parsed.args).hasSize(1)
        assertThat(parsed.args[0]).isEqualTo("10")
    }

    @Test
    fun emptyFieldArgumentParseTest() {
        val args = ParsedFieldFunction.parseArguments("")
        assertThat(args).hasSize(0)
    }
}