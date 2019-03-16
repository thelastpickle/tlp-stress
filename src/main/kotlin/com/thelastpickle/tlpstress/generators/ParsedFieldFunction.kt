package com.thelastpickle.tlpstress.generators

import org.apache.logging.log4j.kotlin.logger

/**
 * Helper class that parses the field function spec
 * Example: book(), random(10), random(10, 20)
 */
class ParsedFieldFunction(function: String) {

    val name : String
    val args: List<String>

    companion object {
        val regex = """(^[a-z]+)\((.*)\)""".toRegex()
        val log = logger()

        internal fun parseArguments(s: String) : List<String> {
            log.debug { "Parsing field arguments $s" }
            if(s.trim().isBlank()) return listOf()
            return s.split(",").map { it.trim() }
        }
    }

    init {
        val searchResult = regex.find(function)?.groupValues ?: throw Exception("Could not parse $function as a field function")

        name = searchResult[1]

        args = parseArguments(searchResult[2])
    }

}