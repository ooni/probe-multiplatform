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

            func checkIn(config: OonimkallBridgeCheckInConfig) throws -> OonimkallBridgeCheckInResults {
                guard let context = session.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw error("Unable to create context")
                }
                do {
                    let info = try session.check(in: context, config: config.toMk())

                    var responseUrls = [OonimkallBridgeUrlInfo]()

                    let size = info.webConnectivity?.size() ?? 0

                    for i in 0..<size {

                        if info.webConnectivity?.at(i) != nil {
                            let urlInfo: OonimkallURLInfo = (info.webConnectivity?.at(i))!
                            responseUrls.append(
                                OonimkallBridgeUrlInfo(
                                    url: urlInfo.url,
                                    categoryCode: urlInfo.categoryCode,
                                    countryCode: urlInfo.countryCode
                                )
                            )
                        }
                    }

                    return OonimkallBridgeCheckInResults(
                        reportId: info.webConnectivity?.reportID,
                        urls: responseUrls
                    )
                } catch {
                    throw error
                }
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
                        updatedReportId: result?.updatedReportID ?? ""
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
        if let probeServicesURL = probeServicesURL {
            config.probeServicesURL = probeServicesURL
        }
        if let proxy = proxy {
            config.proxy = proxy
        }
        // Problem setting logger
        if let logger = logger {
            let applicationLogger = IosLogger(logger: logger)
            // config.logger = applicationLogger
        }
        config.verbose = verbose
        return config
    }
}

extension OonimkallBridgeCheckInConfig {
    func toMk() -> OonimkallCheckInConfig {
        let config = OonimkallCheckInConfig()
        config.charging = charging
        config.onWiFi = onWiFi
        config.platform = platform
        config.runType = runType
        config.softwareName = softwareName
        config.softwareVersion = softwareVersion
        config.webConnectivity = OonimkallCheckInConfigWebConnectivity()
        webConnectivityCategories.forEach { category in
            config.webConnectivity?.addCategory(category)
        }
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
class IosLogger: OonimkallLogger {
    private let logger: OonimkallBridgeLogger?

    override init(ref: Any) {
        self.logger = 0 as? any OonimkallBridgeLogger
        super.init(ref: ref)
    }

    init(logger: OonimkallBridgeLogger) {
        self.logger = logger
        super.init()
    }

    override func debug(_ msg: String?) {
        logger?.debug(msg: msg)
    }

    override func info(_ msg: String?) {
        logger?.info(msg: msg)
    }

    override func warn(_ msg: String?) {
        logger?.warn(msg: msg)
    }
}
