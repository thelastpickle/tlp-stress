package com.thelastpickle.tlpstress.generators.functions

import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Function
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.toList

data class City (val name:String, val stateShort:String, val stateFull:String, val cityAlias:String) {
    override fun toString() : String {
        return "$name, $stateShort"
    }
}

@Function(name="uscities",
        description = "US Cities")
class USCities : FieldGenerator {
    private val cities : List<City>
    private val size : Int
    init {

        val reader = this.javaClass.getResourceAsStream("/us_cities_states_counties.csv").bufferedReader()
        cities = reader.lines().skip(1).map { it.split("|") }.filter { it.size > 4 }.map { City(it[0], it[1], it[2], it[3]) }.toList()
        size = cities.count()

    }

    override fun setParameters(params: List<String>) {

    }

    override fun getText(): String {

        val tmp = ThreadLocalRandom.current().nextInt(0, size)
        return cities[tmp].toString()
    }

    override fun getDescription() = """
        Random US cities.
    """.trimIndent()
}