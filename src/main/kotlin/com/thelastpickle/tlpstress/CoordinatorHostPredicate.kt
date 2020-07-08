package com.thelastpickle.tlpstress

import com.datastax.oss.driver.api.core.metadata.Node
import com.google.common.base.Predicate

class CoordinatorHostPredicate : Predicate<Node> {
    override fun apply(input: Node?): Boolean {
        if(input == null)
            return false
        return true
    }
}

