package com.thelastpickle.tlpstress.prepopulate

import com.thelastpickle.tlpstress.Metrics
import com.thelastpickle.tlpstress.Plugin
import com.thelastpickle.tlpstress.ProfileRunner
import org.apache.cassandra.utils.AsymmetricOrdering

sealed class Option {

    abstract fun execute()

    class Standard : Option() {
        override fun execute() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    class Custom : Option() {
        override fun execute() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}