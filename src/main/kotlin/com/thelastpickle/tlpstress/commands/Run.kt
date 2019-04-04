package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.converters.IParameterSplitter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.ScheduledReporter
import com.datastax.driver.core.*
import com.datastax.driver.core.policies.HostFilterPolicy
import com.datastax.driver.core.policies.RoundRobinPolicy
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.RateLimiter
import com.thelastpickle.tlpstress.*
import com.thelastpickle.tlpstress.Metrics
import com.thelastpickle.tlpstress.converters.ConsistencyLevelConverter
import com.thelastpickle.tlpstress.converters.HumanReadableConverter
import com.thelastpickle.tlpstress.converters.HumanReadableTimeConverter
import com.thelastpickle.tlpstress.generators.ParsedFieldFunction
import com.thelastpickle.tlpstress.generators.Registry
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import org.apache.logging.log4j.kotlin.logger
import kotlin.concurrent.fixedRateTimer

class NoSplitter : IParameterSplitter {
    override fun split(value: String?): MutableList<String> {
        return mutableListOf(value!!)
    }

}


@Parameters(commandDescription = "Run a tlp-stress profile")
class Run : IStressCommand {

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
    var concurrency = 100L

    @Parameter(names = ["--populate"], description = "Pre-population the DB with N rows before starting load test.", converter = HumanReadableConverter::class)
    var populate = 0L

    @Parameter(names = ["--threads", "-t"], description = "Threads to run")
    var threads = 1

    @Parameter(names = ["--iterations", "-i", "-n"], description = "Number of operations to run.", converter = HumanReadableConverter::class)
    var iterations : Long = 0
    val DEFAULT_ITERATIONS : Long = 1000000

    @Parameter(names = ["--duration", "-d"], description = "Duration of the stress test.  Expressed in format 1d 3h 15m", converter = HumanReadableTimeConverter::class)
    var duration : Long = 0

    @Parameter(names = ["-h", "--help"], description = "Show this help", help = true)
    var help = false

    @Parameter(names = ["--replication"], description = "Replication options")
    var replication = "{'class': 'SimpleStrategy', 'replication_factor':3 }"

    @DynamicParameter(names = ["--field."], description = "Override a field's data generator.  Example usage: --field.tablename.fieldname='book(100,200)'")
    var fields = mutableMapOf<String, String>()

    @Parameter(names = ["--rate"], description = "Rate limiter, accepts human numbers. 0 = disabled", converter = HumanReadableConverter::class)
    var rate = 0L

    @Parameter(names = ["--drop"], description = "Drop the keyspace before starting.")
    var dropKeyspace = false

    @Parameter(names = ["--cl"], description = "Consistency level for reads/writes (Defaults to LOCAL_ONE).", converter = ConsistencyLevelConverter::class)
    var consistencyLevel = ConsistencyLevel.LOCAL_ONE

    @Parameter(names = ["--cql"], description = "Additional CQL to run after the schema is created.  Use for DDL modifications such as creating indexes.", splitter = NoSplitter::class)
    var additionalCQL = mutableListOf<String>()

    @Parameter(names = ["--partitiongenerator", "--pg"], description = "Method of generating partition keys.  Supports random, normal (gaussian), and sequence.")
    var partitionKeyGenerator: String = "random"

    @Parameter(names = ["--coordinatoronly", "--co"], description = "Coordinator only made.  This will cause tlp-stress to round robin between nodes without tokens.  Requires using -Djoin_ring=false in cassandra-env.sh.  When using this option you must only provide a coordinator to --host.")
    var coordinatorOnlyMode = false

    @Parameter(names = ["--csv"], description = "When this flag is set, the metrics will be written to .csv files")
    var writeToCsv = false

    val log = logger()

    val session by lazy {
            var builder = Cluster.builder()
                    .addContactPoint(host)
                    .withCredentials(username, password)
                    .withQueryOptions(QueryOptions().setConsistencyLevel(consistencyLevel))
                    .withPoolingOptions(PoolingOptions()
                            .setConnectionsPerHost(HostDistance.LOCAL, 4, 8)
                            .setConnectionsPerHost(HostDistance.REMOTE, 4, 8)
                            .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
                            .setMaxRequestsPerConnection(HostDistance.REMOTE, 2000))

            if(coordinatorOnlyMode) {
                println("Using experimental coordinator only mode.")
                val policy = HostFilterPolicy(RoundRobinPolicy(), CoordinatorHostPredicate())
                builder = builder.withLoadBalancingPolicy(policy)
            }
        
            val cluster = builder.build()

            // set up the keyspace
//        val commandArgs = parser.getParsedPlugin()!!.arguments

            // get all the initial schema
            println("Creating schema")

            println("Executing $iterations operations with consistency level $consistencyLevel")

            val session = cluster.connect()

            println("Connected")
            session
        }

