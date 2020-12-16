package com.thelastpickle.tlpstress

import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.Session
import com.google.common.util.concurrent.RateLimiter
import com.thelastpickle.tlpstress.commands.Run
import com.thelastpickle.tlpstress.generators.Registry
import java.util.concurrent.Semaphore

data class StressContext(val session: Session,
                         val mainArguments: Run,
                         val thread: Int,
                         val metrics: Metrics,
                         val permits: Int,
                         val registry: Registry,
                         val rateLimiter: RateLimiter?,
                         val combineMetrics: Boolean)


