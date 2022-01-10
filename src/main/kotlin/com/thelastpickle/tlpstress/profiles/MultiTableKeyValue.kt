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

    var insert: MutableList<PreparedStatement> = mutableListOf()
    var select: MutableList<PreparedStatement> = mutableListOf()
    var delete: MutableList<PreparedStatement> = mutableListOf()

    @WorkloadParameter("Number of tables to spread the load on.")
    var nbTables = 1

    override fun prepare(session: Session) {
        for (i in 0 until nbTables) {
            insert.add(session.prepare("INSERT INTO keyvalue$i (key, value) VALUES (?, ?)"))
            select.add(session.prepare("SELECT * from keyvalue$i WHERE key = ?"))
            delete.add(session.prepare("DELETE from keyvalue$i WHERE key = ?"))
        }
    }

    override fun schema(): List<String> {
        val listOfTables = mutableListOf<String>()
        for (i in 0 until nbTables) {
            listOfTables.add("""CREATE TABLE IF NOT EXISTS keyvalue$i (
                            key text PRIMARY KEY,
                            value text
                            )""".trimIndent())
        }
        return listOfTables
    }

    override fun getDefaultReadRate(): Double {
        return 0.5
    }

    override fun getRunner(context: StressContext): IStressRunner {
        var ops = 0

        val value = context.registry.getGenerator("keyvalue", "value")

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select[getNextTableId()].bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val data = value.getText()
                val bound = insert[getNextTableId()].bind(partitionKey.getText(),  data)

                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = delete[getNextTableId()].bind(partitionKey.getText())
                return Operation.Deletion(bound)
            }

            private fun getNextTableId(): Int {
                return ++ops % nbTables
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val kv = FieldFactory("keyvalue")
        return mapOf(kv.getField("value") to Random().apply{min=100; max=200})
    }
}