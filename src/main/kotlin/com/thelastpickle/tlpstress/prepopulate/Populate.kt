package com.thelastpickle.tlpstress.prepopulate

import com.google.common.util.concurrent.Futures
import com.thelastpickle.tlpstress.Metrics
import com.thelastpickle.tlpstress.OperationCallback
import com.thelastpickle.tlpstress.ProfileRunner
import com.thelastpickle.tlpstress.commands.Run
import com.thelastpickle.tlpstress.profiles.Operation
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import kotlin.concurrent.fixedRateTimer

sealed class Populate {

    abstract fun execute(runArgs: Run,
                         runners: List<ProfileRunner>,
                         metrics: Metrics)

    class Standard : Populate() {
        override fun execute(runArgs: Run,
                             runners: List<ProfileRunner>,
                             metrics: Metrics) {
            if(runArgs.populate > 0) {
                // .use is the kotlin version of try with resource
                ProgressBar("Populate Progress", runArgs.threads * runArgs.populate, ProgressBarStyle.ASCII).use {
                    // update the timer every second, starting 1 second from now, as a daemon thread
                    val timer = fixedRateTimer("progress-bar", true, 1000, 1000) {
                        it.stepTo(metrics.populate.count)
                    }

                    runners.parallelStream().map {
                        it.populate(runArgs.populate)
                    }.count()

                    // stop outputting the progress bar
                    timer.cancel()
                    println("Pre-populate complete.")
                    // allow the time to die out
                    Thread.sleep(1000)
                }
            }

        }

    }

    class Custom : Populate() {
        override fun execute(runArgs: Run,
                             runners: List<ProfileRunner>,
                             metrics: Metrics) {

            val numRows = runArgs.populate
            var partitionKeyGenerator = runArgs.partitionKeyGenerator

            runners.parallelStream().map {
                ProfileRunner.log.info("Populating Cassandra with $numRows rows")

                // we follow the same access pattern as normal writes when pre-populating
                for (key in partitionKeyGenerator.generateKey(numRows, context.mainArguments.partitionValues)) {

                    sem.acquire()
                    val op = runner.getNextMutation(key) as Operation.Mutation
                    val startTime = context.metrics.populate.time()
                    val future = context.session.executeAsync(op.bound)

                    Futures.addCallback(future, OperationCallback(context, sem, startTime, runner, op))
                }
            }.count()
        }

    }
}