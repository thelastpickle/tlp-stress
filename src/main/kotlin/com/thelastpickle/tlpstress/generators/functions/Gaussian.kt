package com.thelastpickle.tlpstress.generators.functions
import com.thelastpickle.tlpstress.generators.Function
import com.thelastpickle.tlpstress.generators.FieldGenerator


@Function(name="gaussian",
        description = "Gaussian (normal) numerical data distribution")
class Gaussian : FieldGenerator {
    var min: Long = 0
    var max: Long = 1000000

    override fun setParameters(params: List<String>) {
        min = params[0].toLong()
        max = params[1].toLong()
    }


    override fun getDescription() = """
        Generates numbers following a gaussian (normal) distribution.  This is useful for simulating certain workloads which use certain values more than others.
    """.trimIndent()


}