package com.thelastpickle.tlpstress.profiles.keyvalue

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.randomString
import com.thelastpickle.tlpstress.samplers.Fields
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.PrimaryKeySampler
import com.thelastpickle.tlpstress.samplers.ValidationResult


class KeyValue : IStressProfile {

    lateinit var insert: PreparedStatement
    lateinit var select: PreparedStatement

    data class PrimaryKey(val first: String)

    class Arguments {

    }

    override fun getArguments(): Any {
        return Arguments()
    }

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO keyvalue (key, value) VALUES (?, ?)")
        select = session.prepare("SELECT * from keyvalue WHERE key = ?")
    }

    override fun schema(): List<String> {
        val table = """CREATE TABLE IF NOT EXISTS keyvalue (
                        key text PRIMARY KEY,
                        value text
                        )""".trimIndent()
        return listOf(table)
    }

    class KeyValueRunner(val insert: PreparedStatement, val select: PreparedStatement) : IStressRunner {
        override fun getNextSelect(partitionKey: String): Operation {
            val bound = select.bind(partitionKey )
            return Operation.SelectStatement(bound)
        }

        override fun getNextMutation(partitionKey: String): Operation {
            val data = randomString(100)
            val bound = insert.bind(partitionKey,  data)
            val fields = mapOf("value" to data)

            return Operation.Mutation(bound, PrimaryKey(partitionKey), fields)
        }

    }

    override fun getRunner(): IStressRunner {
        return KeyValueRunner(insert, select)
    }

    override fun getSampler(session: Session, sampleRate: Double): ISampler {
        var validate = fun(primaryKey: Any, fields: Fields) : ValidationResult {
            val bound = select.bind(primaryKey)
            val result = session.execute(bound).one()
            if(result.getString("value") == fields.get("value"))
                return ValidationResult.Correct()
            return ValidationResult.Incorrect()
        }
        return PrimaryKeySampler(sampleRate, validate)
    }
}