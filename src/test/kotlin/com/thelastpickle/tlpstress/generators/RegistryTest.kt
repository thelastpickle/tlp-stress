package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

internal class RegistryTest {

    @Test
    fun getNameTest() {
        assertThat(Registry.getName("cities()")).isEqualToIgnoringCase("cities")
        assertThat(Registry.getName("book(10, 50)")).isEqualToIgnoringCase("book")
    }

    @Test
    fun getArgsTest() {
        assertThat(Registry.getArguments("book(10, 50)")).first().isEqualTo("10")
        assertThat(Registry.getArguments("book(10, 50)")[1]).isEqualTo("50")
    }

    @Test
    fun getInstanceType() {
        assertThat(Registry.getInstance("book(10, 50)"))
        assertThat(Registry.getInstance("cities()"))
    }
}