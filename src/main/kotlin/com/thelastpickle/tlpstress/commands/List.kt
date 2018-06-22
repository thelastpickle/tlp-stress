package com.thelastpickle.tlpstress.commands

import com.thelastpickle.tlpstress.Plugin

class List : IStressCommand {
    override fun execute() {

        println("Available Workloads:\n")

        val plugins = Plugin.getPlugins()
        for((key, _) in plugins) {
            println("$key ")
        }
        println("\nDone.")

    }


}