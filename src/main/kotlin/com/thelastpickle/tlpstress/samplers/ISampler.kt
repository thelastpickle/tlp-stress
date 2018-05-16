package com.thelastpickle.tlpstress.samplers

typealias Fields = Map<String, Any>

sealed class ValidationResult {
    class Correct : ValidationResult()
    class Incorrect() : ValidationResult()
}


data class ValidationStatistics(var correct: Int = 0, var incorrect: Int = 0)

interface ISampler  {
    fun maybePut(primaryKey: Any, fields: Fields)
    fun size() : Int
    fun validate() : ValidationStatistics
}