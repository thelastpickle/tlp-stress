package com.thelastpickle.tlpstress

import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.commands.Run
import java.util.concurrent.Semaphore

data class StressContext(val session: Session,
                         val mainArguments: Run,
                         val thread: Int,
                         val metrics: Metrics,
                         val semaphore: Semaphore,
                         val permits : Int)


