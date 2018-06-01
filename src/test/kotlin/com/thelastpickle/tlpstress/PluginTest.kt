package com.thelastpickle.tlpstress

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PluginTest {
    @Test
    fun testGetPlugins() {
        val tmp = Plugin.getPlugins()
        assertThat(tmp.count()).isGreaterThan(1)
    }
}