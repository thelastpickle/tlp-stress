package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.thelastpickle.tlpstress.profiles.IStressProfile
import org.reflections.Reflections


class CommandLineParser {
    companion object {
        fun create(arguments: Array<String>): CommandLineParser {

            // JCommander set up
            val jcommander = JCommander.newBuilder()
            val mainArgs = MainArguments()
            jcommander.addObject(mainArgs)

            // get all the objects that implement IStressProfile
            // add each of them to jcommander as an object
            // each will show up as a subcommand
            // this will let us have people drop a jar into a directory
            // this makes it easy to write plugins and not be concerned with the underlying machinery
            val r = Reflections()
            val modules = r.getSubTypesOf(IStressProfile::class.java)

            var commands = mutableMapOf<String, Class<out IStressProfile>>()
            var argMap = mutableMapOf<String, Any>()

            for(m in modules) {
                val args = m.getConstructor().newInstance().getArguments()
                // name
                jcommander.addCommand(m.simpleName, args)
                commands[m.simpleName] = m
                argMap[m.simpleName] = args

            }

            val jc = jcommander.build()
            jc.parse(*arguments)

            if (mainArgs.help || jc.parsedCommand == null) {
                if (jc.parsedCommand == null) {
                    println("Please provide a workload.")
                }
                jc.usage()
                System.exit(0)
            }
            return CommandLineParser()
        }


    }
}