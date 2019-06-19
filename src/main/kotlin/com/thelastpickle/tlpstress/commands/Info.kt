package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.Plugin
import com.github.ajalt.mordant.TermColors


@Parameters(commandDescription = "Get details of a specific workload.")
class Info : IStressCommand {
    @Parameter(required = true)
    var profile = ""

    override fun execute() {
        // Description
        // Schema
        // Field options
        val plugin = Plugin.getPlugins().get(profile)!!


        for(cql in plugin.instance.schema()) {
            println(cql)
        }

        println("Default read rate: ${plugin.instance.getDefaultReadRate()} (override with -r)\n")


        val params = plugin.getCustomParams()

        if(params.size > 0) {

            println("Dynamic workload parameters (override with --workload.name=X)\n")
            // TODO: Show dynamic parameters

            val cols = arrayOf(0, 0, 0)
            cols[0] = params.map { it.name.length }.max()?:0 + 1
            cols[1] = params.map { it.description.length }.max() ?: 0 + 1
            cols[2] = params.map { it.type.length }.max() ?: 0 + 1

            with(TermColors()) {
                println("${underline("Name".padEnd(cols[0]))} | ${underline("Description".padEnd(cols[1]))} | ${underline("Type".padEnd(cols[2]))}")
            }

            for(row in params) {
                println("${row.name.padEnd(cols[0])} | ${row.description.padEnd(cols[1])} | ${row.type.padEnd(cols[2])}")
            }
        } else {
            println("No dynamic workload parameters.")
        }


    }
}