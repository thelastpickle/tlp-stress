package com.thelastpickle.tlpstress

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MainArgumentsTest {
    @Test
    fun testCompaction() {
        val args = "--compaction '{\"max_sstable_age_days\": \"2\", \"base_time_seconds\": \"240\", \"class\": \"DateTieredCompactionStrategy\"}' -i 1000000000 -p 1000000 BasicTimeSeries"
    }
}