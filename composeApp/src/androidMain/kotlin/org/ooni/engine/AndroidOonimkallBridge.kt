package org.ooni.engine

import oonimkall.HTTPRequest
import oonimkall.Logger
import oonimkall.Oonimkall
import oonimkall.SessionConfig

class AndroidOonimkallBridge : OonimkallBridge {
    override fun startTask(settingsSerialized: String): OonimkallBridge.Task {
        val task = Oonimkall.startTask(settingsSerialized)
        return object : OonimkallBridge.Task {
            override fun interrupt() {
                task.interrupt()
            }

            override fun isDone(): Boolean = task.isDone

            override fun waitForNextEvent() = task.waitForNextEvent()
        }
    }

    override fun newSession(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session {
        val session = Oonimkall.newSession(sessionConfig.toMk())

        return object : OonimkallBridge.Session {
            override fun submitMeasurement(measurement: String): OonimkallBridge.SubmitMeasurementResults {
                val context = session.newContextWithTimeout(CONTEXT_TIMEOUT)
                val results = session.submit(context, measurement)
                return OonimkallBridge.SubmitMeasurementResults(
                    updatedMeasurement = results.updatedMeasurement,
                    updatedReportId = results.updatedReportID,
                    measurementUid = results.measurementUID,
                )
            }

            override fun httpDo(request: OonimkallBridge.HTTPRequest): OonimkallBridge.HTTPResponse {
                val context = session.newContextWithTimeout(CONTEXT_TIMEOUT)
                val response = session.httpDo(context, request.toMk())
                return OonimkallBridge.HTTPResponse(body = response.body)
            }

            override fun close() {
                session.close()
            }
        }
    }

    private fun OonimkallBridge.SessionConfig.toMk() =
        SessionConfig().also {
            it.softwareName = softwareName
            it.softwareVersion = softwareVersion

            it.assetsDir = assetsDir
            // geoipDB may not exist in Android binding; set reflectively if available
            geoIpDB?.let { path ->
                it.geoipDB = path
            }
            it.stateDir = stateDir
            it.tempDir = tempDir
            it.tunnelDir = tunnelDir

            it.probeServicesURL = probeServicesURL
            it.proxy = proxy

            it.logger =
                logger?.let { logger ->
                    object : Logger {
                        override fun debug(msg: String?) = logger.debug(msg)

                        override fun info(msg: String?) = logger.info(msg)

                        override fun warn(msg: String?) = logger.warn(msg)
                    }
                }
            it.verbose = verbose
        }

    private fun OonimkallBridge.HTTPRequest.toMk() =
        HTTPRequest().also {
            it.method = method
            it.url = url
        }

    companion object {
        private const val CONTEXT_TIMEOUT = -1L
    }
}
