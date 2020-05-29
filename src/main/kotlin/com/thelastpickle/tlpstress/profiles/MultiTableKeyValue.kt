package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.WorkloadParameter
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.functions.Random


class MultiTableKeyValue : IStressProfile {

    lateinit var insert: MutableList<PreparedStatement>
    lateinit var select: MutableList<PreparedStatement>
    lateinit var delete: MutableList<PreparedStatement>

    @WorkloadParameter("Number of tables to spread the load on.")
    var nbTables = 1

    override fun prepare(session: Session) {
        insert = mutableListOf()
        select = mutableListOf()
        delete = mutableListOf()
        for (i in 1..nbTables) {
            insert.add(session.prepare("INSERT INTO keyvalue$i (key, value) VALUES (?, ?)"))
            select.add(session.prepare("SELECT * from keyvalue$i WHERE key = ?"))
            delete.add(session.prepare("DELETE from keyvalue$i WHERE key = ?"))
        }
    }

    override fun schema(): List<String> {
        val listOfTables = listOf<String>()
        for (i in 1..nbTables) {
            val table = """CREATE TABLE IF NOT EXISTS keyvalue$i (
                            key text PRIMARY KEY,
                            value text
                            )""".trimIndent()
        }
        return listOfTables
    }

    override fun getDefaultReadRate(): Double {
        return 0.5
    }

    override fun getRunner(context: StressContext): IStressRunner {

        val value = context.registry.getGenerator("keyvalue", "value")

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select[(System.currentTimeMillis() % nbTables).toInt()].bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                val bound = insert[(System.currentTimeMillis() % nbTables).toInt()].bind(partitionKey.getText(),  data)

                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = delete[(System.currentTimeMillis() % nbTables).toInt()].bind(partitionKey.getText())
                return Operation.Deletion(bound)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random().apply{min=100; max=200})
    }
}