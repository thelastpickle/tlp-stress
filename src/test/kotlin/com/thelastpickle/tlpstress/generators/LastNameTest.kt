package com.thelastpickle.tlpstress.generators

import com.thelastpickle.tlpstress.generators.functions.LastName
import org.apache.logging.log4j.kotlin.logger
import org.junit.jupiter.api.Test

internal class LastNameTest {
    val log = logger()

    @Test
    fun getNameTest() {
        val tmp = LastName()
        val n = tmp.getText()
        log.info { n }
    }
}