package com.thelastpickle.tlpstress

import com.datastax.oss.driver.api.core.CqlSession
import com.google.common.util.concurrent.RateLimiter
import com.thelastpickle.tlpstress.commands.Run
import com.thelastpickle.tlpstress.generators.Registry

data class StressContext(val session: CqlSession,
                         val mainArguments: Run,
                         val thread: Int,
                         val metrics: Metrics,
                         val permits: Int,
                         val registry: Registry,
                         val rateLimiter: RateLimiter?)


