package com.thelastpickle.tlpstress

import com.thelastpickle.tlpstress.commands.Run
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandLineParserTest {
    @Test
    fun testBasicParser() {
        val args = arrayOf("run", "BasicTimeSeries")
        val result = CommandLineParser.parse(args)
        assertThat(result.getParsedCommand()).isEqualToIgnoringCase("run")
        assertThat(result.getCommandInstance()).isInstanceOf(Run::class.java)
    }
}