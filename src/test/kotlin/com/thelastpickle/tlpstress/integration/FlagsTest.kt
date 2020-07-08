package com.thelastpickle.tlpstress.integration

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.thelastpickle.tlpstress.commands.Run
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

/**
 * Simple tests for various flags that don't required dedicated testing
 */
class FlagsTest {
    val ip = System.getenv("TLP_STRESS_CASSANDRA_IP") ?: "127.0.0.1"

    val config = DriverConfigLoader.programmaticBuilder()
            .withString(DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS, "DcInferringLoadBalancingPolicy").build()

    val session = CqlSession.builder()
            .addContactPoint(InetSocketAddress(ip, 9042))
            .withConfigLoader(config)
            .build()

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