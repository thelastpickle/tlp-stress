package com.thelastpickle.tlpstress

import com.datastax.driver.core.Cluster


class Main {


}


fun main(args: Array<String>) {
    println("Starting up")

    // hard coded for now
    val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
    val session = cluster.connect()
    session.execute("CREATE KEYSPACE IF NOT EXISTS tlp_stress WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};")
    session.execute("use tlp_stress")
    BasicTimeSeries().execute(session)

}


