package com.thelastpickle.tlpstress

import org.junit.jupiter.api.Assertions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandLineParserTest {
    @Test
    fun testBasicParser() {
        val args = arrayOf("BasicTimeSeries")
        val result = CommandLineParser.parse(args)
        assertThat(result.parsedCommand).isEqualToIgnoringCase("BasicTimeSeries")

        val cls = result.getParsedPlugin()
        assertThat(cls).isNotNull
        assertThat(cls!!.name).isEqualToIgnoringCase("BasicTimeSeries")
        assertThat(cls!!.cls.name)

        val instance = result.getClassInstance()
        assertThat(instance!!.schema().first()).containsIgnoringCase("create")

    }
}