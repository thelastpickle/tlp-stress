package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import java.time.Duration


class HumanReadableTimeConverter : IStringConverter<Long> {
    override fun convert(value: String?): Long {
        var duration = Duration.ofMinutes(0)

        val integer = "\\d+"
        val units = "[dhms]"
        val valueCharSequence = value!!.subSequence(0, value.length)
        /**
         * The duration is passed in via the value variable. It could contain multiple time values e.g. "1d 2h 3m 4s".
         * Parse the string using the following process:
         * 1. Find all occurrences of of an integer with a time unit and iterate through the matches. Note we need
         *      to convert the value from a String to CharSequence so we can pass it to findAll.
         * 2. Iterate through the matched values. Add to the duration based on the units of each value.
         */
        Regex("$integer$units")
            .findAll(valueCharSequence)
            .forEach {
                val quantity = Regex(integer).findAll(it.value).first().value.toLong()
                when (Regex(units).findAll(it.value).first().value) {
                    "d" -> duration = duration.plusDays(quantity)
                    "h" -> duration = duration.plusHours(quantity)
                    "m" -> duration = duration.plusMinutes(quantity)
                    "s" -> duration = duration.plusSeconds(quantity)
                }
            }

        if (duration.isZero)
            throw IllegalArgumentException("Value ${value} resulted in 0 time duration")

        return duration.toMinutes()
    }
}