package com.thelastpickle.tlpstress.generators.functions

import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Function
import java.util.concurrent.ThreadLocalRandom

@Function(name="book",
        description = "Picks random sections of books.")
class Book : FieldGenerator {

    var min: Int = 20
    var max: Int = 50

    override fun setParameters(params: List<String>) {
        min = params[0].toInt()
        max = params[1].toInt()
    }


    override fun getDescription() = """
        Uses random sections of open books to provide real world text data.
    """.trimIndent()

    companion object {
        fun create(min: Int, max: Int) : Book {
            val b = Book()
            b.setParameters(arrayListOf(min.toString(), max.toString()))
            return b
        }
    }

    // all the content from books will go here
    val content = mutableListOf<String>()

    init {

        val files = listOf("alice.txt", "moby-dick.txt", "war.txt")
        for(f in files) {
            val tmp = this::class.java.getResource("/books/$f").readText()
            val splitContent = tmp.split("\\s+".toRegex())
            content.addAll(splitContent)
        }
    }

    override fun getText(): String {
        // first get the length
        val length = ThreadLocalRandom.current().nextInt(min, max)
        val start = ThreadLocalRandom.current().nextInt(0, content.size-length)

        return content.subList(start, start + length).joinToString(" ")
    }
}