package com.thelastpickle.tlpstress.generators

import java.util.concurrent.ThreadLocalRandom

class FirstName(args: ArrayList<String> = arrayListOf()) : DataGenerator {

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