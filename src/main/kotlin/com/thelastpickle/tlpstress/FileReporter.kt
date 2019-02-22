package com.thelastpickle.tlpstress

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import java.io.File
import java.io.FileWriter
import java.text.DecimalFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class FileReporter(registry: MetricRegistry) : ScheduledReporter(registry,
        "file-reporter",
        MetricFilter.ALL,
        TimeUnit.SECONDS,
        TimeUnit.MILLISECONDS
) {
    private val startingTimestamp = Instant.now().toString()
    private val metricsDir = "metrics-$startingTimestamp"
    private val readFilename = "$metricsDir/read-$startingTimestamp.csv"
    private val writeFilename = "$metricsDir/write-$startingTimestamp.csv"
    private val errorFilename = "$metricsDir/error-$startingTimestamp.csv"

    private val opHeaders = listOf("Timestamp", "Count", "Latency (p99)", "1min (req/s)").joinToString(",")
    private val errorHeaders = listOf("Timestamp", "Count", "1min (errors/s)").joinToString(",")

    init {
        File(metricsDir).mkdir()

        writeToFile(readFilename, opHeaders)
        writeToFile(writeFilename, opHeaders)
        writeToFile(errorFilename, errorHeaders)
    }

    private fun Timer.getMetricsList(timestamp: String): List<Any> {
        val duration = convertDuration(this.snapshot.get99thPercentile())
        return listOf(timestamp, this.count, duration, this.oneMinuteRate)
    }

    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {

        val timestamp = Instant.now().toString()

        val writeRow = timers!!["mutations"]!!
                .getMetricsList(timestamp)
                .joinToString(",")
        writeToFile(writeFilename, writeRow)

        val readRow = timers["selects"]!!
                .getMetricsList(timestamp)
                .joinToString(",")
        writeToFile(readFilename, readRow)

        val errors = meters!!["errors"]!!
        val errorRow = listOf(timestamp, errors.count, errors.oneMinuteRate)
                .joinToString(",")
        writeToFile(errorFilename, errorRow)
    }


    fun writeToFile(filename: String, text: String) {
        FileWriter(filename, true).use { out ->
            out.write("$text\n")
        }
    }
}
