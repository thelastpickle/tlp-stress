package com.thelastpickle.tlpstress

import org.slf4j.LoggerFactory


fun main(argv: Array<String>) {

    val parser = CommandLineParser.parse(argv)
    LoggerFactory.getLogger("main")

    try {
        parser.execute()
    } catch (e: Exception) {
        println(e)
    } finally {
        System.exit(0)
    }

}

