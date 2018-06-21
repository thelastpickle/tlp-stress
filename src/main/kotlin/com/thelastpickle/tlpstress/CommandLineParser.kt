package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.thelastpickle.tlpstress.commands.IStressCommand
import com.thelastpickle.tlpstress.commands.Info
import com.thelastpickle.tlpstress.commands.Run


class CommandLineParser(val jCommander: JCommander,
                        val commands: Map<String, IStressCommand>) {

    companion object {
        fun parse(arguments: Array<String>): CommandLineParser {

            // JCommander set up
            val jcommander = JCommander.newBuilder()

            // subcommands
            val commands = mapOf(
                    "run" to Run(),
                    "info" to Info(),
                    "list" to com.thelastpickle.tlpstress.commands.List())

            for(x in commands.entries) {
                jcommander.addCommand(x.key, x.value)
            }

            val jc = jcommander.build()
            jc.parse(*arguments)

            if (jc.parsedCommand == null) {
                jc.usage()
                System.exit(0)
            }
            return CommandLineParser(jc, commands)
        }
    }

    fun execute() {

    }

    fun getParsedCommand() : String {
        return jCommander.parsedCommand
    }

    fun getCommandInstance() : IStressCommand {
        return commands[getParsedCommand()]!!

    }


}

