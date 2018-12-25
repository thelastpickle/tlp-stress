package com.thelastpickle.tlpstress.commands

import com.beust.jcommander.Parameters

@Parameters(commandDescription = "Submit a job to workers running in daemon mode.")
class Submit : IStressCommand {
    override fun execute() {
        println("Submitting job to workers.")
    }
}