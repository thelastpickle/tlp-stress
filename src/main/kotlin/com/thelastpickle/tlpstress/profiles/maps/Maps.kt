package com.thelastpickle.tlpstress.profiles.maps

import com.beust.jcommander.Parameter
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.NoOpSampler


class Maps : IStressProfile {

    lateinit var insert : PreparedStatement
    lateinit var select : PreparedStatement

    data class PrimaryKey(val field: String)

    override fun getArguments(): Any {
        // jcommander arguments
        class Arguments {
            @Parameter(names = ["--limit"], description = "Number of rows to return per partition.")
            var limit = 50

        }
        return Arguments()
    }

    override fun prepare(session: Session) {
        insert = session.prepare("UPDATE map_stress SET data[?] = ? WHERE id = ?")
        select = session.prepare("SELECT * from map_stress WHERE id = ?")
    }

    override fun schema(): List<String> {
        val query = """ CREATE TABLE IF NOT EXISTS map_stress (id text, data map<text, text>, primary key (id)) """
        return listOf(query)
    }


    override fun getRunner(context: StressContext): IStressRunner {
        class MapRunner : IStressRunner {
            override fun getNextMutation(partitionKey: String): Operation {
                return Operation.SelectStatement(insert.bind("key", "value", partitionKey))
            }

            override fun getNextSelect(partitionKey: String): Operation {
                val b = select.bind(partitionKey)
                val fields = mapOf<String, String>()
                return Operation.Mutation(b, PrimaryKey(partitionKey), fields)
            }

        }
        return MapRunner()
    }



    override fun getSampler(session: Session, sampleRate: Double): ISampler {
        return NoOpSampler()
    }
}