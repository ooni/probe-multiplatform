import composeApp
import Oonimkall

let CONTEXT_TIMEOUT: Int64 = -1

class IosOonimkallBridge: OonimkallBridge {

    func startTask(settingsSerialized: String) throws -> OonimkallBridgeTask {
        var error: NSError?
        let task = OonimkallStartTask(settingsSerialized, &error)!

        class Task: OonimkallBridgeTask {
            var task: OonimkallTask

            init(task: OonimkallTask) {
                self.task = task
            }

            func isDone() -> Bool {
                task.isDone()
            }

            func interrupt() {
                task.interrupt()
            }

            func waitForNextEvent() -> String {
                task.waitForNextEvent()
            }
        }

        return Task(task: task)
    }

    func doNewSession(sessionConfig: OonimkallBridgeSessionConfig) throws -> OonimkallBridgeSession {
        func error(_ message: String, code: Int = 0, domain: String = "IosOonimkallBridge", function: String = #function, file: String = #file, line: Int = #line) -> NSError {

            let functionKey = "\(domain).function"
            let fileKey = "\(domain).file"
            let lineKey = "\(domain).line"

            let error = NSError(domain: domain, code: code, userInfo: [
                message: message,
                functionKey: function,
                fileKey: file,
                lineKey: line
            ])

            return error
        }

        class IosSession: OonimkallBridgeSession {
            private let session: OonimkallSession

            init(sessionConfig: OonimkallSessionConfig) throws {
                var sessionError: NSError?
                guard let session = OonimkallNewSession(sessionConfig, &sessionError) else {
                    throw error("Unable to create session")
                }
                // throw error if any
                if sessionError != nil {
                    throw sessionError!
                }
                self.session = session
            }

            func httpDo(request: OonimkallBridgeHTTPRequest) throws -> OonimkallBridgeHTTPResponse {
                guard let context = session.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw error("Unable to create context")
                }
                do {
                    let response = try session.httpDo(context, jreq: request.toMk())
                    return OonimkallBridgeHTTPResponse(body: response.body)
                } catch {
                    throw error
                }
            }

            func submitMeasurement(measurement: String) throws -> OonimkallBridgeSubmitMeasurementResults {
                guard let context = session.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw error("Unable to create context")
                }
                do {

                    let result: OonimkallSubmitMeasurementResults? = try session.submit(context, measurement: measurement)
                    return OonimkallBridgeSubmitMeasurementResults(
                        updatedMeasurement: result?.updatedMeasurement,
                        updatedReportId: result?.updatedReportID ?? "",
                        measurementUid: result?.measurementUID
                    )
                } catch {
                    throw error
                }
            }

            func close() throws {
                try session.close()
            }
        }

        return try IosSession(sessionConfig: sessionConfig.toMk())
    }
}


extension OonimkallBridgeSessionConfig {
    func toMk() -> OonimkallSessionConfig {

        let config: OonimkallSessionConfig = OonimkallSessionConfig()
        config.softwareName = softwareName
        config.softwareVersion = softwareVersion
        config.assetsDir = assetsDir
        config.stateDir = stateDir
        config.tempDir = tempDir
        config.tunnelDir = tunnelDir
        if let geoIpDB = geoIpDB {
            config.geoipDB = geoIpDB
        }
        if let probeServicesURL = probeServicesURL {
            config.probeServicesURL = probeServicesURL
        }
        if let proxy = proxy {
            config.proxy = proxy
        }
        // Problem setting logger
        if let logger = logger {
            config.logger = IosLogger(logger: logger)
        }
        config.verbose = verbose
        return config
    }
}

extension OonimkallBridgeHTTPRequest {
    func toMk() -> OonimkallHTTPRequest {
        let request = OonimkallHTTPRequest()
        request.method = method
        request.url = url
        return request
    }
}

@objc
class IosLogger: NSObject, OonimkallLoggerProtocol {
    private let logger: OonimkallBridgeLogger?

    init(logger: OonimkallBridgeLogger) {
        self.logger = logger
        super.init()
    }

    func debug(_ msg: String?) {
        logger?.debug(msg: msg)
    }

    func info(_ msg: String?) {
        logger?.info(msg: msg)
    }

    func warn(_ msg: String?) {
        logger?.warn(msg: msg)
    }
}
