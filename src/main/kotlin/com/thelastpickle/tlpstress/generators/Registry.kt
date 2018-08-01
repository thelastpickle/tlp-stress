package com.thelastpickle.tlpstress.generators


data class Field(val table: String, val field: String)

class Registry(val generators: Map<String, Class<out DataGenerator>> = mutableMapOf(),
               val defaults: Map<Field, DataGenerator> = mapOf()) {

    companion object {
        fun create(defaults: Map<Field, DataGenerator>) : Registry {
            val data = Registry.getGenerators()
            return Registry(data, defaults)
        }

        fun getGenerators() : Map<String, Class<out DataGenerator>> = mapOf("cities" to USCities::class.java,
                                                                            "gaussian" to Gaussian::class.java,
                                                                            "book" to Book::class.java)

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
}