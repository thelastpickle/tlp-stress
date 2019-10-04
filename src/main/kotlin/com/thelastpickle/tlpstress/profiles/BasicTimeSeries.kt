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
        val query = """CREATE TABLE IF NOT EXISTS sensor_data (
                            sensor_id text,
                            timestamp timeuuid,
                            data text,
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

    override fun prepare(session: Session) {
        println("Using a limit of $limit for reads and deleting data older than $deleteDepth seconds (if enabled).")
        cassandraVersion = session.cluster.metadata.allHosts.map { host -> host.cassandraVersion }.min()!!
        prepared = session.prepare("INSERT INTO sensor_data (sensor_id, timestamp, data) VALUES (?, ?, ?)")
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

        val dataField = context.registry.getGenerator("sensor_data", "data")

        return object : IStressRunner {

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = getPartitionHead.bind(partitionKey.getText(), limit)
                return Operation.SelectStatement(bound)
            }

            override fun getNextMutation(partitionKey: PartitionKey) : Operation {
                val data = dataField.getText()
                val timestamp = UUIDs.timeBased()
                val bound = prepared.bind(partitionKey.getText(),timestamp, data)
                return Operation.Mutation(bound)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val bound = delete.bind(partitionKey.getText(), Timestamp.valueOf(LocalDateTime.now().minusSeconds(deleteDepth.toLong())))
                return Operation.Deletion(bound)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        return mapOf(Field("sensor_data", "data") to Random().apply { min=100; max=200 })
    }


}