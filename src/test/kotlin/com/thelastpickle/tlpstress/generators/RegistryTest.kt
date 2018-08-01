package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.*
import org.assertj.core.api.Assertions.assertThat

internal class RegistryTest {

    lateinit var registry: Registry

    @BeforeEach
    fun setUp() {
        registry = Registry.create()
               .setDefault("test", "city", USCities())
               .setDefault("test", "age", Random(arrayListOf("1", "100")))
    }

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

    @Test
    fun getOverriddenTypeTest() {
        assertThat(registry.getGenerator("test", "city")).isInstanceOf(USCities::class.java)
        registry.setOverride("test", "city", Random(arrayListOf("1", "100")))
        assertThat(registry.getGenerator("test", "city")).isInstanceOf(Random::class.java)
    }
}