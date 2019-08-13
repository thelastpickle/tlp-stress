package com.thelastpickle.tlpstress

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.FormatFeature
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.logger

fun MutableMap<String, String?>.putInt(key: String, value: Int?) : MutableMap<String, String?> {
    if(value != null) {
        this[key] = value.toString()
    }
    return this
}

class SchemaBuilder(var baseStatement : String) {
    private var ttl: Long = 0
    private var compaction = ""
    private var compression = ""

    private var isCreateTable : Boolean

    var rowCache = "NONE"
    var keyCache = "ALL"

    var log = logger()

    val compactionShortcutRegex = """^(stcs|lcs|twcs)((?:,[0-9a-z]+)*)$""".toRegex()

    enum class WindowUnit(val s : String) {
        MINUTES("MINUTES"),
        HOURS("HOURS"),
        DAYS("DAYS");

        companion object {
            fun get(s: String) : WindowUnit = when(s.toLowerCase()) {
                "minutes" -> MINUTES
                "hours" -> HOURS
                "days" -> DAYS
                else -> throw Exception("not a thing")
            }
        }


    }

    sealed class Compaction {

        val mapper = ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // no nulls
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY) // no empty fields
                .writerWithDefaultPrettyPrinter()

        open fun toCQL() = mapper.writeValueAsString(getOptions()).replace("\"", "'")

        abstract fun getOptions() : Map<String, String?>

        data class STCS(val min: Int?,
                        val max: Int? = null) : Compaction() {
            override fun getOptions() = mutableMapOf<String, String?>(
                   "class" to "SizeTieredCompactionStrategy"
                ).putInt( "max_threshold", max)
                    .putInt( "min_threshold" ,min)
        }

        data class LCS(val sstableSizeInMb : Int? = null,
                       val fanoutSize: Int? = null) : Compaction() {
            override fun getOptions() =
                    mutableMapOf<String, String?>("class" to "LeveledCompactionStrategy")
                            .putInt("sstable_size_in_mb", sstableSizeInMb)
                            .putInt("fanout_size", fanoutSize)

        }

        data class TWCS(val windowSize: Int? = null,
                        val windowUnit: WindowUnit? = null) : Compaction() {
            override fun getOptions() =
                    mutableMapOf<String, String?>("class" to "TimeWindowCompactionStrategy",
                            "compaction_window_unit" to (windowUnit?.s ?: "") )
                            .putInt("compaction_window_size", windowSize)
        }

        data class Unknown(val raw: String) : Compaction() {
            override fun getOptions() = mapOf<String, String?>()
            override fun toCQL() = raw.trim().replace("\"", "'")
        }
    }

    init {

        val options = setOf(RegexOption.IGNORE_CASE,
                            RegexOption.MULTILINE,
                            RegexOption.DOT_MATCHES_ALL)

        val r = "^\\s?create\\s+table\\s.*".toRegex(options)

        isCreateTable = r.matches(baseStatement)
        log.debug("checking $baseStatement, isCreateTable=$isCreateTable")


    }

    companion object {
        fun create(baseStatement: String) : SchemaBuilder {
            return SchemaBuilder(baseStatement)
        }
    }

    fun withCompaction(compaction: String) : SchemaBuilder {

        this.compaction = parseCompaction(compaction).toCQL()
        return this
    }

    fun withCompression(compression: String) : SchemaBuilder {
        this.compression = compression.trim()
        return this
    }
    
    fun withDefaultTTL(ttl: Long) : SchemaBuilder {
        this.ttl = ttl
        return this
    }

    fun build() : String {
        val sb = StringBuilder(baseStatement)


        val parts = mutableListOf<String>()

        // there's a whole bunch of flags we can only use in CREATE TABLE statements

        if(isCreateTable) {

            if(compaction.length > 0)
                parts.add("compaction = $compaction")
            if(compression.length > 0)
                parts.add("compression = $compression")

            parts.add("caching = {'keys': '$keyCache', 'rows_per_partition': '$rowCache'}")
            parts.add("default_time_to_live = $ttl")
        }

        val stuff = parts.joinToString(" AND ")

        if(stuff.length > 0 && !baseStatement.toLowerCase().contains("\\swith\\s".toRegex())) {
            sb.append(" WITH ")
        } else if(stuff.count() > 0) {
            sb.append(" AND ")
        }

        sb.append(stuff)

        val tmp = sb.toString()
        log.info(tmp)
        return tmp
    }

    fun withRowCache(rowCache: String): SchemaBuilder {
        this.rowCache = rowCache
        return this
    }

    fun withKeyCache(keyCache: String): SchemaBuilder {
        this.keyCache = keyCache
        return this
    }

    /**
     * Helper function for compaction shortcuts
     * If the functino parses, we return a
     * @see <a href="https://github.com/thelastpickle/tlp-stress/issues/80">Issue 80 on Github</a>
     */
    fun parseCompaction(compaction: String) : Compaction {
        val parsed = compactionShortcutRegex.find(compaction)
        if(parsed == null) {
            return Compaction.Unknown(compaction)
        }
        val groups = parsed.groupValues
        val strategy = groups[1]
        val options = groups[2].removePrefix(",").split(",").filter{ it.length > 0}
        log.debug("Parsing $compaction: strategy: $strategy, options: $options / ${options.size}")

        return when(strategy) {
            "stcs" -> {
                when(options.size) {
                    0 -> Compaction.STCS(null, null)
                    2 -> Compaction.STCS(options[0].toInt(), options[1].toInt())
                    else -> Compaction.Unknown(compaction)
                }
            }
            /*
            lcs: leveled compaction, all defaults
lcs,<sstable_size_in_mb>: leveled, override the default of 160
lcs,<sstable_size_in_mb>,<fanout_size>: leveled, override the default sstable size of 160 and fanout of 10
             */
            "lcs" -> {
                when(options.size) {
                    0 -> Compaction.LCS()
                    1 -> Compaction.LCS(options.get(0).toInt())
                    2 -> Compaction.LCS(options.get(0).toInt(), options.get(1).toInt())
                    else -> Compaction.Unknown(compaction)
                }
            }
            "twcs" -> {
                when(options.size) {
                    0 -> Compaction.TWCS()
                    2 -> Compaction.TWCS(options[0].toInt(), WindowUnit.get(options[1]) )
                    else -> Compaction.Unknown(compaction)
                }
            }
            else -> Compaction.Unknown(compaction)
        }
    }



}