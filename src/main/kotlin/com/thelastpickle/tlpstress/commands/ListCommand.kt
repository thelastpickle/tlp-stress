package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.Plugin

@Parameters(commandDescription = "List all workloads.")
class ListCommand : IStressCommand {
    override fun execute() {

        println("Available Workloads:\n")

        val plugins = Plugin.getPlugins()
        for((key, _) in plugins) {
            println("$key ")
        }
        println("\nYou can run any of these workloads by running tlp-cluster run WORKLOAD.")

    }


}