package com.thelastpickle.tlpstress.profiles.keyvalue

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.functions.Random
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation


class KeyValue : IStressProfile {

    lateinit var insert: PreparedStatement
    lateinit var select: PreparedStatement


    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO keyvalue (key, value) VALUES (?, ?)")
        select = session.prepare("SELECT * from keyvalue WHERE key = ?")
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

        class KeyValueRunner(val insert: PreparedStatement, val select: PreparedStatement) : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select.bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                val bound = insert.bind(partitionKey.getText(),  data)

                return Operation.Mutation(bound)
            }

        }

        return KeyValueRunner(insert, select)
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random().apply{min=100; max=200})
    }
}