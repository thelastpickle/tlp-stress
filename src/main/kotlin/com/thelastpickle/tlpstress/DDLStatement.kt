package com.thelastpickle.tlpstress

sealed class DDLStatement {
    class CreateTable() : DDLStatement() {

    }

    class Unknown : DDLStatement()

    companion object {
        fun parse(cql: String) : DDLStatement {
            return DDLStatement.Unknown()
        }
    }
}