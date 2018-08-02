package com.thelastpickle.tlpstress.profiles.keyvalue

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.DataGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.Random
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.randomString
import com.thelastpickle.tlpstress.samplers.Fields
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.PrimaryKeySampler
import com.thelastpickle.tlpstress.samplers.ValidationResult


class KeyValue : IStressProfile {

    lateinit var insert: PreparedStatement
    lateinit var select: PreparedStatement

    data class PrimaryKey(val first: String)

    class Arguments {

    }

    override fun getArguments(): Any {
        return Arguments()
    }

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



    override fun getRunner(context: StressContext): IStressRunner {

        val value = context.registry.getGenerator("keyvalue", "value")

        class KeyValueRunner(val insert: PreparedStatement, val select: PreparedStatement) : IStressRunner {

            override fun getNextSelect(partitionKey: String): Operation {
                val bound = select.bind(partitionKey )
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: String): Operation {
                val data = value.getText()
                val bound = insert.bind(partitionKey,  data)
                val fields = mapOf("value" to data)

                return Operation.Mutation(bound, PrimaryKey(partitionKey), fields)
            }

        }

        return KeyValueRunner(insert, select)
    }

    override fun getFieldGenerators(): Map<Field, DataGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random(100, 200))
    }
}