package com.thelastpickle.tlpstress

import com.google.common.util.concurrent.Futures
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.Operation
import org.apache.logging.log4j.kotlin.logger
import java.time.LocalDateTime
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadLocalRandom

class PartitionKeyGeneratorException(e: String) : Exception()

/**
 * Single threaded profile runner.
 * One profile runner should be created per thread
 * Logs all errors along the way
 * Keeps track of useful metrics, per thread
 */
class ProfileRunner(val context: StressContext,
                    val profile: IStressProfile,
                    val partitionKeyGenerator: PartitionKeyGenerator) {

    companion object {
        fun create(context: StressContext, profile: IStressProfile) : ProfileRunner {

            val partitionKeyGenerator = getGenerator(context, context.mainArguments.partitionKeyGenerator)

            return ProfileRunner(context, profile, partitionKeyGenerator)
        }

        fun getGenerator(context: StressContext, name: String) : PartitionKeyGenerator {
            val prefix = context.mainArguments.id + "." + context.thread + "."
            val partitionKeyGenerator = when(name) {
                "normal" -> PartitionKeyGenerator.normal(prefix)
                "random" -> PartitionKeyGenerator.random(prefix)
                "sequence" -> PartitionKeyGenerator.sequence(prefix)
                else -> throw PartitionKeyGeneratorException("not a valid generator")
            }
            return partitionKeyGenerator
        }

        val log = logger()
    }


    val readRate: Double

    init {
        val tmp = context.mainArguments.readRate

        if(tmp != null) {
            readRate = tmp
        }
        else {
            readRate = profile.getDefaultReadRate()
        }
    }

    fun print(message: String) {
        println("[Thread ${context.thread}]: $message")

    }


    /**

     */
    fun run() {

        if (context.mainArguments.duration == 0L) {
            print("Running the profile for ${context.mainArguments.iterations} iterations...")
        } else {
            print("Running the profile for ${context.mainArguments.duration}min...")
        }
        executeOperations(context.mainArguments.iterations, context.mainArguments.duration)
    }

    /**
     * Used for both pre-populating data and for performing the actual runner
     */
    private fun executeOperations(iterations: Long, duration: Long) {

        val desiredEndTime = LocalDateTime.now().plusMinutes(duration)
        var operations = 0
        // create a semaphore local to the thread to limit the query concurrency
        val sem = Semaphore(context.permits)

        val runner = profile.getRunner(context)

        // we use MAX_VALUE since it's essentially infinite if we give a duration
        val totalValues = if (duration > 0) Long.MAX_VALUE else iterations

        for (key in partitionKeyGenerator.generateKey(totalValues, context.mainArguments.partitionValues)) {
            if (duration > 0 && desiredEndTime.isBefore(LocalDateTime.now())) {
                break
            }
            // get next thing from the profile
            // thing could be a statement, or it could be a failure command
            // certain profiles will want to deterministically inject failures
            // others can be randomly injected by the runner
            // I should be able to just tell the runner to inject gossip failures in any test
            // without having to write that code in the profile

            val op : Operation = if(readRate * 100 > ThreadLocalRandom.current().nextInt(0, 100)) {
                runner.getNextSelect(key)
            } else {
                runner.getNextMutation(key)
            }
            
            // if we're using the rate limiter (unlikely) grab a permit
            context.rateLimiter?.run {
                acquire(1)
            }

            sem.acquire()

            val startTime = when(op) {
                is Operation.Mutation -> context.metrics.mutations.time()
                is Operation.SelectStatement -> context.metrics.selects.time()
            }

            val future = context.session.executeAsync(op.bound)
            Futures.addCallback(future, OperationCallback(context, sem, startTime, runner, op) )

            operations++
        }

        // block until all the queries are finished
        sem.acquireUninterruptibly(context.permits)
    }

    /**
     * Prepopulates the database with numRows
     * Mutations only, does not count towards the normal metrics
     * Records all timers in the populateMutations metrics
     * Can (and should) be graphed separately
     */
    fun populate(numRows: Long) {

        val runner = profile.getRunner(context)
        val sem = Semaphore(context.permits)

        fun executePopulate(op: Operation.Mutation) {
            context.rateLimiter?.run {
                acquire(1)
            }
            sem.acquire()

            val startTime = context.metrics.populate.time()
            val future = context.session.executeAsync(op.bound)
            Futures.addCallback(future, OperationCallback(context, sem, startTime, runner, op))
        }

        when(profile.getPopulateOption(context.mainArguments)) {
            is PopulateOption.Custom -> {
                log.info { "Starting a custom population" }

                for (op in runner.customPopulateIter()) {
                    executePopulate(op)
                }
            }
            is PopulateOption.Standard -> {

                log.info("Populating Cassandra with $numRows rows")

                // we follow the same access pattern as normal writes when pre-populating
                for (key in partitionKeyGenerator.generateKey(numRows, context.mainArguments.partitionValues)) {
                    val op = runner.getNextMutation(key) as Operation.Mutation
                    executePopulate(op)
                }

            }
        }
        sem.acquireUninterruptibly(context.permits)
    }


    fun prepare() {
        profile.prepare(context.session)
    }


}