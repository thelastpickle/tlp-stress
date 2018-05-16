package com.thelastpickle.tlpstress

import com.datastax.driver.core.Session

data class StressContext(val session: Session,
                         val mainArguments: MainArguments,
                         val profileArguments: Any,
                         val thread: Int,
                         val metrics: Metrics )