    override fun execute() {

        Preconditions.checkArgument(!(duration > 0 && iterations > 0L), "Duration and iterations shouldn't be both set at the same time. Please pick just one.")
        iterations = if (duration == 0L && iterations == 0L) DEFAULT_ITERATIONS else iterations // apply the default if the number of iterations wasn't set


        Preconditions.checkArgument(partitionKeyGenerator in setOf("random", "normal", "sequence"), "Partition generator Supports random, normal, and sequence.")

        createKeyspace()

        val plugin = Plugin.getPlugins().get(profile)!!

        val rateLimiter = getRateLimiter()

        createSchema(plugin)
        executeAdditionalCQL()

        val fieldRegistry = createFieldRegistry(plugin)

        println("Preparing queries")
        plugin.instance.prepare(session)

        val metrics = createMetrics()

        // run the prepare for each
        val runners = createRunners(plugin, metrics, fieldRegistry, rateLimiter)

        populateData(plugin, runners, metrics)

        println("Starting main runner")

        metrics.startReporting()

        val runnersExecuted = runners.parallelStream().map {
            println("Running")
            it.run()
        }.count()

        // hopefully at this point we have a valid stress profile to run

        Thread.sleep(1000)

        // dump out metrics
        for(reporter in metrics.reporters) {
            reporter.report()
        }
        metrics.shutdown()
        Thread.sleep(1000)
        println("Stress complete, $runnersExecuted.")
    }

    

    private fun getRateLimiter() = if(rate > 0) {
            RateLimiter.create(rate.toDouble())
        } else null


    private fun populateData(plugin: Plugin, runners: List<ProfileRunner>, metrics: Metrics) {

        val max = when(val option = plugin.instance.getPopulateOption(this)) {
            is PopulateOption.Standard -> populate
            is PopulateOption.Custom -> option.rows
        } * threads

        log.info { "Prepopulating data with $max records per thread" }

        if(max > 0) {
            ProgressBar("Populate Progress", max, ProgressBarStyle.ASCII).use {
                // update the timer every second, starting 1 second from now, as a daemon thread
                val timer = fixedRateTimer("progress-bar", true, 1000, 1000) {
                    it.stepTo(metrics.populate.count)
                }

                // calling it on the runner
                runners.parallelStream().map {
                    it.populate(populate)
                }.count()

                // have we really reached 100%?
                Thread.sleep(1000)
                // stop outputting the progress bar
                timer.cancel()
                // allow the time to die out
            }
            Thread.sleep(1000)
            println("\nPre-populate complete.")
        }

    }

    private fun createRunners(plugin: Plugin, metrics: Metrics, fieldRegistry: Registry, rateLimiter: RateLimiter?): List<ProfileRunner> {
        val runners = IntRange(0, threads - 1).map {
            println("Connecting")
            val context = StressContext(session, this, it, metrics, concurrency.toInt(), fieldRegistry, rateLimiter)
            ProfileRunner.create(context, plugin.instance)
        }

        val executed = runners.parallelStream().map {
            println("Preparing statements.")
            it.prepare()
        }.count()

        println("$executed threads prepared.")
        return runners
    }

    private fun createMetrics(): Metrics {
        println("Initializing metrics")
        val registry = MetricRegistry()

        val reporters = mutableListOf<ScheduledReporter>()

        if(writeToCsv) {
            reporters.add(FileReporter(registry))
        }
        reporters.add(SingleLineConsoleReporter(registry))

        return Metrics(registry, reporters)
    }

    private fun createFieldRegistry(plugin: Plugin): Registry {

        val fieldRegistry = Registry.create()

        for((field,generator) in plugin.instance.getFieldGenerators()) {
            fieldRegistry.setDefault(field, generator)
        }

        // Fields  is the raw --fields
        for((field, generator) in fields) {
            println("$field, $generator")
            // Parsed field, switch this to use the new parser
            //val instance = Registry.getInstance(generator)
            val instance = ParsedFieldFunction(generator)

            val parts = field.split(".")
            val table = parts[0]
            val fieldName = parts[1]
//            val
            // TODO check to make sure the fields exist
            fieldRegistry.setOverride(table, fieldName, instance)
        }
        return fieldRegistry
    }

    private fun createKeyspace() {

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
    }


    private fun executeAdditionalCQL() {
        // run additional CQL
        for (statement in additionalCQL) {
            println(statement)
            session.execute(statement)
        }
    }

    fun createSchema(plugin: Plugin) {
        println("Creating Tables")
        for (statement in plugin.instance.schema()) {
            val s = SchemaBuilder.create(statement)
                    .withCompaction(compaction)
                    .withCompression(compression)
                    .build()
            println(s)
            session.execute(s)
        }
    }


}