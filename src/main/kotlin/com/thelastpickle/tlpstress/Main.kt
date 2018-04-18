package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.profiles.BasicTimeSeries
import com.thelastpickle.tlpstress.profiles.IStressProfile
import org.reflections.Reflections

class MainArguments {
    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i"], description = "Number of operations")
    var iterations = 1000
}

fun main(argv: Array<String>) {
    println("Starting up")

    // hard coded for now
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()

    StressContext(session)

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

    for(m in modules) {
//        Class.forName(m.canonicalName)
        val args = m.getConstructor().newInstance().getArguments()
        // name
        jcommander.addCommand(m.simpleName, args)
        commands[m.simpleName] = m

    }

    val jc = jcommander.build()
    jc.parse(*argv)

    // hopefully at this point we have a valid stress profile to run

    val profile = commands[jc.parsedCommand]!!.getConstructor().newInstance()
    val runner = ProfileRunner.create(session, 1, profile)

    runner.execute()

    session.cluster.close()
}


