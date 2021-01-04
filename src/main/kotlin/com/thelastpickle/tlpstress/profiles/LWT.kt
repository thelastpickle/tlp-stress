package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import java.util.*

class LWT : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var update: PreparedStatement
    lateinit var select: PreparedStatement
    lateinit var delete: PreparedStatement
    lateinit var deletePartition: PreparedStatement

    override fun schema(): List<String> {
        return arrayListOf("""CREATE TABLE IF NOT EXISTS lwt (id text primary key, value int) """)
    }

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO lwt (id, value) VALUES (?, ?) IF NOT EXISTS")
        update = session.prepare("UPDATE lwt SET value = ? WHERE id = ? IF value = ?")
        select = session.prepare("SELECT * from lwt WHERE id = ?")
        delete = session.prepare("DELETE from lwt WHERE id = ? IF value = ?")
        deletePartition = session.prepare("DELETE from lwt WHERE id = ? IF EXISTS")
    }


    override fun getRunner(context: StressContext): IStressRunner {
        data class CallbackPayload(val id: String, val value: Int)

        return object : IStressRunner {
            val state = mutableMapOf<String, Int>()

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val currentValue = state[partitionKey.getText()]
                val newValue: Int

                val mutation = if(currentValue != null) {
                    newValue = currentValue + 1
                    update.bind(0, partitionKey.getText(), newValue)
                } else {
                    newValue = 0
                    insert.bind(partitionKey.getText(), newValue)
                }
                val payload = CallbackPayload(partitionKey.getText(), newValue)
                return Operation.Mutation(mutation, context, payload)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                return Operation.SelectStatement(select.bind(partitionKey.getText()), context)
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                val currentValue = state[partitionKey.getText()]
                val newValue: Int

                val deletion = if(currentValue != null) {
                    delete.bind(partitionKey.getText(), currentValue)
                } else {
                    deletePartition.bind(partitionKey.getText())
                }
                return Operation.Deletion(deletion, context)
            }

            override fun onSuccess(op: Operation.Mutation, result: ResultSet?) {
                val payload = op.callbackPayload!! as CallbackPayload
                state[payload.id] = payload.value

            }

        }
    }
}