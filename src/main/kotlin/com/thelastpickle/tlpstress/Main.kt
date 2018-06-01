package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.profiles.IStressProfile
import mu.KotlinLogging
import org.reflections.Reflections
import ch.qos.logback.classic.util.ContextInitializer;
import java.util.concurrent.Semaphore

fun main(argv: Array<String>) {

    println("Starting up")
    println(argv)

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml")

    val logger = KotlinLogging.logger {}


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
    jc.parse(*argv)

    if (mainArgs.help || jc.parsedCommand == null) {
        if (jc.parsedCommand == null) {
            println("Please provide a workload.")
        }
        jc.usage()
        System.exit(0)
    }

    try {


        // we're going to build one session per thread for now
        val cluster = Cluster.builder().addContactPoint(mainArgs.host).build()

        // set up the keyspace
        val profile = commands[jc.parsedCommand]!!.getConstructor().newInstance()

        val commandArgs = argMap[jc.parsedCommand]!!

        // get all the initial schema
        println("Creating schema")

        val session = cluster.connect()

        val createKeyspace = """CREATE KEYSPACE IF NOT EXISTS ${mainArgs.keyspace}
                        | WITH replication =
                        | {'class': 'SimpleStrategy',
                        | 'replication_factor':3} """.trimMargin()

        logger.debug { createKeyspace }
        session.execute(createKeyspace)

        session.execute("USE ${mainArgs.keyspace}")

        for (statement in profile.schema()) {
            val s = SchemaBuilder.create(statement)
                    .withCompaction(mainArgs.compaction)
                    .withCompression(mainArgs.compression)
                    .build()
            println(s)
            session.execute(s)
        }

        profile.prepare(session)


        val metrics = Metrics()

        val permits = 250
        var sem = Semaphore(permits)

        // run the prepare for each
        val runners = IntRange(0, mainArgs.threads - 1).map {
            println("Connecting")
            println("Connected")
            val context = StressContext(session, mainArgs, commandArgs, it, metrics, sem, permits)
            ProfileRunner.create(context, profile)
        }

        val executed = runners.parallelStream().map {
            println("Preparing")
            it.prepare()
        }.count()

        println("$executed threads prepared.")

        metrics.startReporting()

        val runnersExecuted = runners.parallelStream().map {
            println("Running")
            it.run()
        }.count()

        // hopefully at this point we have a valid stress profile to run
        println("Stress complete, $runnersExecuted.")

        Thread.sleep(1000)

        // dump out metrics
        metrics.reporter.report()
    } catch (e: Exception) {
        println(e)
    } finally {
        System.exit(0)
    }

}

