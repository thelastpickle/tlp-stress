package com.thelastpickle.tlpstress

import org.junit.jupiter.api.Assertions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandLineParserTest {
    @Test
    fun testBasicParser() {
        val args = arrayOf("run BasicTimeSeries")
        val result = CommandLineParser.parse(args)
        assertThat(result.parsedCommand).isEqualToIgnoringCase("run")
    }
}