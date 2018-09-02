package com.thelastpickle.tlpstress.profiles.materializedviews

import com.beust.jcommander.Parameter
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.*
import com.thelastpickle.tlpstress.profiles.IStressProfile
import com.thelastpickle.tlpstress.profiles.IStressRunner
import com.thelastpickle.tlpstress.profiles.Operation
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.NoOpSampler
import java.util.concurrent.ThreadLocalRandom

class MaterializedViews : IStressProfile {

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO person (name, age, city) values (?, ?, ?)")
        select_base = session.prepare("SELECT * FROM person WHERE name = ?")
        select_by_age = session.prepare("SELECT * FROM person_by_age WHERE age = ?")
        select_by_city = session.prepare("SELECT * FROM person_by_city WHERE city = ?")


    }

    override fun schema(): List<String> = listOf("""CREATE TABLE IF NOT EXISTS person
                        | (name text, age int, city text, primary key(name))""".trimMargin(),

                        """CREATE MATERIALIZED VIEW IF NOT EXISTS person_by_age AS
                            |SELECT age, name, city FROM person
                            |WHERE age IS NOT NULL AND name IS NOT NULL
                            |PRIMARY KEY (age, name)""".trimMargin(),

                        """CREATE MATERIALIZED VIEW IF NOT EXISTS person_by_city AS
                            |SELECT city, name, age FROM person
                            |WHERE city IS NOT NULL AND name IS NOT NULL
                            |PRIMARY KEY (city, name) """.trimMargin())

    override fun getRunner(context: StressContext): IStressRunner {

        class MVRunner : IStressRunner {
            var select_count = 0L

            val cities = context.registry.getGenerator("person", "city")

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val num = ThreadLocalRandom.current().nextInt(1, 110)
                return Operation.Mutation(insert.bind(partitionKey.getText(), num, cities.getText()))
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val num = ThreadLocalRandom.current().nextInt(1, 110)
                val result = when(select_count % 2L) {
                    0L ->
                        Operation.SelectStatement(select_by_age.bind(num))
                    else ->
                        Operation.SelectStatement(select_by_city.bind("test"))

                }
                select_count++
                return result
            }

        }
        return MVRunner()
    }

    override fun getFieldGenerators(): Map<Field, DataGenerator> {
        val person = FieldFactory("person")
        return mapOf(person.getField("firstname") to FirstName(),
                     person.getField("lastname") to LastName(),
                     person.getField("city") to USCities()
                    )
    }


    lateinit var insert : PreparedStatement
    lateinit var select_base : PreparedStatement
    lateinit var select_by_age : PreparedStatement
    lateinit var select_by_city : PreparedStatement
}