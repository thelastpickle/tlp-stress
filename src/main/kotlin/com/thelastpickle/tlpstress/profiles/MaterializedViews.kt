package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Session
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.*
import com.thelastpickle.tlpstress.generators.functions.FirstName
import com.thelastpickle.tlpstress.generators.functions.LastName
import com.thelastpickle.tlpstress.generators.functions.USCities
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class MaterializedViews : IStressProfile {

    override fun prepare(session: Session) {
        insert = session.prepare("INSERT INTO person (name, age, city) values (?, ?, ?)")
        select_base = session.prepare("SELECT * FROM person WHERE name = ?")
        select_by_age = session.prepare("SELECT * FROM person_by_age WHERE age = ?")
        select_by_city = session.prepare("SELECT * FROM person_by_city WHERE city = ?")
        delete_base = session.prepare("DELETE FROM person WHERE name = ?")


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

        return object : IStressRunner {
            var select_count = 0L

            val cities = context.registry.getGenerator("person", "city")

            override fun getNextMutation(partitionKey: PartitionKey): Operation {
                val num = ThreadLocalRandom.current().nextInt(1, 110)
                return Operation.Mutation(insert.bind(partitionKey.getText(), num, cities.getText()), context)
            }

            override fun getNextSelect(partitionKey: PartitionKey): Operation {
                val num = ThreadLocalRandom.current().nextInt(1, 110)
                val result = when(select_count % 2L) {
                    0L ->
                        Operation.SelectStatement(select_by_age.bind(num), context)
                    else ->
                        Operation.SelectStatement(select_by_city.bind("test"), context)

                }
                select_count++
                return result
            }

            override fun getNextDelete(partitionKey: PartitionKey): Operation {
                return Operation.Deletion(delete_base.bind(partitionKey.getText()), context)
            }
        }
    }

    override fun getFieldGenerators(): Map<Field, FieldGenerator> {
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
    lateinit var delete_base : PreparedStatement
}