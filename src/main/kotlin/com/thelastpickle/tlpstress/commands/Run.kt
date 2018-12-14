package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.QueryOptions
import com.google.common.util.concurrent.RateLimiter
import com.thelastpickle.tlpstress.*
import com.thelastpickle.tlpstress.converters.ConsistencyLevelConverter
import com.thelastpickle.tlpstress.converters.HumanReadableConverter
import com.thelastpickle.tlpstress.generators.Registry
import java.util.concurrent.Semaphore

@Parameters(commandDescription = "Run a tlp-stress profile")
class Run : IStressCommand {

//    val logger = KotlinLogging.logger {}

    @Parameter(names = ["--host"])
    var host = "127.0.0.1"

    @Parameter(names = ["--username", "-U"])
    var username = "cassandra"

    @Parameter(names = ["--password", "-P"])
    var password = "cassandra"

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

    @Parameter(names = ["--partitions", "-p"], description = "Max value of integer component of first partition key.", converter = HumanReadableConverter::class)
    var partitionValues = 1000000L

//    @Parameter(names = ["--sample", "-s"], description = "Sample Rate (0-1)")
//    var sampleRate : Double? = null // .1%..  this might be better as a number, like a million.  reasonable to keep in memory

    @Parameter(names = ["--readrate", "--reads", "-r"], description = "Read Rate, 0-1.  Workloads may have their own defaults.  Default is dependent on workload.")
    var readRate : Double? = null

    @Parameter(names = ["--concurrency", "-c"], description = "Concurrent queries allowed.  Increase for larger clusters.", converter = HumanReadableConverter::class)
    var concurrency = 250

    @Parameter(names = ["--populate"], description = "Pre-population the DB")
    var populate = false

    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i", "-n"], description = "Number of operations to run.", converter = HumanReadableConverter::class)
    var iterations : Long = 1000

    @Parameter(names = ["-h", "--help"], description = "Show this help", help = true)
    var help = false

    @Parameter(names = ["--replication"], description = "Replication options")
    var replication = "{'class': 'SimpleStrategy', 'replication_factor':3 }"

    @DynamicParameter(names = ["--field."], description = "Override a field's data generator")
    var fields = mutableMapOf<String, String>()

    @Parameter(names = ["--rate"], description = "Rate limiter, accepts human numbers. 0 = disabled", converter = HumanReadableConverter::class)
    var rate = 0L

    @Parameter(names = ["--drop"], description = "Drop the keyspace before starting.")
    var dropKeyspace = false

    @Parameter(names = ["--cl"], description = "Consistency level for reads/writes (Defaults to LOCAL_ONE).", converter = ConsistencyLevelConverter::class)
    var consistencyLevel = ConsistencyLevel.LOCAL_ONE

    
    override fun execute() {

        // we're going to build one session per thread for now
        val cluster = Cluster.builder()
                .addContactPoint(host)
                .withCredentials(username, password)
                .withQueryOptions(QueryOptions().setConsistencyLevel(consistencyLevel))
                .build()

        // set up the keyspace
//        val commandArgs = parser.getParsedPlugin()!!.arguments

        // get all the initial schema
        println("Creating schema")

        println("Executing $iterations operations with consistency level ${consistencyLevel.toString()}")

        val session = cluster.connect()

        println("Connected")

        if(dropKeyspace) {
            println("Dropping $keyspace")
            session.execute("DROP KEYSPACE IF EXISTS $keyspace")
        }

        val createKeyspace = """CREATE KEYSPACE
            | IF NOT EXISTS $keyspace
            | WITH replication = $replication""".trimMargin()

        println("Creating $keyspace: \n$createKeyspace\n")
        session.execute(createKeyspace)

        session.execute("USE $keyspace")

        val plugin = Plugin.getPlugins().get(profile)!!

        // used for the DataGenerator

        /*
        Here we add the compaction and compression options.  in the future we'll be able to do stuff like
        compression.mytable.chunk_length_in_kb=4
        compaction.mytable.class=TimeWindowCompactionStrategy

        ideally we should have shortcuts

        compaction.mytable.class=twcs
         */

        val rateLimiter = if(rate > 0) {
            RateLimiter.create(rate.toDouble())
        } else null

        println("Creating Tables")
        for (statement in plugin.instance.schema()) {
            val s = SchemaBuilder.create(statement)
                    .withCompaction(compaction)
                    .withCompression(compression)
                    .build()
            println(s)
            session.execute(s)
        }

        val fieldRegistry = Registry.create()

        for((field,generator) in plugin.instance.getFieldGenerators()) {
            fieldRegistry.setDefault(field, generator)
        }

        for((field, generator) in fields) {
            println("$field, $generator")
            val instance = Registry.getInstance(generator)
            val parts = field.split(".")
            // TODO check to make sure the fields exist
            fieldRegistry.setOverride(parts[0], parts[1], instance)
        }

        println("Preparing queries")
        plugin.instance.prepare(session)

        println("Initializing metrics")
        val metrics = Metrics()

        val permits = concurrency
        var sem = Semaphore(permits)

        // run the prepare for each
        val runners = IntRange(0, threads - 1).map {
            println("Connecting")
            val context = StressContext(session, this, it, metrics, sem, permits, fieldRegistry, rateLimiter, consistencyLevel)
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