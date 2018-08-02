package com.thelastpickle.tlpstress.generators

import org.apache.commons.text.RandomStringGenerator
import java.util.concurrent.ThreadLocalRandom

class Random(var args: ArrayList<String>) : DataGenerator {

    constructor(min: Int, max: Int) : this(arrayListOf(min.toString(), max.toString()))
    constructor(min: Long, max: Long) : this(arrayListOf(min.toString(), max.toString()))

    val min = args[0].toLong()
    val max = args[1].toLong()

    override fun getInt(): Int {
        if(min > Int.MAX_VALUE || max > Int.MAX_VALUE)
            throw Exception("Int larger than Int.MAX_VALUE requested, use a long instead")

        return ThreadLocalRandom.current().nextInt(min.toInt(), max.toInt())
    }

    override fun getText(): String {
        val length = ThreadLocalRandom.current().nextInt(min.toInt(), max.toInt())

        val generator = RandomStringGenerator.Builder().withinRange(65, 90).build()
        return generator.generate(length)
    }
}