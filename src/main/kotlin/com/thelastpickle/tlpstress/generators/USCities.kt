package com.thelastpickle.tlpstress.generators

import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.toList

data class City (val name:String, val stateShort:String, val stateFull:String, val cityAlias:String) {
    override fun toString() : String {
        return "$name, $stateShort"
    }
}


class USCities(val args: ArrayList<String> = arrayListOf<String>()) : DataGenerator {


    private val cities : List<City>
    private val size : Int
    init {

        val reader = this.javaClass.getResourceAsStream("/us_cities_states_counties.csv").bufferedReader()
        cities = reader.lines().skip(1).map { it.split("|") }.filter { it.size > 4 }.map { City(it[0], it[1], it[2], it[3]) }.toList()
        size = cities.count()

    }

    override fun getText(): String {

        val tmp = ThreadLocalRandom.current().nextInt(0, size)
        return cities[tmp].toString()
    }
}