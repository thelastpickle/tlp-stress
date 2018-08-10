package com.thelastpickle.tlpstress.generators

data class Field(val table: String, val field: String)

class FieldFactory(private val table: String) {
    fun getField(field: String) : Field = Field(table, field)
}

/**
 * Registry for data generators
 * When the original schema is created, the registry will be set up with default generators for each field
 * A generator option can be overridden on the command line as a dynamic flag with field.*
 * The idea here is we should be able to customize the data we increment without custom coding
 * for instance, I could use random(1, 100) to be an int field of 1-100 or a text field of 1-100 characters.
 * book(10, 100) is a random selection of 10-100 words from a bunch of open source licensed books
 * Ideally we have enough here to simulate a lot (call it 90%) of common workloads
 *
 */
class Registry(val generators: Map<String, Class<out DataGenerator>> = mutableMapOf(),
               val defaults: MutableMap<Field, DataGenerator> = mutableMapOf(),
               val overrides: MutableMap<Field, DataGenerator> = mutableMapOf()) {




    companion object {
        fun create(defaults: MutableMap<Field, DataGenerator>) : Registry {
            val data = Registry.getGenerators()
            return Registry(data, defaults)
        }

        fun create() : Registry {
            val data = Registry.getGenerators()
            return Registry(data)
        }

        private fun getGenerators() : Map<String, Class<out DataGenerator>>
                = mutableMapOf("cities" to USCities::class.java,
                                "gaussian" to Gaussian::class.java,
                                "book" to Book::class.java,
                                "random" to Random::class.java,
                                "firstname" to FirstName::class.java)

        /**
         *
         */
        fun getInstance(s: String) : DataGenerator {

            val name = Registry.getName(s)
            val args = Registry.getArguments(s)

            val cls = getGenerators()[name]!!

            val arg = ArrayList<String>().javaClass
            val constructor = cls.getConstructor(arg)
            return constructor.newInstance(args)
        }



        internal fun getName(s: String) : String {
            val nameRegex = """(^[a-z]+)\(.*\)""".toRegex()
            val result = nameRegex.find(s)
            return result!!.groupValues[1]
        }

        internal fun getArguments(s: String) : List<String> {
            val nameRegex = """(^[a-z]+)\((.*)\)""".toRegex()
            val result = nameRegex.find(s)
            val tmp = result!!.groupValues[2]
            return tmp.split(",").map { it.trim() }
        }
    }

    /**
     * Sets the default generator for a table / field pair
     * Not all generators work on all fields
     */
    fun setDefault(table: String, field: String, generator: DataGenerator) : Registry {
        val f = Field(table, field)
        return this.setDefault(f, generator)
    }

    fun setDefault(field: Field, generator: DataGenerator) : Registry {
        defaults[field] = generator
        return this
    }

    /**
     * Overrides the default generator for a table / field pair
     * Not all generators work on all fields
     *
     * @param table table that's affected
     * @param field field that's affected
     */
    fun setOverride(table: String, field: String, generator: DataGenerator) : Registry {
        val f = Field(table, field)
        return this.setOverride(f, generator)
    }

    fun setOverride(field: Field, generator: DataGenerator) : Registry {
        overrides[field] = generator
        return this
    }

    fun getGenerator(table: String, field: String) : DataGenerator {
        val tmp = Field(table, field)
        if(tmp in overrides)
            return overrides[tmp]!!
        return defaults[tmp]!!
    }

}