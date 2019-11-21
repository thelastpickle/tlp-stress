package com.thelastpickle.tlpstress.integration

import com.datastax.driver.core.Session
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PaginationTest {
    lateinit var session : Session
    lateinit var testUtils: TestUtils

    @BeforeAll
    fun setupSession() {
        testUtils = TestUtils.connect()
        session = testUtils.session
    }

    @AfterAll
    fun closeSession() {
        session.close()
    }

    @Test
    fun testPaginateGetsAllRows() {

        val table = """CREATE TABLE pagination_test (
            | id int,
            | c int,
            | primary key(id, c)
            |) with clustering order by (c DESC)
        """.trimMargin()

        session.execute(table)

        val statement = session.prepare("INSERT INTO pagination_test (id, c) VALUES (?, ?)")

        for(x in 0..10000) {
            val bound = statement.bind(0, x)
            session.executeAsync(bound)
        }

        val future = session.execute("SELECT * from pagination_test WHERE id = 0")


    }
}