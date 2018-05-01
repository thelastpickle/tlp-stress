package com.thelastpickle.tlpstress.profiles

import com.beust.jcommander.Parameter
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.randomString
import java.util.concurrent.ThreadLocalRandom

/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class BasicTimeSeries : IStressProfile {

    lateinit var prepared: PreparedStatement

    class Arguments {

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

    override fun getNextOperation(i: Int) : Operation {
        val sensorId = ThreadLocalRandom.current().nextInt(1, 1000)

        val bound = prepared.bind(sensorId, randomString(100))
        return Operation.Statement(bound)
    }

}