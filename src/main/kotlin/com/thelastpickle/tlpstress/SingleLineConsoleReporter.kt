package com.thelastpickle.tlpstress

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import java.util.*
import java.util.concurrent.TimeUnit


class SingleLineConsoleReporter(registry: MetricRegistry) : ScheduledReporter(registry,
    "single-line-console-reporter",
    MetricFilter.ALL,
    TimeUnit.SECONDS,
    TimeUnit.MILLISECONDS
    ) {

    var lines = 0L

    var width = mapOf( 0 to 12,
                       1 to 8 ).withDefault { 0 }

    var writeHeaders = listOf("Count", "p99", "5min")


    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {


        if(lines % 10L == 0L)
            printHeader()

        with(timers!!.get("mutations")!!) {
            printColumn(count, width.getValue(0))
            printColumn(snapshot.get99thPercentile(), width.getValue(1))
            printColumn("${fiveMinuteRate}/s", width.getValue(2))
        }

        println()
        lines++
    }

    fun printColumn(value: Double, width: Int) {
        printColumn(value.toString(), width)
    }

    fun printColumn(value: Long, width: Int) {
        printColumn(value.toString(), width)
    }

    fun printColumn(value: Int, width: Int) {
        printColumn(value.toString(), width)
    }

    fun printColumn(value: String, width: Int) {
        val tmp = value.padStart(width)
        print(value)
    }

    fun printHeader() {
        println("Writes")
        var i = 0
        for(h in writeHeaders) {
            val tmp = h.padStart(width.getValue(i))
            print(h)
            print(" ")
            i++
        }



        println()
    }



}
