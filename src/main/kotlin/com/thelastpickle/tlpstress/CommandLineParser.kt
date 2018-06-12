package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.commands.Info
import com.thelastpickle.tlpstress.commands.Init
import com.thelastpickle.tlpstress.commands.Run
import com.thelastpickle.tlpstress.profiles.IStressProfile


class CommandLineParser(val mainArgs: MainArguments,
                        val parsedCommand: String) {

    companion object {
        fun parse(arguments: Array<String>): CommandLineParser {

            // JCommander set up
            val jcommander = JCommander.newBuilder()
            val mainArgs = MainArguments()

            // subcommands
            val run = Run()
            val info = Info()
            val list = com.thelastpickle.tlpstress.commands.List()
            val init = Init()

            jcommander.addCommand(run)
            jcommander.addCommand(info)
            jcommander.addCommand(list)
            jcommander.addCommand(init)

            val jc = jcommander.build()
            jc.parse(*arguments)

            if (mainArgs.help || jc.parsedCommand == null) {
                if (jc.parsedCommand == null) {
                    println("Please provide a workload.")
                }
                jc.usage()
                System.exit(0)
            }
            return CommandLineParser(mainArgs, jc.parsedCommand)
        }
    }
}

@Parameters(commandDescription = "tlp-stress")
class MainArguments {

    @Parameter(names = ["--host"], description = "Cassandra host for first contact point.")
    var host = "127.0.0.1"



}