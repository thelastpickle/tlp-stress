package com.thelastpickle.tlpstress


class SchemaBuilder(var baseStatement : String) {
    private var compaction = ""
    private var compression = ""

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

    fun build() : String {
        val sb = StringBuilder(baseStatement)

        val parts = mutableListOf<String>()

        if(compaction.length > 0)
            parts.add("compaction = $compaction")
        if(compression.length > 0)
            parts.add("compression = $compression")

        val stuff = parts.joinToString(" AND ")

        if(stuff.length > 0 && !baseStatement.toLowerCase().contains("\\swith\\s".toRegex())) {
            sb.append(" WITH ")
        } else if(stuff.count() > 0) {
            sb.append(" AND ")
        }

        sb.append(stuff)

        return sb.toString()
    }


}