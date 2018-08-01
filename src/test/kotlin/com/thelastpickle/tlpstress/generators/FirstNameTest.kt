package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FirstNameTest {
    @Test
    fun getNameTest() {
        val tmp = FirstName()
        val n = tmp.getText()
        println(n)

    }
}