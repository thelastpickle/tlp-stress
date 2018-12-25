package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpstress.StressProtoServer


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


    }
}