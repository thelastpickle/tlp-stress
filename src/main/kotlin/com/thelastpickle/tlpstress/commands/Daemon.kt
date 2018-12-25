package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.StressProtoServer
import com.thelastpickle.tlpstress.StressServerGrpc

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

@Parameters(commandDescription = "Run in daemon mode.  Submit jobs with tlp-stress submit")
class Daemon : IStressCommand {

    @Parameter(description = "Port to listen on")
    var port = 5001

    override fun execute() {
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

        override fun runWorkload(request: StressProtoServer.RunRequest?, responseObserver: StreamObserver<StressProtoServer.RunStatus>?) {
            if(request == null || responseObserver == null)
                return

            println("Received workload, starting $request")



            println("Finished workload")
        }
    }
}