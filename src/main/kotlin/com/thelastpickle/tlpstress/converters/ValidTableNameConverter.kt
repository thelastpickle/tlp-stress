package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import org.joda.time.format.PeriodFormatter



class ValidTableNameConverter : IStringConverter<String> {
    override fun convert(value: String?): String {
        val re = Regex("[^a-z0-9_]")
        return re.replace(value!!, "_")
    }
}