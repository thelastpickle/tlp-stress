package com.thelastpickle.tlpstress

import org.apache.commons.text.RandomStringGenerator
import java.text.DecimalFormat

/*
Will be used later when adding configuration
 */
typealias Range<T> = Pair<T, T>


/*
Throwaway - will need to be thought out for
 */
fun randomString(length: Int) : String {
    val generator = RandomStringGenerator.Builder().withinRange(65, 90).build()
    return generator.generate(length)
}

fun round(num: Double) : Double {
    return DecimalFormat("##.##").format(num).toDouble()
}