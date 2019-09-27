package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.datastax.driver.core.utils.UUIDs
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.WorkloadParameter
import com.thelastpickle.tlpstress.generators.*
import com.thelastpickle.tlpstress.generators.functions.Random


/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class UdtTimeSeries : IStressProfile {

    override fun schema(): List<String> {
        val queryUdt = """CREATE TYPE IF NOT EXISTS sensor_data_details (
                          data1 text,
                          data2 text,
                          data3 text
                        )""".trimIndent()

        val queryTable = """CREATE TABLE IF NOT EXISTS sensor_data_udt (
                            sensor_id text,
                            timestamp timeuuid,
                            data frozen<sensor_data_details>,
                            primary key(sensor_id, timestamp))
                            WITH CLUSTERING ORDER BY (timestamp DESC)
                           """.trimIndent()

        return listOf(queryUdt, queryTable)
    }

    lateinit var insert: PreparedStatement
    lateinit var getRow: PreparedStatement
    lateinit var getPartitionHead: PreparedStatement

    @WorkloadParameter("Limit select to N rows.")
    var limit = 500

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO sensor_data_udt (sensor_id, timestamp, data) VALUES (?, ?, ?)")
        getPartitionHead = session.prepare("SELECT * from sensor_data_udt WHERE sensor_id = ? LIMIT ?")
    }

    /**
     * need to fix custom arguments
     */
    override fun getRunner(context: StressContext): IStressRunner {

        val dataField = context.registry.getGenerator("sensor_data", "data")

        return object : IStressRunner {

            val udt = context.session.cluster.getMetadata().getKeyspace(context.session.loggedKeyspace).getUserType("sensor_data_details")

            override fun getNextSelect(partitionKey: PartitionKey): Operation {

                val bound = getPartitionHead.bind(partitionKey.getText(), limit)
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey) : Operation {
                val data = dataField.getText()
                val chunks = data.chunked(data.length/3)
                val udtValue = udt.newValue().setString("data1", chunks[0]).setString("data2", chunks[1]).setString("data3", chunks[2])
                val timestamp = UUIDs.timeBased()
                val bound = insert.bind(partitionKey.getText(),timestamp, udtValue)
                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                throw UnsupportedOperationException("Deletions are not implemented for this workload")
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        return mapOf(Field("sensor_data", "data") to Random().apply {min=100; max=200})
    }


}