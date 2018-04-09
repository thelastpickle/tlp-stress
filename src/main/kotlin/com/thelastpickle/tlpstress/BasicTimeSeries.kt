package com.thelastpickle.tlpstress

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import java.util.concurrent.ThreadLocalRandom

/**
 * Create a simple time series use case with some number of partitions
 * TODO make it use TWCS
 */
class BasicTimeSeries : StressProfile {

    override fun execute(session: Session) {
        val partitions: Int = 10000
        val max: Int = 10000000

        session.execute("""CREATE TABLE IF NOT EXISTS sensor_data (
                            |sensor_id int,
                            |timestamp timeuuid,
                            |data text,
                            |primary key(sensor_id, timestamp))
                            |WITH CLUSTERING ORDER BY (timestamp DESC)
                            |""".trimMargin())

        val inFlight = mutableListOf<ResultSetFuture>()
        val maxInflight = 250
        var completed = 0

        val prepared = session.prepare("INSERT INTO sensor_data (sensor_id, timestamp, data) VALUES (?, now(), ?)")

        println("Starting")

        for(x in 1..max) {
            val sensorId = ThreadLocalRandom.current().nextInt(1, partitions)

            val bound = prepared.bind(sensorId, randomString(100))
            val future = session.executeAsync(bound)
            inFlight.add(future)

            if(inFlight.size >= maxInflight) {
                inFlight.forEach {
                    it.uninterruptibly
                    completed++
                }
                val pCompleted = round(completed.toDouble() / max * 100)

                inFlight.clear()
                print("$completed done ($pCompleted%)\r")
            }

        }
        println("Done")


    }

}