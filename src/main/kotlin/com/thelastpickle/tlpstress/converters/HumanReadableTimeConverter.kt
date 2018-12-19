package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import org.joda.time.format.PeriodFormatter



class HumanReadableTimeConverter : IStringConverter<Int> {
    override fun convert(value: String?): Int {
        val daysFormatter = PeriodFormatterBuilder()
                .appendDays().appendSuffix("d").appendSeparatorIfFieldsAfter(" ")
                .appendHours().appendSuffix("h").appendSeparatorIfFieldsAfter(" ")
                .appendMinutes().appendSuffix("m")
                .toFormatter();

        val hoursFormatter = PeriodFormatterBuilder()
                .appendHours().appendSuffix("h").appendSeparatorIfFieldsAfter(" ")
                .appendMinutes().appendSuffix("m")
                .toFormatter();

        val minutesFormatter = PeriodFormatterBuilder()
                .appendMinutes().appendSuffix("m")
                .toFormatter();


        var duration = Period()

        if (value!!.contains('d')) {
            duration = daysFormatter.parsePeriod(value)
        } else if (value!!.contains('h')) {
            duration = hoursFormatter.parsePeriod(value)
        } else {
            duration = minutesFormatter.parsePeriod(value)
        }

        return duration.toStandardMinutes().minutes
    }
}