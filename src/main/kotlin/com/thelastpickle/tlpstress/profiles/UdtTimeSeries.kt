package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.datastax.driver.core.utils.UUIDs
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.WorkloadParameter
import com.thelastpickle.tlpstress.generators.*
import com.thelastpickle.tlpstress.generators.functions.Random
import java.util.*


/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class UdtTimeSeries : IStressProfile {

    override fun schema(): List<String> {
        var dataColumns = ""
        for (i in 2..nbDataColumns) {
            dataColumns += ", data${i} text"
        }

        val dropTable = """DROP TABLE IF EXISTS sensor_data_udt"""

        val dropUdt = """DROP TYPE IF EXISTS sensor_data_details"""

        val queryUdt = """CREATE TYPE IF NOT EXISTS sensor_data_details (
                          data1 text $dataColumns
                        )""".trimIndent()

        val queryTable = """CREATE TABLE IF NOT EXISTS sensor_data_udt (
                            sensor_id text,
                            timestamp timeuuid,
                            data frozen<sensor_data_details>,
                            primary key(sensor_id, timestamp))
                            WITH CLUSTERING ORDER BY (timestamp DESC)
                           """.trimIndent()

        return listOf(dropTable, dropUdt, queryUdt, queryTable)
    }

    lateinit var insert: PreparedStatement
    lateinit var getPartitionHead: PreparedStatement
    lateinit var deletePartitionHead: PreparedStatement

    @WorkloadParameter("Limit select to N rows.")
    var limit = 500

    @WorkloadParameter("Number of data columns.")
    var nbDataColumns = 1

    @WorkloadParameter("Minimum number of characters per data column")
    var minChars = 100

    @WorkloadParameter("Maximum number of characters per data column")
    var maxChars = 200

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO sensor_data_udt (sensor_id, timestamp, data) VALUES (?, ?, ?)")
        getPartitionHead = session.prepare("SELECT * from sensor_data_udt WHERE sensor_id = ? LIMIT ?")
        deletePartitionHead = session.prepare("DELETE from sensor_data_udt WHERE sensor_id = ?")
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
                return Operation.SelectStatement(bound, context)
            }

            override fun getNextMutation(partitionKey: PartitionKey) : Operation {
                val udtValue = udt.newValue().setString("data1", dataField.getText())
                var dataColumns = ""
                for (i in 2..nbDataColumns) {
                    udtValue.setString("data$i", dataField.getText())
                }
                val timestamp = UUIDs.timeBased()
                val bound = insert.bind(partitionKey.getText(),timestamp, udtValue)
                return Operation.Mutation(bound, context)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = deletePartitionHead.bind(partitionKey.getText())
                return Operation.Deletion(bound, context)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        println("Using between $minChars and $maxChars characters")
        return mapOf(Field("sensor_data", "data") to Random().apply {min=minChars.toLong(); max=maxChars.toLong()})
    }


}