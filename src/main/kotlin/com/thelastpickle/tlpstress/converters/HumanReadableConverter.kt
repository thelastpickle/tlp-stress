package com.thelastpickle.tlpstress.converters

import com.beust.jcommander.IStringConverter
import com.beust.jcommander.IStringConverterFactory

class HumanReadableConverter : IStringConverter<Long> {
    override fun convert(value: String?): Long {
        val regex = """(\d+)([BbMmKk]?)""".toRegex()
        val result = regex.find(value!!)

        return result?.groups?.let {
            val value = it[1]?.value
            val label = it[2]

            if(value == null) return 0L

            when(label?.value?.toLowerCase()) {
                "k" -> 1000L * value.toLong()
                "m" -> 1000000L * value.toLong()
                "b" -> 1000000000L * value.toLong()
                else -> value.toLong()
            }
        } ?: 0L
    }
}