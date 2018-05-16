package com.thelastpickle.tlpstress.samplers

import java.util.concurrent.ThreadLocalRandom

/**
 * data sampler for holding onto stuff we want
 * to validate later
 * Rules:
 * if a key is already held, we updated it
 *
 * Using the validation function gives us flexibility - it's not possible (for me) to understand every validation need
 * This allows us to create arbitrary samplers which fully encapsulate their validation rules
 * Technically each profile could create it's own sampler, but I'm hoping that's not necessary 90% of the time.
 */
class PrimaryKeySampler(val rate: Double, val validationFun : (primaryKey: Any, fields: Fields) -> ValidationResult) : ISampler {

    var data = mutableMapOf<Any, Fields>()

    override fun maybePut(primaryKey: Any, fields: Fields) {
        var shouldAdd = false

        if(data.contains(primaryKey))
            shouldAdd = true

        if(ThreadLocalRandom.current().nextInt(0, 100) < (rate * 100).toInt() )
            shouldAdd = true

        if(shouldAdd) {
            data[primaryKey] = fields
        }
    }

    override fun size(): Int {
        return data.size
    }

    override fun validate() : ValidationStatistics {

        var stats = ValidationStatistics()
        for (row in data.iterator()) {
            when(validationFun(row.key, row.value)) {
                is ValidationResult.Correct ->
                    stats.correct++
                is ValidationResult.Incorrect ->
                    stats.incorrect++
            }
        }
        return stats
    }



}