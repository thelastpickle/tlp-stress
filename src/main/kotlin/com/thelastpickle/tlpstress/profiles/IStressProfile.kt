package com.thelastpickle.tlpstress.profiles

import com.datastax.driver.core.Session
import com.datastax.driver.core.BoundStatement
import com.thelastpickle.tlpstress.StressContext
import com.thelastpickle.tlpstress.generators.DataGenerator
import com.thelastpickle.tlpstress.generators.Field
import com.thelastpickle.tlpstress.samplers.ISampler
import com.thelastpickle.tlpstress.samplers.NoOpSampler

interface IStressRunner {
    fun getNextMutation(partitionKey: String) : Operation
    fun getNextSelect(partitionKey: String) : Operation
}

/**
 * Stress profile interface.  A stress profile defines the schema, prepared
 * statements, and queries that will be executed.  It should be fairly trivial
 * to imp
 */
interface IStressProfile {

    /**
     * jcommander arguments that will be auto added to the commander subcommand.
     * This lets us drop in arbitrary classes, annotate them with Parameters,
     * and they just magically show up in the CLI tool.
     */
    fun getArguments() : Any

    /**
     * Handles any prepared statements that are needed
     * the class should track all prepared statements internally
     * and pass them on to the Runner
     */
    fun prepare(session: Session)
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
    fun getFieldGenerators() : Map<Field, DataGenerator> = mapOf()

    /**
     * returns an instance of ISampler.
     */
    fun getSampler(session: Session, sampleRate: Double) : ISampler { return NoOpSampler() }



}


sealed class Operation {
    // we're going to track metrics on the mutations differently
    // inserts will also carry data that might be saved for later validation
    // TODO needs to be updated to hold full primary key
    // clustering keys won't be realistic to compute in the framework
    data class Mutation(val bound: BoundStatement,
                        val partitionKey: Any,
                        val fields: Map<String, Any>) : Operation()

    data class SelectStatement(var bound: BoundStatement): Operation()
    // JMX commands


}