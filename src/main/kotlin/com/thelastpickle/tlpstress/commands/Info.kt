package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.thelastpickle.tlpstress.Plugin

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

    }
}