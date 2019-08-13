package com.thelastpickle.tlpstress

import com.thelastpickle.tlpstress.commands.Run
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MainArgumentsTest {
    @Test
    fun testPagingFlagWorks() {
        val run = Run("placeholder")
        val pageSize = 20000
        run.paging = pageSize
        assertThat(run.options.fetchSize).isEqualTo(pageSize)
    }
}