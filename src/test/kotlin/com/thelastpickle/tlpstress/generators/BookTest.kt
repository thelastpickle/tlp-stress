package com.thelastpickle.tlpstress.generators

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

internal class BookTest {
    @Test
    fun bookSliceTest() {
        val b = Book()
        var previous = ""
        for(i in 1..10) {
            val tmp = b.getText()
            assertThat(tmp).isNotBlank().isNotEqualToIgnoringCase(previous)
            previous = tmp
        }


    }


}