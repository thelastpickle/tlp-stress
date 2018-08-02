package com.thelastpickle.tlpstress.generators

import java.util.concurrent.ThreadLocalRandom

class LastName(args: ArrayList<String> = arrayListOf()) : DataGenerator {

    val names = mutableListOf<String>()

    init {

        val tmp = this::class.java.getResource("/names/last.txt")
                    .readText()
                    .split("\n")
                    .map { it.split(" ").first() }
        names.addAll(tmp)
    }

    override fun getText(): String {
        val element = ThreadLocalRandom.current().nextInt(0, names.size)
        return names[element]
    }

}