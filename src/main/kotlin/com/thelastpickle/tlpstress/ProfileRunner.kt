package com.thelastpickle.tlpstress

import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.Operation
import mu.KotlinLogging
import kotlin.math.log

private val logger = KotlinLogging.logger {}

/**
 * Single threaded profile runner.
 * One profile runner should be created per thread
 * Logs all errors along the way
 * Keeps track of useful metrics, per thread
 */
class ProfileRunner(val session: Session, val profile: IStressProfile) {

    companion object {
        fun create(session: Session, seed: Int, profile: IStressProfile) : ProfileRunner {
            return ProfileRunner(session, profile)
        }
    }

    fun execute() {
        prepare()
        run()
        verify()

    }

    /**

     */
    fun run() {

        val max: Int = 1000
        var completed = 0
        var errors = 0

        profile.prepare(session)

        logger.info { "Starting up" }
        val inFlight = mutableListOf<ResultSetFuture>()

        for(x in 1..max) {
            // get next thing from the profile
            // thing could be a statement, or it could be a failure command
            // certain profiles will want to deterministically inject failures
            // others can be randomly injected by the runner
            // I should be able to just tell the runner to inject gossip failures in any test
            // without having to write that code in the profile

            val result = profile.getNextOperation(x)

            when(result) {
                is Operation.Statement -> {
                    logger.debug { result }
                    val future = session.executeAsync(result.bound)
                    inFlight.add(future)
                    completed++
                }
            }
        }
        logger.info { "Done" }
    }

    fun verify() {

    }

    fun prepare() {
        profile.prepare(session)
    }


}