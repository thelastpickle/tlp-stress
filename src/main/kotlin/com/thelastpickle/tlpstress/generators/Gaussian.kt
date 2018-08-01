package com.thelastpickle.tlpstress.generators


class Gaussian(args: ArrayList<String>) : DataGenerator {

    val min: Long
    val max: Long

    init {
        min = args[1].toLong()
        max = args[2].toLong()
    }


}