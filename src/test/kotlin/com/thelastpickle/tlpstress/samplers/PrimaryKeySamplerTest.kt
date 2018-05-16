package com.thelastpickle.tlpstress.samplers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PrimaryKeySamplerTest {
    @Test
    fun maybePutTest() {
        // record everything

        val validate = fun(primaryKey: Any, fields: Fields) : ValidationResult {
            return ValidationResult.Correct()
        }

        val pks = PrimaryKeySampler(1.0, validate)

        for(x in 1..100) {
            pks.maybePut(x, mapOf("test" to "test"))
        }

        assertThat(pks.size()).isEqualTo(100)



    }

    @Test
    fun maybePutTest2() {
        val validate = fun(primaryKey: Any, fields: Fields) : ValidationResult {
            return ValidationResult.Correct()
        }

        val pks = PrimaryKeySampler(.1, validate)

        for(x in 1..100) {
            pks.maybePut(x, mapOf("test" to "test"))
        }
        assertThat(pks.size()).isLessThan(25)

    }



}