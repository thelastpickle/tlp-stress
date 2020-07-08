package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel

class ConsistencyLevelConverter : IStringConverter<ConsistencyLevel> {
    override fun convert(value: String?): ConsistencyLevel {
        return DefaultConsistencyLevel.valueOf(value!!)
    }
}