package com.thelastpickle.tlpstress

import com.datastax.driver.core.Session
import java.util.concurrent.Semaphore

data class StressContext(val session: Session,
                         val mainArguments: MainArguments,
                         val profileArguments: Any,
                         val thread: Int,
                         val metrics: Metrics,
                         val semaphore: Semaphore,
                         val permits : Int)


