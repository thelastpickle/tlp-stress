package com.thelastpickle.tlpstress

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.profiles.IStressProfile
import mu.KotlinLogging
import org.reflections.Reflections
import kotlin.concurrent.thread
import ch.qos.logback.classic.util.ContextInitializer;


class MainArguments {
    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i"], description = "Number of operations to run.")
    var iterations = 1000

    @Parameter(names = ["-h", "--help"], description = "Show this help", help = true)
    var help = false

    @Parameter(names = ["--replication"], description = "Replication options")
    var replication = "{'class': 'SimpleStrategy', 'replication_factor':3"

    @Parameter(names = ["--host", "--hosts"], description = "Cassandra hosts, comma separated.  Used as contact points.")
    var contactPoints = "127.0.0.1"

    @Parameter(names = ["--compaction"], description = "Compaction option to use")
    var compaction = ""

    @Parameter(names = ["--keyspace"], description = "Keyspace to use")
    var keyspace = "tlp-stress"

    @Parameter(names = ["--id"], description = "Identifier for this run, will be used in partition keys.  Make unique for when starting concurrent runners.")
    var id = "001"

}

fun main(argv: Array<String>) {

    println("Starting up")

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

    for(m in modules) {
//        Class.forName(m.canonicalName)
        val args = m.getConstructor().newInstance().getArguments()
        // name
        jcommander.addCommand(m.simpleName, args)
        commands[m.simpleName] = m

    }

    val jc = jcommander.build()
    jc.parse(*argv)

    if (mainArgs.help) {
        jc.usage()
        System.exit(0)
    }


    // we're going to build one session per thread for now
    // will be configurable because why not
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    // set up the keyspace

    val profile = commands[jc.parsedCommand]!!.getConstructor().newInstance()
    // get all the initial schema
    println("Creating schema")

    val session = cluster.connect()

    session.execute("""CREATE KEYSPACE ${mainArgs.keyspace}
                        |WITH replication =
                        |{'class': 'SimpleStrategy',
                        |'replication_factor':3})
        |""".trimMargin())

    session.execute("use ${mainArgs.keyspace}")

    for(statement in profile.schema()) {
        session.execute(statement)
    }

    // run the prepare for each
    IntRange(0, mainArgs.threads).map {
        val session = cluster.connect()
        val context = StressContext(session, mainArgs, it)
        ProfileRunner.create(context, profile)
    }.parallelStream().map {
        it.prepare()
        it
    }.parallel().map {
        it.run()
        it
    }


    var threads = mutableListOf<Thread>()

    for(threadId in 1..mainArgs.threads) {
        val t = thread(start = true) {
            logger.info { "Starting thread $threadId" }


        }
        threads.add(t)
    }

    // should move this into the profile runner




    logger.info{"${mainArgs.threads} threads created, waiting to join"}
    println("{$mainArgs.threads} threads created, waiting to join")
    threads.forEach { it.join() }
    println("All threads complete")

    // hopefully at this point we have a valid stress profile to run

    logger.info { "Stress complete." }
}


