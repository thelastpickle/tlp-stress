package com.thelastpickle.tlpstress


class SchemaBuilder(var baseStatement : String) {
    var compaction : String? = null
    var compression : String? = null

    companion object {
        fun create(baseStatement: String) : SchemaBuilder {
            return SchemaBuilder(baseStatement)
        }
    }

    fun withCompaction(compaction: String) : SchemaBuilder {
        this.compaction = compaction
        return this
    }

    fun withCompression(compression: String) : SchemaBuilder {
        this.compression = compression
        return this
    }

    fun build() : String {
        val sb = StringBuilder(baseStatement)

        val parts = mutableListOf<String>()

        if(compaction != null)
            parts.add("compaction = $compaction")
        if(compression != null)
            parts.add("compression = $compression")

        val stuff = parts.joinToString(" AND ")
        if(stuff.count() > 0) {
            sb.append(" WITH ")
            sb.append(stuff)
        }

        return sb.toString()
    }


}