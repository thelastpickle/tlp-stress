package com.thelastpickle.tlpstress

import com.datastax.driver.core.Cluster
import mu.KotlinLogging
import ch.qos.logback.classic.util.ContextInitializer
import java.util.concurrent.Semaphore

fun main(argv: Array<String>) {

    println("Starting up")

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml")

    val logger = KotlinLogging.logger {}

    val parser = CommandLineParser.parse(argv)

    try {

        val mainArgs = parser.mainArgs

        // we're going to build one session per thread for now
        val cluster = Cluster.builder().addContactPoint(mainArgs.host).build()

        // set up the keyspace
        val profile = parser.getClassInstance()!!
        val commandArgs = parser.getParsedPlugin()!!.arguments

//        val commandArgs = argMap[jc.parsedCommand]!!

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

