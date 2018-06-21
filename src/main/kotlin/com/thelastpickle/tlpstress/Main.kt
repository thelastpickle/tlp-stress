package com.thelastpickle.tlpstress

import com.datastax.driver.core.Cluster
import mu.KotlinLogging
import ch.qos.logback.classic.util.ContextInitializer
import java.util.concurrent.Semaphore

fun main(argv: Array<String>) {

    println("Starting up")

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml")

    val logger = KotlinLogging.logger {}

    val parser = CommandLineParser.parse(argv)

    try {
        parser.execute()
    } catch (e: Exception) {
        println(e)
    } finally {
        System.exit(0)
    }

}

