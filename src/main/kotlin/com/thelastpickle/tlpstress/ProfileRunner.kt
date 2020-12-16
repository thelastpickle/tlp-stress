package com.thelastpickle.tlpstress

import com.google.common.util.concurrent.Futures
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import org.apache.logging.log4j.kotlin.logger
import java.lang.RuntimeException
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.*

class PartitionKeyGeneratorException(e: String) : Exception()

/**
 * Single threaded profile runner.
 * One profile runner should be created per thread
 * Logs all errors along the way
 * Keeps track of useful metrics, per thread
 */

const val STATEMENT_TIMEOUT_IN_MILLISECONDS: Long = 12000L
const val POPULATE_CONCURRENCY: Int = 200
val statementQueue: BlockingQueue<Operation> = LinkedBlockingQueue<Operation>();

class ProfileRunner(val context: StressContext,
                    val profile: IStressProfile,
                    val partitionKeyGenerator: PartitionKeyGenerator,
                    val isStatementGenerator: Boolean) {

    companion object {
        var statementGeneratorIsRunning = false
        fun create(context: StressContext, profile: IStressProfile) : ProfileRunner {

            val partitionKeyGenerator = getGenerator(context, context.mainArguments.partitionKeyGenerator)

            return ProfileRunner(context, profile, partitionKeyGenerator, false)
        }

        fun createStatementGenerator(context: StressContext, profile: IStressProfile) : ProfileRunner {

            val partitionKeyGenerator = getGenerator(context, context.mainArguments.partitionKeyGenerator)

            return ProfileRunner(context, profile, partitionKeyGenerator, true)
        }

        fun getGenerator(context: StressContext, name: String) : PartitionKeyGenerator {
            val prefix = context.mainArguments.id + "." + context.thread + "."
            println("Creating generator $name")
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

    val deleteRate: Double

    init {
        val tmp = context.mainArguments.deleteRate

        if(tmp != null) {
            deleteRate = tmp
        }
        else {
            deleteRate = 0.0
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
        if (isStatementGenerator) {
            generateOperations(context.mainArguments.iterations, context.mainArguments.duration)
            statementGeneratorIsRunning = false
        } else {
            executeStatements()
        }

    }

    /**
     * Used to generate statements and post them into the statement queue
     */
    private fun generateOperations(iterations: Long, duration: Long) {

        val desiredEndTime = LocalDateTime.now().plusMinutes(duration)
        var operations = 0
        val runner = profile.getRunner(context)
        val maxStatementQueueSize = context.thread*context.permits*10

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
            val nextOp = ThreadLocalRandom.current().nextInt(0, 100)
            val op : Operation = getNextOperation(nextOp, runner, key)
            
            // if we're using the rate limiter grab a permit
            if (context.rateLimiter == null) {
                // No rate limiting, backpressure will be applied on the queue size
                // The queue is allowed to contain ten times the number of concurrent requests times the number of threads
                // This should give a big enough buffer
                while (statementQueue.size >= maxStatementQueueSize.coerceAtLeast(1000)) {
                    Thread.sleep(10)
                }
            } else {
                context.rateLimiter.acquire(1)
            }
            statementQueue.add(op)
            operations++
        }
    }

    /**
     * Used for both pre-populating data and for performing the actual runner
     */
    private fun executeStatements() {
        val semaphore = Semaphore(context.permits)
        val runner = profile.getRunner(context)

        var op: Operation? = null
        while (true) {
            op = statementQueue.poll(1, TimeUnit.SECONDS)
            if (op != null) {
                semaphore.acquire()
                if (op.startTimestamp + STATEMENT_TIMEOUT_IN_MILLISECONDS < Instant.now().toEpochMilli()) {
                    // the query already spent too much time in the queue
                    OperationCallback(context, op.startTime, runner, op, semaphore).onFailure(RuntimeException("Operation timed out"))
                } else {
                    val future = context.session.executeAsync(op.bound)
                    Futures.addCallback(future, OperationCallback(context, op.startTime, runner, op, semaphore))
                }
            } else {
                if (!ProfileRunner.statementGeneratorIsRunning) {
                    break
                }
            }
        }
        // block until all the queries are finished
        semaphore.acquireUninterruptibly(context.permits)
    }

    private fun getNextOperation(nextOp: Int, runner: IStressRunner, key: PartitionKey): Operation {
        return if (readRate * 100 > nextOp) {
            runner.getNextSelect(key)
        } else if ((readRate * 100) + (deleteRate * 100) > nextOp) {
            runner.getNextDelete(key)
        } else {
            runner.getNextMutation(key)
        }
    }

    /**
     * Prepopulates the database with numRows
     * Mutations only, does not count towards the normal metrics
     * Records all timers in the populateMutations metrics
     * Can (and should) be graphed separately
     */
    fun populate(numRows: Long) {

        val runner = profile.getRunner(context)
        val semaphore = Semaphore(POPULATE_CONCURRENCY)

        fun executePopulate(op: Operation.Mutation) {
            semaphore.acquire()

            val startTime = context.metrics.populate.time()
            val future = context.session.executeAsync(op.bound)
            Futures.addCallback(future, OperationCallback(context, startTime, runner, op, semaphore))
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
        semaphore.acquireUninterruptibly(POPULATE_CONCURRENCY)
    }


    fun prepare() {
        profile.prepare(context.session)
    }


}