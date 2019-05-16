package com.thelastpickle.tlpstress.profiles

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.CqlSession
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext


class Maps : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var select : PreparedStatement

    override fun prepare(session: CqlSession) {
        insert = session.prepare("UPDATE map_stress SET data[?] = ? WHERE id = ?")
        select = session.prepare("SELECT * from map_stress WHERE id = ?")
    }

    override fun schema(): List<String> {
        val query = """ CREATE TABLE IF NOT EXISTS map_stress (id text, data map<text, text>, primary key (id)) """
        return listOf(query)
    }


    override fun getRunner(context: StressContext): IStressRunner {
        return object : IStressRunner {
            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                return Operation.SelectStatement(insert.bind("key", "value", partitionKey.getText()))
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val b = select.bind(partitionKey.getText())
                return Operation.Mutation(b)
            }

        }
    }
}