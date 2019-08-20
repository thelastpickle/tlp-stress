package com.thelastpickle.tlpstress.integration

import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.commands.Run
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Simple tests for various flags that don't required dedicated testing
 */
class FlagsTest {
    val ip = System.getenv("TLP_STRESS_CASSANDRA_IP") ?: "127.0.0.1"

    val connection = Cluster.builder()
            .addContactPoint(ip)
            .build().connect()

    var keyvalue = Run("placeholder")


    @BeforeEach
    fun resetRunners() {
        keyvalue = keyvalue.apply {
            profile = "KeyValue"
            iterations = 100
        }
    }


    @Test
    fun csvTest() {
        keyvalue.apply {
            csvFile = "test.csv"
        }.execute()

    }
}