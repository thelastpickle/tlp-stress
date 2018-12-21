package com.thelastpickle.tlpstress.profiles.maps

import com.beust.jcommander.Parameter
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.NoOpSampler


class Maps : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var select : PreparedStatement

    data class PrimaryKey(val field: String)

    override fun prepare(session: Session, tableSuffix: String) {
        insert = session.prepare("UPDATE map_stress$tableSuffix SET data[?] = ? WHERE id = ?")
        select = session.prepare("SELECT * from map_stress$tableSuffix WHERE id = ?")
    }

    override fun schema(tableSuffix: String): List<String> {
        val query = """ CREATE TABLE IF NOT EXISTS map_stress$tableSuffix (id text, data map<text, text>, primary key (id)) """
        return listOf(query)
    }


    override fun getRunner(context: StressContext): IStressRunner {
        class MapRunner : IStressRunner {
            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                return Operation.SelectStatement(insert.bind("key", "value", partitionKey.getText()))
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val b = select.bind(partitionKey.getText())
                return Operation.Mutation(b)
            }

        }
        return MapRunner()
    }
}