package com.thelastpickle.tlpstress.profiles.basictimeseries

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.datastax.driver.core.utils.UUIDs
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.Book
import com.thelastpickle.tlpstress.generators.DataGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.Random
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.samplers.PrimaryKeySampler
import com.thelastpickle.tlpstress.randomString
import com.thelastpickle.tlpstress.samplers.Fields
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.ValidationResult
import java.util.UUID


/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
@Parameters(commandDescription = "Basic Time Series workload defaulting to TWCS.")
class BasicTimeSeries : IStressProfile {

    data class PrimaryKey(val first: String, val timestamp: UUID)

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
    lateinit var getRow: PreparedStatement
    lateinit var getPartitionHead: PreparedStatement


    // jcommander arguments
    class Arguments {
        @Parameter(names = ["--limit"], description = "Number of rows to return per partition.")
        var limit = 50

    }

    override fun getArguments() : Any {
        return Arguments()
    }


    override fun prepare(session: Session) {
        prepared = session.prepare("INSERT INTO sensor_data (sensor_id, timestamp, data) VALUES (?, ?, ?)")
        getRow = session.prepare("SELECT * from sensor_data WHERE sensor_id = ? AND timestamp = ? ")
        getPartitionHead = session.prepare("SELECT * from sensor_data WHERE sensor_id = ? LIMIT ?")
    }


    class TimeSeriesRunner(val context: StressContext, val insert: PreparedStatement, val select: PreparedStatement, val limit: Int) : IStressRunner {
        override fun getNextSelect(partitionKey: String): Operation {

            val bound = select.bind(partitionKey, limit)
            return Operation.SelectStatement(bound)
        }

        override fun getNextMutation(partitionKey: String) : Operation {
            val data = randomString(100)
            val timestamp = UUIDs.timeBased()
            val bound = insert.bind(partitionKey,timestamp, data)
            val fields = mapOf("data" to data)
            return Operation.Mutation(bound, PrimaryKey(partitionKey, timestamp), fields)
        }

    }

    /**
     * need to fix custom arguments
     */
    override fun getRunner(context: StressContext): IStressRunner {

        return TimeSeriesRunner(context, prepared, getPartitionHead, 500)

    }

    override fun getFieldGenerators(): Map<Field, DataGenerator> {
        return mapOf(Field("sensor_data", "data") to Random(20, 100))
    }


}