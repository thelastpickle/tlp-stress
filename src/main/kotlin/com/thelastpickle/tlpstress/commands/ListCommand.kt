package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.Plugin

@Parameters(commandDescription = "ListCommand all workloads.")
class ListCommand : IStressCommand {
    override fun execute() {

        println("Available Workloads:\n")

        val plugins = Plugin.getPlugins()
        for((key, _) in plugins) {
            println("$key ")
        }
        println("\nDone.")

    }


}