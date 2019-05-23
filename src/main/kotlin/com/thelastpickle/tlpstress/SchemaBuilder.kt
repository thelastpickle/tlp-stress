package com.thelastpickle.tlpstress

import org.apache.logging.log4j.kotlin.logger


class SchemaBuilder(var baseStatement : String) {
    private var ttl: Long = 0
    private var compaction = ""
    private var compression = ""

    private var isCreateTable : Boolean

    var rowCache = "NONE"
    var keyCache = "ALL"

    var log = logger()

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
        this.compaction = compaction.trim().replace("\"", "'")
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


        }
        parts.add("default_time_to_live = $ttl")

        val stuff = parts.joinToString(" AND ")

        if(stuff.length > 0 && !baseStatement.toLowerCase().contains("\\swith\\s".toRegex())) {
            sb.append(" WITH ")
        } else if(stuff.count() > 0) {
            sb.append(" AND ")
        }



        sb.append(stuff)

        return sb.toString()
    }

    fun withRowCache(rowCache: String): SchemaBuilder {
        this.rowCache = rowCache
        return this
    }

    fun withKeyCache(keyCache: String): SchemaBuilder {
        this.keyCache = keyCache
        return this
    }


}