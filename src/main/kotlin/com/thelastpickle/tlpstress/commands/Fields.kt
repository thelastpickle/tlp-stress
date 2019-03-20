package com.thelastpickle.tlpstress.commands

import com.thelastpickle.tlpstress.generators.Registry


class Fields : IStressCommand {
    override fun execute() {
        // show each generator
        val registry = Registry.create()

        for(func in registry.getFunctions()) {
            println("Generator: ${func.name}")

            print("Description:")
            println(func.description)

        }

    }
}