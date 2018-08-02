package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class LastNameTest {
    @Test
    fun getNameTest() {
        val tmp = LastName()
        val n = tmp.getText()
        println(n)

    }
}