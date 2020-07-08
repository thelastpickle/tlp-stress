package com.thelastpickle.tlpstress

import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.thelastpickle.tlpstress.commands.Run
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MainArgumentsTest {
    @Test
    fun testPagingFlagWorks() {
        val run = Run("placeholder")
        val pageSize = 20000
        run.paging = pageSize
        assertThat(run.session.context.config.defaultProfile.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE)).isEqualTo(pageSize)
    }
}