package com.thelastpickle.tlpstress.integration

import com.codahale.metrics.Meter
import com.codahale.metrics.Timer
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.thelastpickle.tlpstress.OperationCallback
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import io.mockk.mockk
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Semaphore
import kotlin.test.fail

class PaginationTest {
    lateinit var session : Session
    lateinit var testUtils: TestUtils

    var log = logger()

    class SimpleCallback(val sem: Semaphore) : FutureCallback<ResultSet> {
        override fun onSuccess(result: ResultSet?) {
            sem.release()
        }

        override fun onFailure(t: Throwable) {
            fail("Was unable to execute query needed for test setup")
        }

    }

    @BeforeEach
    fun setupSession() {
        testUtils = TestUtils.connect(pageSize = 10)
        session = testUtils.session
    }

    @AfterEach
    fun closeSession() {
        session.cluster.close()
        session.close()

    }

    @Test
    fun testPaginateGetsAllRows() {

        val table = """CREATE TABLE IF NOT EXISTS pagination_test (
            | id int,
            | c int,
            | primary key(id, c)
            |) with clustering order by (c DESC)
        """.trimMargin()

        log.info(table)

        session.execute(table)
        session.execute("TRUNCATE pagination_test")

        val statement = session.prepare("INSERT INTO pagination_test (id, c) VALUES (?, ?)")

        val semCount = 10
        val sem = Semaphore(semCount)
        var total = 0
        for(x in 0..100) {
            sem.acquire()
            val bound = statement.bind(0, x)
            bound.setConsistencyLevel(ConsistencyLevel.QUORUM)
            val future = session.executeAsync(bound)
            Futures.addCallback(future, SimpleCallback(sem))
            total++
        }

        sem.acquireUninterruptibly(semCount)
        sem.release(semCount)
        log.debug("$total rows inserted")

        val runner = mockk<IStressRunner>()

        val bound = session.prepare("SELECT * from pagination_test WHERE id = ?").bind(0)
        bound.setConsistencyLevel(ConsistencyLevel.QUORUM)
        bound.setFetchSize(10)

        val future = session.executeAsync(bound)

        val callback = OperationCallback(Meter(), sem, Timer().time(), runner, Operation.SelectStatement(bound))

        Futures.addCallback(future, callback)

        sem.acquireUninterruptibly()

        log.debug("pages read: ${callback.pageRequests}")
        assertThat(callback.pageRequests).isEqualTo(10)


    }
}