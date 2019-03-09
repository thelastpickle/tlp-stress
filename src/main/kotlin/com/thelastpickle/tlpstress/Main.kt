package com.thelastpickle.tlpstress

import org.apache.logging.log4j.kotlin.logger


fun main(argv: Array<String>) {

    val log = logger("main")
    

    log.info { "Parsing $argv" }
    val parser = CommandLineParser.parse(argv)

    try {
        parser.execute()
    } catch (e: Exception) {
        log.error { "Crashed with error: " + e.message }
        println(e.message)
        e.printStackTrace()
    } finally {
        // we exit here to kill the console thread otherwise it waits forever.
        // I'm sure a reasonable fix exists, but I don't have time to look into it.
        System.exit(0)
    }

}

