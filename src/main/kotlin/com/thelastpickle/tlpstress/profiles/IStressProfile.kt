package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.Session
import com.datastax.driver.core.BoundStatement

interface IStressProfile {

    fun prepare(session: Session)
    fun getNextOperation(i: Int) : Operation
}


sealed class Operation {
    data class Statement(val bound: BoundStatement) : Operation()
    // JMX commands


}