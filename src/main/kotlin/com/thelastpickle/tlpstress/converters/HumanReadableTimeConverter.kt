package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import java.time.Duration


class HumanReadableTimeConverter : IStringConverter<Long> {
    override fun convert(value: String?): Long {
        var duration = Duration.ofMinutes(0)

        /**
         * The duration string could contain multiple time values e.g. "1d 2h 3m". Split the string by space then
         * operate on the collection of values using the following steps:
         * 1. Run a filter so that we only select values with a number and a unit. The unit must be either
         *      'd' (days), 'h' (hours), or 'm' (mins).
         * 2. Iterate through remaining values. Add to the duration based on the units in each value.
         */
        value!!
            .split(" ")
            .filter{ "[\\d]+[dhm]".toRegex().matches(it) }
            .forEach {
                val quantity = "[\\d]+".toRegex().findAll(it).first().value.toLong()
                when ("[dhm]".toRegex().findAll(it).first().value) {
                    "d" -> duration = duration.plusDays(quantity)
                    "h" -> duration = duration.plusHours(quantity)
                    "m" -> duration = duration.plusMinutes(quantity)
                }
            }

        if (duration.isZero)
            throw IllegalArgumentException("Value ${value} resulted in 0 time duration")

        return duration.toMinutes()
    }
}