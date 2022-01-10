package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.datastax.driver.core.VersionNumber
import com.datastax.driver.core.utils.UUIDs
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.WorkloadParameter
import com.thelastpickle.tlpstress.generators.*
import com.thelastpickle.tlpstress.generators.functions.Random
import java.lang.UnsupportedOperationException
import java.sql.Timestamp
import java.time.LocalDateTime


/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class BasicTimeSeries : IStressProfile {

    override fun schema(): List<String> {

        var dataColumns = ""
        for (i in 2..nbDataColumns) {
            dataColumns += "data${i} text,"
        }

        val query = """CREATE TABLE IF NOT EXISTS sensor_data (
                            sensor_id text,
                            timestamp timeuuid,
                            data text,
                            $dataColumns
                            primary key(sensor_id, timestamp))
                            WITH CLUSTERING ORDER BY (timestamp DESC)
                           """.trimIndent()

        return listOf(query)
    }

    lateinit var prepared: PreparedStatement
    lateinit var getPartitionHead: PreparedStatement
    lateinit var delete: PreparedStatement
    lateinit var cassandraVersion: VersionNumber

    @WorkloadParameter("Number of rows to fetch back on SELECT queries")
    var limit = 500

    @WorkloadParameter("Deletion range in seconds. Range tombstones will cover all rows older than the given value.")
    var deleteDepth = 30

    @WorkloadParameter("Number of data columns.")
    var nbDataColumns = 1

    @WorkloadParameter("Minimum number of characters per data column")
    var minChars = 100

    @WorkloadParameter("Maximum number of characters per data column")
    var maxChars = 200

    override fun prepare(session: Session) {
        println("Using a limit of $limit for reads and deleting data older than $deleteDepth seconds (if enabled).")
        cassandraVersion = session.cluster.metadata.allHosts.map { host -> host.cassandraVersion }.min()!!
        var dataColumns = ""
        var questionMarks = ""
        for (i in 2..nbDataColumns) {
            dataColumns += ",data${i}"
            questionMarks += ", ?"
        }
        prepared = session.prepare("INSERT INTO sensor_data (sensor_id, timestamp, data $dataColumns) VALUES (?, ?, ? $questionMarks)")
        getPartitionHead = session.prepare("SELECT * from sensor_data WHERE sensor_id = ? LIMIT ?")
        if (cassandraVersion.compareTo(VersionNumber.parse("3.0")) >= 0) {
            delete = session.prepare("DELETE from sensor_data WHERE sensor_id = ? and timestamp < maxTimeuuid(?)")
        } else {
            throw UnsupportedOperationException("Cassandra version $cassandraVersion does not support range deletes (only available in 3.0+).")
        }
    }

    /**
     * need to fix custom arguments
     */
    override fun getRunner(context: StressContext): IStressRunner {

        val dataFields = mutableListOf(context.registry.getGenerator("sensor_data", "data"))
        for (i in 2..nbDataColumns) {
            dataFields.add(context.registry.getGenerator("sensor_data", "data$i"))
        }

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = getPartitionHead.bind(partitionKey.getText(), limit)
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey) : Operation {
                val data = dataFields[0].getText()
                val timestamp = UUIDs.timeBased()
                val values = mutableListOf<Any>(partitionKey.getText(),timestamp, data)
                for (i in 2..nbDataColumns) {
                    values.add(dataFields[i-1].getText())
                }
                val bound = prepared.bind(*values.toTypedArray())
                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = delete.bind(partitionKey.getText(), Timestamp.valueOf(LocalDateTime.now().minusSeconds(deleteDepth.toLong())))
                return Operation.Deletion(bound)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val fields = mutableMapOf<Field, FieldGenerator>()
        fields[Field("sensor_data", "data")] = Random().apply { min=minChars.toLong(); max=maxChars.toLong() }
        for (i in 2..nbDataColumns) {
            fields[Field("sensor_data", "data$i")] = Random().apply { min=minChars.toLong(); max=maxChars.toLong() }
        }
        return fields
    }


}