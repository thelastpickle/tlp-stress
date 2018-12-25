package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.StressProtoServer
import com.thelastpickle.tlpstress.StressServerGrpc

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

@Parameters(commandDescription = "Run in daemon mode.  Submit jobs with tlp-stress submit")
class Daemon : IStressCommand {

    override fun execute() {
        val port = 5000

        ServerBuilder.forPort(port)
                .addService(DaemonServer())
                .build()
                .start()

    }



    internal class DaemonServer : StressServerGrpc.StressServerImplBase() {
        override fun runWorkload(request: StressProtoServer.RunRequest?, responseObserver: StreamObserver<StressProtoServer.RunReply>?) {
            if(request == null || responseObserver == null)
                return

        }
    }
}