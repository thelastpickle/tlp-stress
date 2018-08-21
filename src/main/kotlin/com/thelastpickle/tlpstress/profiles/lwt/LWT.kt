package com.thelastpickle.tlpstress.profiles.lwt

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation

class LWT : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var update: PreparedStatement
    lateinit var select: PreparedStatement

    override fun schema(): List<String> {
        return arrayListOf("""CREATE TABLE IF NOT EXISTS lwt (id text primary key, value int) """)
    }

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO lwt (id, value) VALUES (?, ?) IF NOT EXISTS")
        update = session.prepare("UPDATE lwt SET value = ? WHERE id = ? IF value = ?")
        select = session.prepare("SELECT * from lwt WHERE id = ?")
    }


    override fun getRunner(context: StressContext): IStressRunner {
        class LWTRunner : IStressRunner {
            val state = mutableMapOf<String, Int>()

            override fun getNextMutation(partitionKey: String): Operation {
                val currentValue = state[partitionKey]

                val mutation = if(currentValue != null) {
                    val newValue = currentValue + 1
                    state[partitionKey] = newValue
                    update.bind(0, partitionKey, newValue)
                } else {
                    state[partitionKey] = 0
                    insert.bind(partitionKey, 0)
                }
                return Operation.Mutation(mutation)
            }

            override fun getNextSelect(partitionKey: String): Operation {
                return Operation.SelectStatement(select.bind(partitionKey))

            }

        }
        return LWTRunner()
    }
}