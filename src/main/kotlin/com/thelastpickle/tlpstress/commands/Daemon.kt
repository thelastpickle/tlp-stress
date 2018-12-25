package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.StressProtoServer
import com.thelastpickle.tlpstress.StressServerGrpc

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

@Parameters(commandDescription = "Run in daemon mode.  Submit jobs with tlp-stress submit")
class Daemon : IStressCommand {

    override fun execute() {
        val port = 5001

        println("Starting daemon mode.")
        val daemonServer = DaemonServer()
        println("Hi")
        val server = ServerBuilder.forPort(port)
                .addService(daemonServer)
                .build()
                .start()

        server.awaitTermination()
        println("Terminated")
    }



    internal class DaemonServer : StressServerGrpc.StressServerImplBase() {
        init {
            println("Starting up daemon")
        }

        override fun runWorkload(request: StressProtoServer.RunRequest?, responseObserver: StreamObserver<StressProtoServer.RunReply>?) {
            if(request == null || responseObserver == null)
                return

        }
    }
}