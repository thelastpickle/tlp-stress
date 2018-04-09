package com.thelastpickle.tlpstress

import com.datastax.driver.core.Session

interface StressProfile {

    fun execute(session: Session)
}