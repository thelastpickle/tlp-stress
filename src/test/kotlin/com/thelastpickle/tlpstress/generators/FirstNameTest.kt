package com.thelastpickle.tlpstress.generators

import com.thelastpickle.tlpstress.generators.functions.FirstName
import org.junit.jupiter.api.Test

internal class FirstNameTest {
    @Test
    fun getNameTest() {
        val tmp = FirstName()
        val n = tmp.getText()
        println(n)

    }
}