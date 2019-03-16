package com.thelastpickle.tlpstress.generators

class UnsupportedTypeException : Exception()

interface FieldGenerator {
    fun getInt() : Int = throw UnsupportedTypeException()
    fun getFloat() : Float = throw UnsupportedTypeException()
    fun getText() : String = throw UnsupportedTypeException()

    fun getDescription() : String

    fun setParameters(params: List<String>)
}