package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.datastax.driver.core.Cluster
import com.thelastpickle.tlpstress.*
import mu.KotlinLogging
import java.util.concurrent.Semaphore

@Parameters(commandDescription = "Run a tlp-stress profile")
class Run {

    val logger = KotlinLogging.logger {}

    @Parameter(names = ["--host"])
    var host = "127.0.0.1"

    @Parameter(required = true)
    var profile = ""

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

    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i"], description = "Number of operations to run.")
    var iterations : Long = 1000

    @Parameter(names = ["-h", "--help"], description = "Show this help", help = true)
    var help = false

    @Parameter(names = ["--replication"], description = "Replication options")
    var replication = "{'class': 'SimpleStrategy', 'replication_factor':3 }"

    
    fun execute() {

        // we're going to build one session per thread for now
        val cluster = Cluster.builder().addContactPoint(host).build()

        // set up the keyspace
//        val commandArgs = parser.getParsedPlugin()!!.arguments

        // get all the initial schema
        println("Creating schema")

        val session = cluster.connect()

        val createKeyspace = """CREATE KEYSPACE IF NOT EXISTS $keyspace
                        | WITH replication =
                        | {'class': 'SimpleStrategy',
                        | 'replication_factor':3} """.trimMargin()

        logger.debug { createKeyspace }
        session.execute(createKeyspace)

        session.execute("USE $keyspace")

        val plugin = Plugin.getPlugins().get(profile)!!

        for (statement in plugin.instance.schema()) {
            val s = SchemaBuilder.create(statement)
                    .withCompaction(compaction)
                    .withCompression(compression)
                    .build()
            println(s)
            session.execute(s)
        }

        plugin.instance.prepare(session)


        val metrics = Metrics()

        val permits = 250
        var sem = Semaphore(permits)

        // run the prepare for each
        val runners = IntRange(0, threads - 1).map {
            println("Connecting")
            println("Connected")
            val context = StressContext(session, this, it, metrics, sem, permits)
            ProfileRunner.create(context, plugin.instance)
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
    }
}