package com.thelastpickle.tlpstress

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.ScheduledReporter
import java.util.concurrent.TimeUnit

class Metrics(metricRegistry: MetricRegistry, val reporters: List<ScheduledReporter>) {

    fun startReporting() {
        for(reporter in reporters)
            reporter.start(3, TimeUnit.SECONDS)
    }

    val errors = metricRegistry.meter("errors")
    val mutations = metricRegistry.timer("mutations")
    val selects = metricRegistry.timer("selects")

}