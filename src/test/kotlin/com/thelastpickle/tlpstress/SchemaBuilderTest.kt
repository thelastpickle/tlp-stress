package com.thelastpickle.tlpstress

import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFails
import kotlin.test.fail

internal class SchemaBuilderTest {
    var log = logger()

    lateinit var createTable : SchemaBuilder

    @BeforeEach
    fun setUp() {
        val statement = """CREATE TABLE test (
                                | id int primary key,
                                | name text
                                | ) """.trimMargin()

        createTable = SchemaBuilder.create(statement)
    }


    @Test
    fun compactionTest() {
        val result = createTable.withCompaction("{ 'class': 'LeveledCompactionStrategy', 'sstable_size_in_mb': 100}").build()
        assertThat(result).contains("sstable_size_in_mb': 100")
        assertThat(result).doesNotContain("compression")
        assertThat(result.toLowerCase()).containsOnlyOnce("with")
    }

    @Test
    fun compressionTest() {
        val c = "{'enabled':false}"
        val result = createTable.withCompression(c).build()
        assertThat(result).contains(c)
    }

    @Test
    fun clusteringOrderTest() {
        val base = """CREATE TABLE IF NOT EXISTS sensor_data (
                            |sensor_id int,
                            |timestamp timeuuid,
                            |data text,
                            |primary key(sensor_id, timestamp))
                            |WITH CLUSTERING ORDER BY (timestamp DESC)
                            |
                            |""".trimMargin()

        val query = SchemaBuilder.create(base)
                .withCompression("{'enabled':enabled}")
                .build()

        assertThat(query.toLowerCase()).containsOnlyOnce("with")

    }

    @Test
    fun createTypeShouldNotHaveWithClause() {
        val query = """CREATE TYPE IF NOT EXISTS sensor_data_details (
                          data1 text,
                          data2 text,
                          data3 text
                        )"""

        val result = SchemaBuilder.create(query)
                .withKeyCache("NONE")
                .build()

        assertThat(result).doesNotContain("WITH")
    }

    @Test
    fun ensureRegexFailsOnStupid() {
        var result = createTable.compactionShortcutRegex.find("stcsf")
        assertThat(result).isNull()
    }

    @Test
    fun ensureRegexMatchesBasic() {
        val result = createTable.compactionShortcutRegex.find("stcs")!!.groupValues
        assertThat(result[1]).isEqualTo("stcs")

        val result2 = createTable.compactionShortcutRegex.find("lcs")!!.groupValues
        assertThat(result2[1]).isEqualTo("lcs")

        val result3 = createTable.compactionShortcutRegex.find("twcs")!!.groupValues
        assertThat(result3[1]).isEqualTo("twcs")
    }


    @Test
    fun ensureRegexMatchesSTCSWithParams() {
        val result = createTable.compactionShortcutRegex.find("stcs,4,48")!!.groupValues
        assertThat(result[2]).isEqualTo(",4,48")
    }

    @Test
    fun testParseStcsComapctionReturnsStcs() {
        when (val compaction = createTable.parseCompaction("stcs")) {
            is SchemaBuilder.Compaction.STCS -> Unit
            else -> {
                fail("Expecting STCS, Got $compaction")
            }

        }
    }

    @Test
    fun testParseLCS() {
        when (val compaction = createTable.parseCompaction("lcs,120,8")) {
            is SchemaBuilder.Compaction.LCS -> {
                assertThat(compaction.fanoutSize).isEqualTo(8)
                assertThat(compaction.sstableSizeInMb).isEqualTo(120)
            }
            else -> {
                fail("Expecting LCS, Got $compaction")
            }

        }

    }
    @Test
    fun testParseTWCS() {
        val compaction = createTable.parseCompaction("twcs,1,days")
        when (compaction) {
            is SchemaBuilder.Compaction.TWCS -> {
                assertThat(compaction.windowSize).isEqualTo(1)
                assertThat(compaction.windowUnit).isEqualTo(SchemaBuilder.WindowUnit.DAYS)
            }
            else -> {
                fail("Expecting TWCS, 1 DAYS, Got $compaction")
            }
        }
        val cql = compaction.toCQL()

    }

    @Test
    fun tesstFullCompactionShortcut() {
        val result = createTable.withCompaction("lcs").build()
        assertThat(result).contains("LeveledCompactionStrategy")
        assertThat(result).doesNotContain("null")

    }

    @Test
    fun testTWCSEmptyWindow() {
        val result = createTable.withCompaction("twcs").build()
        assertThat(result).doesNotContain("compaction_window_unit")
    }
}