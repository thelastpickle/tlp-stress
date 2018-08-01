package com.thelastpickle.tlpstress.generators

class UnsupportedTypeException : Exception()

interface DataGenerator {
    fun getInt() : Int = throw UnsupportedTypeException()
    fun getFloat() : Float = throw UnsupportedTypeException()
    fun getText() : String = throw UnsupportedTypeException()


}