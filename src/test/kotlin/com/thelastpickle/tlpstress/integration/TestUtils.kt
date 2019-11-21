package com.thelastpickle.tlpstress.integration

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

class TestUtils(val session: Session) {

    val tables = mutableListOf<String>()

    companion object {

        private val ip = System.getenv("TLP_STRESS_CASSANDRA_IP") ?: "127.0.0.1"

        fun connect(keyspace: String = "tlp_stress_test", dropExisting: Boolean = true) : TestUtils {
            val session = Cluster.builder()
                    .addContactPoint(ip)
                    .build().connect()

            if(dropExisting) {
                session.execute("DROP KEYSPACE IF EXISTS $keyspace")
            }

            session.execute("USE $keyspace")
            return TestUtils(session)
        }
    }

    fun createTable(name: String) {
        tables.add(name)
    }

    /**
     * Drop all tables created in the TestUtils instance
     */
    fun dropAll() {
        for(t in tables) {
            session.execute("DROP TABLE $t")
        }
    }


}

