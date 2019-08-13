package com.thelastpickle.tlpstress.integration

import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.Plugin
import com.thelastpickle.tlpstress.commands.Run
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource


@Retention(AnnotationRetention.RUNTIME)
@MethodSource("getPlugins")
annotation class AllPlugins

/**
 * This test grabs every plugin and ensures it can run against localhost
 * Next step is to start up a docker container with Cassandra
 * Baby steps.
 */
class AllPluginsBasicTest {

    val ip = System.getenv("TLP_STRESS_CASSANDRA_IP") ?: "127.0.0.1"

    val connection = Cluster.builder()
            .addContactPoint(ip)
            .build().connect()

    lateinit var run : Run

    var prometheusPort = 9600

    /**
     * Annotate a test with @AllPlugins
     */
    companion object {
        @JvmStatic
        fun getPlugins() = Plugin.getPlugins().values.filter {
            it.name != "Demo"
        }

    }


    @BeforeEach
    fun cleanup() {
        connection.execute("DROP KEYSPACE IF EXISTS tlp_stress")
        run = Run("placeholder")
    }

    @AfterEach
    fun shutdownMetrics() {


    }

    @AllPlugins
    @ParameterizedTest(name = "run test {0}")
    fun runEachTest(plugin: Plugin) {

        run.apply {
            host = ip
            profile = plugin.name
            iterations = 1000
            rate = 100L
            concurrency = 10L
            partitionValues = 1000
            prometheusPort = prometheusPort++
        }.execute()
    }


}