package com.thelastpickle.tlpstress.generators

import java.util.concurrent.ThreadLocalRandom

class Book(val args: ArrayList<String>) : DataGenerator {
    // secondary constructor shorthand
    constructor(min: Int, max: Int) :
        this(arrayListOf(min.toString(), max.toString()))

    constructor() : this(20, 50)

    // all the content from books will go here
    val content = mutableListOf<String>()

    val min: Int
    val max: Int

    init {
        min = args[0].toInt()
        max = args[1].toInt()

        val files = arrayListOf("alice.txt", "moby-dick.txt", "war.txt")
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