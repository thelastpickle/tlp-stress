package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.StressProtoServer
import com.thelastpickle.tlpstress.StressServerGrpc
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.CountDownLatch


@Parameters(commandDescription = "Submit a job to workers running in daemon mode.")
class Submit : IStressCommand {

    @Parameter(names = ["--hosts", "-h"])
    var hosts : kotlin.collections.List<String> = listOf()

    @Parameter(names = ["--port", "-p"])
    var port = 5001

    override fun execute() {
        println("Submitting job to workers at $hosts")

        val req = StressProtoServer.RunRequest.newBuilder()
                .setArguments("test")
                .build()

        // just do one host
        var remaining = CountDownLatch(hosts.size)
        for(host in hosts) {
            val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

            StressServerGrpc.newStub(channel).runWorkload(req, object : StreamObserver<StressProtoServer.RunStatus> {
                override fun onNext(value: StressProtoServer.RunStatus?) {
                    println("Got a status")
                }

                override fun onError(t: Throwable?) {
                    println("Error")
                }

                override fun onCompleted() {
                    println("Done")
                    remaining.countDown()
                }

            })
        }
        remaining.await()
        // wait for hosts to complete
    }
}