package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.datastax.oss.driver.api.core.CqlSession
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.PopulateOption
import com.thelastpickle.tlpstress.ProfileRunner
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.commands.Run
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * Warning: this workload is under development and should not be used as a reference across multiple tlp-stress runs with
 * different versions of tlp-stress as the implementation may change!
 *
 * Load test for a case where we have a dataset that requires LWT for a status update type workload
 * This could be a lock on status or a state machine in the real world
 *
 * For this test, we'll use the following states
 *
 * 0: normal
 * 1: temporarily locked
 */
class Locking : IStressProfile {



    lateinit var insert: PreparedStatement
    lateinit var update: PreparedStatement
    lateinit var select: PreparedStatement

    var log = logger()

    override fun prepare(session: CqlSession) {
        insert = session.prepare("INSERT INTO lwtupdates (item_id, name, status) VALUES (?, ?, 0)")
        update = session.prepare("UPDATE lwtupdates set status = ? WHERE item_id = ? IF status = ?")
        select = session.prepare("SELECT * from lwtupdates where item_id = ?")
    }

    override fun schema(): List<String> {
        val query = """
            CREATE TABLE IF NOT EXISTS lwtupdates (
                item_id text primary key,
                name text,
                status int
            )
        """.trimIndent()
        return listOf(query)
    }

    override fun getPopulateOption(args: Run) : PopulateOption = PopulateOption.Custom(args.partitionValues)



    override fun getRunner(context: StressContext): IStressRunner {
        return object : IStressRunner {

            // this test can't do more than 2 billion partition keys

            val state : ConcurrentHashMap<String, Int> = ConcurrentHashMap(context.mainArguments.partitionValues.toInt())

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val currentState = state.getOrDefault(partitionKey.getText(), 0)

                val newState = when(currentState) {
                    0 -> 1
                    else -> 0
                }

                val bound = update.bind(newState, partitionKey.getText(), newState)
                state[partitionKey.getText()] = newState
                return Operation.Mutation(bound)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val bound = select.bind(partitionKey.getText())
                return Operation.SelectStatement(bound)
            }

            override fun customPopulateIter() = iterator {

                val generator = ProfileRunner.getGenerator(context, "sequence")
                for(partitionKey in generator.generateKey(context.mainArguments.partitionValues, context.mainArguments.partitionValues)) {
                    val bound = insert.bind(partitionKey.getText(), "test")
                    yield(Operation.Mutation(bound))
                }

            }



        }
    }


}