package org.ooni.probe.cli

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val signals = CliSignals().apply { install() }
    val code = OoniprobeCli(
        runtime = CliRuntime.default(),
        coreGatewayFactory = ProductionCliCoreGatewayFactory,
        signals = signals,
    ).run(
        args = args,
        stdout = ::println,
        stderr = System.err::println,
    )
    exitProcess(code)
}
