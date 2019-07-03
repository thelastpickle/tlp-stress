package com.thelastpickle.tlpstress

import com.codahale.metrics.*
import com.codahale.metrics.Timer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


class FileReporter(registry: MetricRegistry, outputFile: File) : ScheduledReporter(registry,
        "file-reporter",
        MetricFilter.ALL,
        TimeUnit.SECONDS,
        TimeUnit.MILLISECONDS
) {

    // date 24h time
    // Thu-14Mar19-13.30.00
    private val startTime = Date()

    private val opHeaders = listOf("Count", "Latency (p99)", "1min (req/s)").joinToString(",", postfix = ",")
    private val errorHeaders = listOf("Count", "1min (errors/s)").joinToString(",")


    val buffer = outputFile.bufferedWriter()

    init {
        buffer.write(",,Mutations,,,")
        buffer.write("Reads,,,")
        buffer.write("Errors,")
        buffer.newLine()

        buffer.write("Timestamp, Elapsed Time,")
        buffer.write(opHeaders)
        buffer.write(opHeaders)
        buffer.write(errorHeaders)
        buffer.newLine()
    }

    private fun Timer.getMetricsList(): List<Any> {
        val duration = convertDuration(this.snapshot.get99thPercentile())

        return listOf(this.count, duration, this.oneMinuteRate)
    }

    override fun report(gauges: SortedMap<String, Gauge<Any>>?,
                        counters: SortedMap<String, Counter>?,
                        histograms: SortedMap<String, Histogram>?,
                        meters: SortedMap<String, Meter>?,
                        timers: SortedMap<String, Timer>?) {

        val timestamp = Instant.now().toString()
        val elapsedTime = Instant.now().minusMillis(startTime.time).toEpochMilli() / 1000

        buffer.write(timestamp + "," + elapsedTime + ",")

        val writeRow = timers!!["mutations"]!!
                .getMetricsList()
                .joinToString(",", postfix = ",")

        buffer.write(writeRow)

        val readRow = timers["selects"]!!
                .getMetricsList()
                .joinToString(",", postfix = ",")

        buffer.write(readRow)

        val errors = meters!!["errors"]!!
        val errorRow = listOf(errors.count, errors.oneMinuteRate)
                .joinToString(",", postfix = "\n")

        buffer.write(errorRow)
        buffer.flush()

    }
}
