package com.thelastpickle.tlpstress.generators.functions

import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Function
import java.util.concurrent.ThreadLocalRandom

@Function(name="firstname",
        description = "First names.")
class FirstName : FieldGenerator {

    override fun setParameters(params: List<String>) {
        // nothing to do here
    }

    override fun getDescription() = """
        Uses common first names, both male and female.
    """.trimIndent()


    val names = mutableListOf<String>()

    init {

        for (s in arrayListOf("female", "male")) {
            val tmp = this::class.java.getResource("/names/female.txt")
                    .readText()
                    .split("\n")
                    .map { it.split(" ").first() }
            names.addAll(tmp)
        }
    }

    override fun getText(): String {
        val element = ThreadLocalRandom.current().nextInt(0, names.size)
        return names[element]
    }

}