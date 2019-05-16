package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel

class ConsistencyLevelConverter : IStringConverter<DefaultConsistencyLevel> {
    override fun convert(value: String?): DefaultConsistencyLevel {
        return DefaultConsistencyLevel.valueOf(value!!)
    }
}