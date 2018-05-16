package com.thelastpickle.tlpstress

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PartitionKeyGeneratorTest {
    @Test
    fun basicKeyGenerationTest() {
        val p = PartitionKeyGenerator.random("test")
        val tmp = p.generateKey(1000000)
        val pk = tmp.take(1).toList()[0]
        assertThat(pk).contains("test")
    }
}