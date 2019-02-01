package com.thelastpickle.tlpstress

import com.datastax.driver.core.Host
import com.google.common.base.Predicate

class CoordinatorHostPredicate : Predicate<Host> {
    override fun apply(input: Host?): Boolean {
        if(input == null)
            return false
        return input.tokens == null || input.tokens.size == 0
    }
}

