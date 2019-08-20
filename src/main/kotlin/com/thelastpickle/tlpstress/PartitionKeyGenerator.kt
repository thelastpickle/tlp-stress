package com.thelastpickle.tlpstress

import java.util.concurrent.ThreadLocalRandom

import org.apache.commons.math3.random.RandomDataGenerator

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
        /**
         *
         */
        fun random(prefix: String = "test") : PartitionKeyGenerator {
            return PartitionKeyGenerator({max -> ThreadLocalRandom.current().nextLong(0, max) }, prefix)
        }

        /**
         *
         */
        fun sequence(prefix: String = "test") : PartitionKeyGenerator {
            var current = 0L
            return PartitionKeyGenerator(
                    {
                        max ->
                        if(current > max)
                            current = 0
                        current++
                    }, prefix)
        }

        /**
         * Gaussian distribution
         */
        fun normal(prefix: String = "test") : PartitionKeyGenerator {
            val generator = RandomDataGenerator()
            return PartitionKeyGenerator({ max ->
                var result = 0L
                while(true) {
                    val mid = (max / 2).toDouble()
                    result = generator.nextGaussian(mid, mid / 4.0).toLong()
                    if(result in 0..max)
                        break
                }
                result
            }, prefix)
        }
    }


    fun generateKey(total: Long, maxId: Long = 100000) = sequence {
        var i : Long = 0
        while(true) {
            val tmp = genFunc(maxId)

            val result = PartitionKey(prefix, tmp)
            yield(result)
            i++

            if(i == total)
                break
        }
    }

}