package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.functions.Random

class Sets : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var update : PreparedStatement
    lateinit var select : PreparedStatement
    lateinit var deleteElement : PreparedStatement
    lateinit var deleteRow : PreparedStatement

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO sets (key, values) VALUES (?, ?)")
        update = session.prepare("UPDATE sets SET values = values + ? WHERE key = ?")
        select = session.prepare("SELECT * from sets WHERE key = ?")
        deleteRow = session.prepare("DELETE FROM sets WHERE key = ?")
        deleteElement = session.prepare("UPDATE sets SET values = values - ? WHERE key = ?")
    }

    override fun schema(): List<String> {
        return listOf("""
            CREATE TABLE IF NOT EXISTS sets (
            |key text primary key,
            |values set<text>
            |)
        """.trimMargin())
    }

    override fun getRunner(context: StressContext): IStressRunner {
        val payload = context.registry.getGenerator("sets", "values")

        return object : IStressRunner {

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val value = payload.getText()
                val bound = update.bind()
                        .setSet(0, setOf(value))
                        .setString(1, partitionKey.getText())

                return Operation.Mutation(bound)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select.bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = deleteRow.bind(partitionKey.getText())
                return Operation.Deletion(bound)
            }

        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        return mapOf(Field("sets", "values") to Random().apply{ min=6; max=16})
    }
}