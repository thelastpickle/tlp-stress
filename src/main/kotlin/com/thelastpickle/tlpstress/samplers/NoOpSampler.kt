package com.thelastpickle.tlpstress.samplers

class NoOpSampler : ISampler {
    override fun maybePut(primaryKey: Any, fields: Fields) {

    }

    override fun size(): Int {
        return 0
    }

    override fun validate(): ValidationStatistics {
        val statistics = ValidationStatistics()
        return statistics
    }

}