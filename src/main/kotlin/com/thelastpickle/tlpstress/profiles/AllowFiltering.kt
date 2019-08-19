package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.WorkloadParameter
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.generators.FieldFactory
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.functions.Random
import java.util.concurrent.ThreadLocalRandom

class AllowFiltering : IStressProfile {

    @WorkloadParameter(description = "Number of rows per partition")
    var rows = 100

    @WorkloadParameter(description = "Max Value of the value field.  Lower values will return more results.")
    var maxValue = 100

    lateinit var insert : PreparedStatement
    lateinit var select: PreparedStatement

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO allow_filtering (partition_id, row_id, value, payload) values (?, ?, ?, ?)")
        select = session.prepare("SELECT * from allow_filtering WHERE partition_id = ? and value = ? ALLOW FILTERING")
    }

    override fun schema(): List<String> {
        return listOf("""CREATE TABLE IF NOT EXISTS allow_filtering (
            |partition_id text,
            |row_id int,
            |value int,
            |payload text,
            |primary key (partition_id, row_id)
            |) 
        """.trimMargin())
    }

    override fun getRunner(context: StressContext): IStressRunner {

        val payload = context.registry.getGenerator("allow_filtering", "payload")
        val random = ThreadLocalRandom.current()

        return object : IStressRunner {
            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val rowId = random.nextInt(0, rows)
                val value = random.nextInt(0, maxValue)

                val bound = insert.bind(partitionKey.getText(), rowId, value, payload.getText())
                return Operation.Mutation(bound)

            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val value = random.nextInt(0, maxValue)
                val bound = select.bind(partitionKey.getText(), value)
                return Operation.SelectStatement(bound)
            }

        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
        val af = FieldFactory("allow_filtering")
        return mapOf(af.getField("payload") to Random().apply{ min = 0; max = 1})

    }
}