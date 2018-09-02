package com.thelastpickle.tlpstress.profiles.counterswide

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToLong

class CountersWide : IStressProfile {

    lateinit var increment: PreparedStatement
    lateinit var selectOne: PreparedStatement
    lateinit var selectAll: PreparedStatement

    override fun prepare(session: Session) {
        increment = session.prepare("UPDATE counter_wide SET value = value + 1 WHERE key = ? and cluster = ?")
        selectOne = session.prepare("SELECT * from counter_wide WHERE key = ? AND cluster = ?")
        selectAll = session.prepare("SELECT * from counter_wide WHERE key = ?")
    }

    override fun schema(): List<String> {
        return listOf("""CREATE TABLE IF NOT EXISTS counter_wide (
            | key text,
            | cluster bigint,
            | value counter,
            | primary key(key, cluster))
        """.trimMargin())

    }

    override fun getRunner(context: StressContext): IStressRunner {

        // for now i'm just going to hardcode this at 10K items
        // later when a profile can accept dynamic parameters i'll make it configurable

        val rowsPerPartition = 10000

        class CountersWideRunner : IStressRunner {

            var iterations = 0L

            override fun getNextMutation(partitionKey: PartitionKey): Operation {

                val clusteringKey = (ThreadLocalRandom.current().nextGaussian() * rowsPerPartition.toDouble()).roundToLong()
                val tmp = increment.bind(partitionKey.getText(), clusteringKey)
                return Operation.Mutation(tmp)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                iterations++

                if (iterations % 2 == 0L) {
                    val clusteringKey = (ThreadLocalRandom.current().nextGaussian() * rowsPerPartition.toDouble()).roundToLong()
                    return Operation.SelectStatement(selectOne.bind(partitionKey.getText(), clusteringKey))
                }

                return Operation.SelectStatement(selectAll.bind(partitionKey.getText()))
            }

        }
        return CountersWideRunner()
    }
}