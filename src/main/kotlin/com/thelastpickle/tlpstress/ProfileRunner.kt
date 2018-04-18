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
class ProfileRunner(val context: StressContext, val profile: IStressProfile) {

    companion object {
        fun create(context: StressContext, profile: IStressProfile) : ProfileRunner {
            return ProfileRunner(context, profile)
        }
    }

    /**
     * Main entrypoint for the runner
     */
    fun execute() {
        context.session.execute("CREATE KEYSPACE IF NOT EXISTS tlp_stress WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};")
        context.session.execute("use tlp_stress")
        prepare()
        run()
        verify()

    }

    /**

     */
    fun run() {

        val max = context.mainArguments.iterations

        var completed = 0
        var errors = 0

        profile.prepare(context.session)

        logger.info { "Starting up runner" }
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
                    val future = context.session.executeAsync(result.bound)
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
        profile.prepare(context.session)
    }


}