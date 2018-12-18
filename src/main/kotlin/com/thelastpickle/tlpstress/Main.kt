package com.thelastpickle.tlpstress

import org.slf4j.LoggerFactory


fun main(argv: Array<String>) {

    val parser = CommandLineParser.parse(argv)
    LoggerFactory.getLogger("main")

    try {
        parser.execute()
    } catch (e: Exception) {
        println(e.message)
        e.printStackTrace()
    } finally {
        // we exit here to kill the console thread otherwise it waits forever.
        // I'm sure a reasonable fix exists, but I don't have time to look into it.
        System.exit(0)
    }

}

