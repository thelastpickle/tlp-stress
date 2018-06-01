package com.thelastpickle.tlpstress

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PluginTest {
    // simple test, but ¯\_(ツ)_/¯
    // we should have at least 2 plugins
    @Test
    fun testGetPlugins() {
        val tmp = Plugin.getPlugins()
        assertThat(tmp.count()).isGreaterThan(1)
    }
}