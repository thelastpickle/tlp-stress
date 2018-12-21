package com.thelastpickle.tlpstress.profiles.lwt

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation

class LWT : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var update: PreparedStatement
    lateinit var select: PreparedStatement

    override fun schema(tableSuffix: String): List<String> {
        return arrayListOf("""CREATE TABLE IF NOT EXISTS lwt$tableSuffix (id text primary key, value int) """)
    }

    override fun prepare(session: Session, tableSuffix: String) {
        insert = session.prepare("INSERT INTO lwt$tableSuffix (id, value) VALUES (?, ?) IF NOT EXISTS")
        update = session.prepare("UPDATE lwt$tableSuffix SET value = ? WHERE id = ? IF value = ?")
        select = session.prepare("SELECT * from lwt$tableSuffix WHERE id = ?")
    }


    override fun getRunner(context: StressContext): IStressRunner {
        data class CallbackPayload(val id: String, val value: Int)

        class LWTRunner : IStressRunner {
            val state = mutableMapOf<String, Int>()

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val currentValue = state[partitionKey.getText()]
                val newValue: Int

                val mutation = if(currentValue != null) {
                    newValue = currentValue + 1
                    update.bind(0, partitionKey, newValue)
                } else {
                    newValue = 0
                    insert.bind(partitionKey, newValue)
                }
                val payload = CallbackPayload(partitionKey.getText(), newValue)
                return Operation.Mutation(mutation, payload)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                return Operation.SelectStatement(select.bind(partitionKey.getText()))

            }

            override fun onSuccess(op: Operation.Mutation, result: ResultSet?) {
                val payload = op.callbackPayload!! as CallbackPayload
                state[payload.id] = payload.value

            }

        }
        return LWTRunner()
    }
}