package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.profiles.IStressProfile


class CommandLineParser(val mainArgs: MainArguments,
                        val parsedCommand: String,
                        val plugins: Map<String, Plugin>) {

    companion object {
        fun parse(arguments: Array<String>): CommandLineParser {

            // JCommander set up
            val jcommander = JCommander.newBuilder()
            val mainArgs = MainArguments()
            jcommander.addObject(mainArgs)

            val plugins = Plugin.getPlugins()
            for(plugin in plugins.values) {
                jcommander.addCommand(plugin.name, plugin.arguments)
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
            return CommandLineParser(mainArgs, jc.parsedCommand, plugins)
        }
    }

    fun getParsedPlugin() : Plugin? {
        return plugins[parsedCommand]
    }

    fun getClassInstance() : IStressProfile? {
        return getParsedPlugin()?.cls?.getConstructor()?.newInstance()
    }
}

@Parameters(commandDescription = "tlp-stress")
class MainArguments {
    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i"], description = "Number of operations to run.")
    var iterations : Long = 1000

    @Parameter(names = ["-h", "--help"], description = "Show this help", help = true)
    var help = false

    @Parameter(names = ["--replication"], description = "Replication options")
    var replication = "{'class': 'SimpleStrategy', 'replication_factor':3 }"

    @Parameter(names = ["--host"], description = "Cassandra host for first contact point.")
    var host = "127.0.0.1"

    @Parameter(names = ["--compaction"], description = "Compaction option to use.  Double quotes will auto convert to single for convenience.")
    var compaction = ""

    @Parameter(names = ["--compression"], description = "Compression options")
    var compression = ""

    @Parameter(names = ["--keyspace"], description = "Keyspace to use")
    var keyspace = "tlp_stress"

    @Parameter(names = ["--id"], description = "Identifier for this run, will be used in partition keys.  Make unique for when starting concurrent runners.")
    var id = "001"

    @Parameter(names = ["--partitions", "-p"], description = "Max value of integer component of first partition key.")
    var partitionValues = 1000000

    @Parameter(names = ["--sample", "-s"], description = "Sample Rate (0-1)")
    var sampleRate = 0.001 // .1%..  this might be better as a number, like a million.  reasonable to keep in memory

    @Parameter(names = ["--readrate", "--reads", "-r"], description = "Read Rate, 0-1.  Workloads may have their own defaults.  Default is 0.01, or 1%")
    var readRate = 0.01

    @Parameter(names = ["--concurrency", "-c"], description = "Concurrent queries allowed.  Increase for larger clusters.")
    var concurrency = 250

    @Parameter(names = ["--populate"], description = "Pre-population the DB")
    var populate = false

}