package com.thelastpickle.tlpstress.profiles

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.functions.Random


class KeyValue : IStressProfile {

    lateinit var insert: PreparedStatement
    lateinit var select: PreparedStatement
    lateinit var delete: PreparedStatement


    override fun prepare(session: CqlSession) {
        insert = session.prepare("INSERT INTO keyvalue (key, value) VALUES (?, ?)")
        select = session.prepare("SELECT * from keyvalue WHERE key = ?")
        delete = session.prepare("DELETE from keyvalue WHERE key = ?")
    }

    override fun schema(): List<String> {
        val table = """CREATE TABLE IF NOT EXISTS keyvalue (
                        key text PRIMARY KEY,
                        value text
                        )""".trimIndent()
        return listOf(table)
    }

    override fun getDefaultReadRate(): Double {
        return 0.5
    }

    override fun getRunner(context: StressContext): IStressRunner {

        val value = context.registry.getGenerator("keyvalue", "value")

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select.bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                val bound = insert.bind(partitionKey.getText(),  data)

                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = delete.bind(partitionKey.getText())
                return Operation.Deletion(bound)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random().apply{min=100; max=200})
    }
}