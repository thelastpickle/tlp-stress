package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.Session
import com.datastax.driver.core.BoundStatement

interface IStressRunner {
    fun getNextOperation(i: Int) : Operation
}

interface IStressProfile {

    fun getArguments() : Any
    fun prepare(session: Session)
    fun schema(): List<String>
    fun getRunner(): IStressRunner
}


sealed class Operation {
    data class Statement(val bound: BoundStatement) : Operation()
    // JMX commands


}