package com.thelastpickle.tlpstress

import com.codahale.metrics.Meter
import com.datastax.driver.core.Session

class StressContext(val session: Session,
                    val mainArguments: MainArguments,
                    val thread: Int,
                    val requests: Meter,
                    val errors: Meter) {

}

