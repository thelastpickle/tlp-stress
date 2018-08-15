package com.thelastpickle.tlpstress

import com.codahale.metrics.MetricRegistry
import java.util.concurrent.TimeUnit

class Metrics {
    val metrics = MetricRegistry()

    val reporter = SingleLineConsoleReporter(metrics)

    fun startReporting() {
        reporter.start(1, TimeUnit.SECONDS)
    }

    val errors = metrics.meter("errors")
    val mutations = metrics.timer("mutations")
    val selects = metrics.timer("selects")

}