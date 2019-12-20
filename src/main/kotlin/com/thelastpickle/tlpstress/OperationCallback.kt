package com.thelastpickle.tlpstress

import com.codahale.metrics.Meter
import com.datastax.driver.core.ResultSet
import com.google.common.util.concurrent.FutureCallback
import com.codahale.metrics.Timer
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.Semaphore

/**
 * Callback after a mutation or select
 * This was moved out of the inline ProfileRunner to make populate mode easier
 * as well as reduce clutter
 */
class OperationCallback(val errors: Meter,
                        val semaphore: Semaphore,
                        val startTime: Timer.Context,
                        val runner: IStressRunner,
                        val op: Operation,
                        var pageRequests : Long = 0) : FutureCallback<ResultSet> {

    companion object {
        val log = logger()
    }

    override fun onFailure(t: Throwable?) {
        semaphore.release()
        errors.mark()
        startTime.stop()

        log.error { t }

    }

    override fun onSuccess(result: ResultSet?) {
        if(result == null)
            error("Unexpected result")

        // this needs to happen before the semaphore is released and the timer is stopped
        // otherwise we end up prematurely releasing the semaphore and recording a time that's not accounting
        // for the pagination
        if(op is Operation.SelectStatement) {
            log.info { "select statement, might need to fetch more"}

            while(!result.isFullyFetched ) {
                log.info { "We need to fetch more" }
                log.info { result.executionInfo.pagingState.toString() }

                val tmp = result.fetchMoreResults()

                if(tmp.isCancelled || tmp.isDone) {
                    log.info {"Cancelled or done"}
                    break
                }

                log.info { "Waiting on fetch "}

                val moreResults = tmp.get()

                log.info { "Fetch OK, request # $pageRequests"}
                pageRequests++
            }
            log.info { "Select finished" }
        }

        log.info { "releasing semaphore" }

        semaphore.release()
        startTime.stop()

        // we only do the callback for mutations
        // might extend this to select, but I can't see a reason for it now
        if(op is Operation.Mutation) {
            runner.onSuccess(op, result)
        }

    }
}