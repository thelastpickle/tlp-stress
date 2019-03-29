package com.thelastpickle.tlpstress.prepopulate

import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.Metrics
import com.thelastpickle.tlpstress.Plugin
import com.thelastpickle.tlpstress.ProfileRunner
import com.thelastpickle.tlpstress.commands.Run
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import kotlin.concurrent.fixedRateTimer

sealed class Option {

    abstract fun execute(runArgs: Run,
                         runners: List<ProfileRunner>,
                         metrics: Metrics)

    class Standard : Option() {
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

    class Custom : Option() {
        override fun execute(runArgs: Run,
                             runners: List<ProfileRunner>,
                             metrics: Metrics) {
            runners.parallelStream().map {
                it.populate(runArgs.populate)
            }.count()
        }

    }
}