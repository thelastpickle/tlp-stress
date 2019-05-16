package com.thelastpickle.tlpstress.profiles

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.BoundStatement
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.thelastpickle.tlpstress.PartitionKey
import com.thelastpickle.tlpstress.PopulateOption
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.commands.Run
import com.thelastpickle.tlpstress.generators.FieldGenerator
import com.thelastpickle.tlpstress.generators.Field

interface IStressRunner {
    fun getNextMutation(partitionKey: PartitionKey) : Operation
    fun getNextSelect(partitionKey: PartitionKey) : Operation
    /**
     * Callback after a query executes successfully.
     * Will be used for state tracking on things like LWTs as well as provides an avenue for future work
     * doing post-workload correctness checks
     */
    fun onSuccess(op: Operation.Mutation, result: ResultSet?) { }

    fun customPopulateIter() : Iterator<Operation.Mutation> {
        return listOf<Operation.Mutation>().iterator()
    }

}

/**
 * Stress profile interface.  A stress profile defines the schema, prepared
 * statements, and queries that will be executed.  It should be fairly trivial
 * to imp
 */
interface IStressProfile {
    /**
     * Handles any prepared statements that are needed
     * the class should track all prepared statements internally
     * and pass them on to the Runner
     */
    fun prepare(session: CqlSession)
    /**
     * returns a bunch of DDL statements
     * this can be create table, index, materialized view, etc
     * for most tests this is probably a single table
     * it's OK to put a clustering order in, but otherwise the schema
     * should not specify any other options here because they can all
     * but supplied on the command line.
     *
     * I may introduce a means of supplying default values, because
     * there are plenty of use cases where you would want a specific
     * compaction strategy most of the time (like a time series, or a cache)
     */
    fun schema(): List<String>

    /**
     * returns an instance of the stress runner for this particular class
     * This was done to allow a single instance of an IStress profile to be
     * generated, and passed to the ProfileRunner.
     * The issue is that the profile needs to generate a single schema
     * but then needs to create multiple stress runners
     * this allows the code to be a little cleaner
     */
    fun getRunner(context: StressContext): IStressRunner

    /**
     * returns a map of generators cooresponding to the different fields
     * it's required to specify all fields that use a generator
     * some fields don't, like TimeUUID or the first partition key
     * This is optional, but encouraged
     *
     * A profile can technically do whatever it wants, no one is obligated to use the generator
     * Using this does give the flexibility of specifying a different generator however
     * In the case of text fields, this is VERY strongly encouraged to allow for more flexibility with the size
     * of the text payload
     */
    fun getFieldGenerators() : Map<Field, FieldGenerator> = mapOf()

    fun getDefaultReadRate() : Double { return .01 }

    fun getCustomArguments() : Map<String, String> { return mapOf() }

    fun getPopulateOption(args: Run)  : PopulateOption = PopulateOption.Standard()




}


sealed class Operation(val bound: BoundStatement) {
    // we're going to track metrics on the mutations differently
    // inserts will also carry data that might be saved for later validation
    // clustering keys won't be realistic to compute in the framework

    class Mutation(bound: BoundStatement, val callbackPayload: Any? = null ) : Operation(bound)

    class SelectStatement(bound: BoundStatement): Operation(bound)


}