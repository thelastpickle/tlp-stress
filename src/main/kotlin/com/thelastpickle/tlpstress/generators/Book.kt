package com.thelastpickle.tlpstress.generators

class Book(val args: ArrayList<String>) : DataGenerator {
    // secondary constructor shorthand
    constructor(min: Int, max: Int) :
        this(arrayListOf(min.toString(), max.toString()))

}