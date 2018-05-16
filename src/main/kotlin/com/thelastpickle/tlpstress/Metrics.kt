package com.thelastpickle.tlpstress

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import java.util.concurrent.TimeUnit

class Metrics {
    val metrics = MetricRegistry()

    val reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()

    init {
        reporter.start(1, TimeUnit.SECONDS)
    }


    val requests = metrics.meter("requests")
    val errors = metrics.meter("errors")
    val mutations = metrics.meter("mutations")
    val selects = metrics.meter("select")
}