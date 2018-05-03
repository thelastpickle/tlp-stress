package com.thelastpickle.tlpstress.profiles

import com.beust.jcommander.Parameter
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.randomString
import com.thelastpickle.tlpstress.round
import java.util.concurrent.ThreadLocalRandom

/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class BasicTimeSeries : IStressProfile {
    override fun schema(): List<String> {
        return listOf()
    }

    lateinit var prepared: PreparedStatement

    class Arguments {
        @Parameter(names=["max_id"], description = "Max id of the sensor")
        var maxId = 10000
    }

    override fun getArguments() : Any {
        return Arguments()
    }


    override fun prepare(session: Session) {
        session.execute("""CREATE TABLE IF NOT EXISTS sensor_data (
                            |sensor_id int,
                            |timestamp timeuuid,
                            |data text,
                            |primary key(sensor_id, timestamp))
                            |WITH CLUSTERING ORDER BY (timestamp DESC)
                            |
                            |""".trimMargin())

        prepared = session.prepare("INSERT INTO sensor_data (sensor_id, timestamp, data) VALUES (?, now(), ?)")
    }

    class TimeSeriesRunner(val insert: PreparedStatement) : IStressRunner {
        override fun getNextOperation(i: Int) : Operation {
            val sensorId = ThreadLocalRandom.current().nextInt(1, 1000)

            val bound = insert.bind(sensorId, randomString(100))
            return Operation.Statement(bound)
        }

    }


    override fun getRunner(): IStressRunner {
        return TimeSeriesRunner(prepared)
    }


}