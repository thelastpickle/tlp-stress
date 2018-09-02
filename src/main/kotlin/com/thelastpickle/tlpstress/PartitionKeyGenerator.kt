package com.thelastpickle.tlpstress

import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.experimental.buildSequence

/**
 * Accepts a function that generates numbers.
 * returns a partition key using a prefix
 * for any normal case there should be a compaction object function
 * that should create a generator that has a function with all the logic inside it
 */

class PartitionKeyGenerator(
        val genFunc: (max: Long) -> Long,
        val prefix: String) {
    /**
     *
     */
    companion object {
        fun random(prefix: String = "test") : PartitionKeyGenerator {
            return PartitionKeyGenerator({max -> ThreadLocalRandom.current().nextLong(1, max) }, prefix)
        }
        fun sequence(prefix: String = "test") : PartitionKeyGenerator {
            var current = 0L
            return PartitionKeyGenerator({max -> current++ }, prefix)
        }
    }


    fun generateKey(total: Long, maxId: Long = 100000) = buildSequence {
        var i : Long = 0
        while(true) {
            val tmp = genFunc(maxId)
//            val result = prefix + tmp.toString()
            val result = PartitionKey(prefix, tmp)
            yield(result)
            i++

            if(i == total)
                break
        }
    }

}