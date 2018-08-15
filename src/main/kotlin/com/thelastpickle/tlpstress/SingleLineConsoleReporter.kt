package com.thelastpickle.tlpstress

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.github.ajalt.mordant.TermColors

class SingleLineConsoleReporter(registry: MetricRegistry) : ScheduledReporter(registry,
    "single-line-console-reporter",
    MetricFilter.ALL,
    TimeUnit.SECONDS,
    TimeUnit.MILLISECONDS
    ) {

    val logger = LoggerFactory.getLogger(this.javaClass.simpleName)
    var lines = 0L

    var opHeaders = listOf("Count", "Latency (p99)", "5min (req/s)")
    var width = mutableMapOf<Int, Int>( ).withDefault { 0 }

    // initialize all the headers
    // it's ok if this isn't perfect, it just has to work for the first round of headers
    init {

        for ((i, h) in opHeaders.withIndex()) {
            getWidth(i, h) // first pass - writes
            getWidth(i + opHeaders.size, h) // second pass - reads
        }
    }


    val formatter = DecimalFormat("##.##")


    val termColors = TermColors()

    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {


        if(lines % 10L == 0L)
            printHeader()

        val state = AtomicInteger()

        // this is a little weird, but we should show the same headers for writes & selects
        val queries = listOf(timers!!["mutations"]!!, timers["selects"]!!)

        for(queryType in queries) {
            with(queryType) {
                printColumn(count, state.getAndIncrement())

                val duration = convertDuration(snapshot.get99thPercentile())

                printColumn(duration, state.getAndIncrement())
                printColumn(formatter.format(fiveMinuteRate), state.getAndIncrement())

            }
            print(" | ")
        }

        val errors = meters!!["errors"]!!
        printColumn(errors.count, state.getAndIncrement())
        printColumn(formatter.format(errors.fiveMinuteRate), state.getAndIncrement())


        println()
        lines++
    }

    /*
    Helpers for printing the column with correct spacing
     */
    fun printColumn(value: Double, index: Int) {
        // round to 2 decimal places
        val tmp = DecimalFormat("##.##").format(value)

        printColumn(tmp, index)
    }

    fun printColumn(value: Long, index: Int) {
        printColumn(value.toString(), index)
    }

    fun printColumn(value: Int, index: Int) {
        printColumn(value.toString(), index)
    }

    fun printColumn(value: String, index: Int) {
        val width = getWidth(index, value)
        val tmp = value.padStart(width)
        print(tmp)
    }

    fun printHeader() {

        var widthOfEachOperation = 0

        for(i in 0..opHeaders.size) {
           widthOfEachOperation += getWidth(i)
        }

        val paddingEachSide = (widthOfEachOperation - "Writes".length) / 2

        print(" ".repeat(paddingEachSide))
        print( termColors.blue("Writes"))
        print(" ".repeat(paddingEachSide))
        print(" ".repeat(paddingEachSide))
        print(termColors.blue("Reads"))
        print(" ".repeat(paddingEachSide))

        print(termColors.red("Errors"))

        println()
        var i = 0

        for(x in 0..1) {

            for (h in opHeaders) {

                val colWidth = getWidth(i, h)
                val required = colWidth - h.length

                val tmp = " ".repeat(required) + termColors.underline(h)



                print(tmp)
                i++
            }
            print(" | ")

        }

        val errorHeaders = arrayListOf("Count", "5min (errors/s)")
        // TODO: refactor this + the above loop to be a single function
        for (h in errorHeaders) {

            val colWidth = getWidth(i, h)
            val required = colWidth - h.length

            val tmp = " ".repeat(required) + termColors.underline(h)

            print(tmp)
            i++
        }


        println()

    }

    /**
     * Gets the width for a column, resizing the column if necessary
     */
    fun getWidth(i: Int, value : String = "") : Int {
        val tmp = width.getValue(i)
        if(value.length > tmp) {

            logger.debug("Resizing column[$i] to ${value.length}")
            // give a little extra padding in case the number grows quickly
            width.set(i, value.length + 2)
        }
        return width.getValue(i)
    }



}
