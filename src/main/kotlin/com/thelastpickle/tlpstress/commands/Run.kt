package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.converters.IParameterSplitter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.ScheduledReporter
import com.datastax.driver.core.*
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy
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
import java.io.File
import kotlin.concurrent.fixedRateTimer

class NoSplitter : IParameterSplitter {
    override fun split(value: String?): MutableList<String> {
        return mutableListOf(value!!)
    }

}


@Parameters(commandDescription = "Run a tlp-stress profile")
class Run(val command: String) : IStressCommand {

    @Parameter(names = ["--host"])
    var host = System.getenv("TLP_STRESS_CASSANDRA_HOST") ?: "127.0.0.1"

    @Parameter(names = ["--port"], description = "Override the cql port. Defaults to 9042.")
    var cqlPort = 9042

    @Parameter(names = ["--username", "-U"])
    var username = "cassandra"

    @Parameter(names = ["--password", "-P"])
    var password = "cassandra"

    @Parameter(required = true)
    var profile = ""

    @Parameter(names = ["--compaction"], description = "Compaction option to use.  Double quotes will auto convert to single for convenience.  A shorthand is also available: stcs, lcs, twcs.  See the full documentation for all possibilities.")
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

    @Parameter(names = ["--csv"], description = "Write metrics to this file in CSV format.")
    var csvFile = ""

    @Parameter(names = ["--paging"], description = "Override the driver's default page size.")
    var paging : Int? = null

    @Parameter(names = ["--rowcache"], description = "Row cache setting")
    var rowCache = "NONE"

    @Parameter(names = ["--keycache"], description = "Key cache setting")
    var keyCache = "ALL"

    @Parameter(names = ["--prometheusport"], description = "Override the default prometheus port.")
    var prometheusPort = 9500

    @Parameter(names = ["--ssl"], description = "Enable SSL")
    var ssl = false

    @Parameter(names = ["--ttl"], description = "Table level TTL, 0 to disable.")
    var ttl : Long = 0

    @Parameter(names = ["--dc"], description = "The data center to which requests should be sent")
    var dc : String = ""

    @DynamicParameter(names = ["--workload.", "-w."], description = "Override workload specific parameters.")
    var workloadParameters: Map<String, String> = mutableMapOf()

    @Parameter(names = ["--no-schema"], description = "Skips schema creation")
    var noSchema: Boolean = false

    @Parameter(names = ["--deleterate", "--deletes"], description = "Deletion Rate, 0-1.  Workloads may have their own defaults.  Default is dependent on workload.")
    var deleteRate : Double? = null
    
    val log = logger()

    /**
     * Lazily generate query options
     */
    val options by lazy {
        val tmp = QueryOptions().setConsistencyLevel(consistencyLevel)
        if(paging != null) {
            println("Using custom paging size of $paging")
            tmp.setFetchSize(paging!!)
        }
        tmp
    }

    val session by lazy {

        var builder = Cluster.builder()
                .addContactPoint(host)
                .withPort(cqlPort)
                .withCredentials(username, password)
                .withQueryOptions(options)
                .withPoolingOptions(PoolingOptions()
                        .setConnectionsPerHost(HostDistance.LOCAL, 4, 8)
                        .setConnectionsPerHost(HostDistance.REMOTE, 4, 8)
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, 2000))
        if(ssl) {
            builder = builder.withSSL()
        }

        if (dc != "") {
            builder.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder()
                    .withLocalDc(dc)
                    .withUsedHostsPerRemoteDc(0)
                    .build())
        }

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

        plugin.applyDynamicSettings(workloadParameters)

        createSchema(plugin)
        executeAdditionalCQL()

        val fieldRegistry = createFieldRegistry(plugin)

        println("Preparing queries")
        plugin.instance.prepare(session)

        // Both of the following are set in the try block, so this is OK
        val metrics = createMetrics()
        var runnersExecuted = 0L

        try {
            // run the prepare for each
            val runners = createRunners(plugin, metrics, fieldRegistry, rateLimiter)

            populateData(plugin, runners, metrics)

            println("Starting main runner")

            metrics.startReporting()

            runnersExecuted = runners.parallelStream().map {
                println("Running")
                it.run()
            }.count()

            // hopefully at this point we have a valid stress profile to run

            Thread.sleep(1000)

            // dump out metrics
            for (reporter in metrics.reporters) {
                reporter.report()
            }
        } catch (e: Exception) {
            println("There was an error with tlp-stress.  Please file a bug at https://github.com/thelastpickle/tlp-stress and report the following exception:\n $e")
            throw e
        } finally {
            // we need to be able to run multiple tests in the same JVM
            // without this cleanup we could have the metrics runner still running and it will cause subsequent tests to fail
            metrics.shutdown()
            Thread.sleep(1000)
            println("Stress complete, $runnersExecuted.")
        }

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

        if(csvFile.isNotEmpty()) {
            reporters.add(FileReporter(registry, csvFile, command))
        }
        reporters.add(SingleLineConsoleReporter(registry))

        return Metrics(registry, reporters, prometheusPort)
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
        if(noSchema) {
            println("Skipping keyspace creation")
        }
        else {
            if (dropKeyspace) {
                println("Dropping $keyspace")
                session.execute("DROP KEYSPACE IF EXISTS $keyspace")
            }

            val createKeyspace = """CREATE KEYSPACE
            | IF NOT EXISTS $keyspace
            | WITH replication = $replication""".trimMargin()

            println("Creating $keyspace: \n$createKeyspace\n")
            session.execute(createKeyspace)
        }
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

        if(noSchema) return

        for (statement in plugin.instance.schema()) {
            val s = SchemaBuilder.create(statement)
                    .withCompaction(compaction)
                    .withCompression(compression)
                    .withRowCache(rowCache)
                    .withKeyCache(keyCache)
                    .withDefaultTTL(ttl)
                    .build()
            println(s)
            session.execute(s)
        }
    }


}