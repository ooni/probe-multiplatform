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
        class IosSession: OonimkallBridgeSession {
            private let sessionConfig: OonimkallSessionConfig

            init(sessionConfig: OonimkallSessionConfig) {
                self.sessionConfig = sessionConfig
            }

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

            func checkIn(config: OonimkallBridgeCheckInConfig) throws -> OonimkallBridgeCheckInResults {
                var error: NSError?
                let ses = OonimkallNewSession(sessionConfig, &error)
                // throw error if any
                if error != nil {
                    throw error!
                }
                guard let context = ses?.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw self.error("Unable to create context")
                }
                do {
                    let info = try ses?.check(in: context, config: config.toMk())
                    
                    
                    var responseUrls = [OonimkallBridgeUrlInfo]()

                    let size = info?.webConnectivity?.size() ?? 0

                    for i in 0..<size {

                        if info?.webConnectivity?.at(i) != nil {
                            let urlInfo: OonimkallURLInfo = (info?.webConnectivity?.at(i))!
                            responseUrls.append(
                                OonimkallBridgeUrlInfo(
                                    url: urlInfo.url,
                                    categoryCode: urlInfo.categoryCode,
                                    countryCode: urlInfo.countryCode
                                )
                            )
                        }
                    }
                    
                    do {
                        try ses?.close()
                    } catch {}
                    return OonimkallBridgeCheckInResults(
                        reportId: info?.webConnectivity?.reportID,
                        urls: responseUrls
                    )
                } catch {
                    throw error
                }
            }

            func httpDo(request: OonimkallBridgeHTTPRequest) throws -> OonimkallBridgeHTTPResponse {
                var error: NSError?
                let ses = OonimkallNewSession(sessionConfig, &error)
                // throw error if any
                if error != nil {
                    throw error!
                }
                guard let context = ses?.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw self.error("Unable to create context")
                }
                do {
                    let response = try ses?.httpDo(context, jreq: request.toMk())
                    return OonimkallBridgeHTTPResponse(body: response?.body)
                } catch {
                    throw error
                }
            }

            func submitMeasurement(measurement: String) throws -> OonimkallBridgeSubmitMeasurementResults {
                var error: NSError?
                let ses = OonimkallNewSession(sessionConfig, &error)
                // throw error if any
                if error != nil {
                    throw error!
                }
                guard let context = ses?.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw self.error("Unable to create context")
                }
                do {

                    let result: OonimkallSubmitMeasurementResults? = try ses?.submit(context, measurement: measurement)
                    return OonimkallBridgeSubmitMeasurementResults(
                        updatedMeasurement: result?.updatedMeasurement,
                        updatedReportId: result?.updatedReportID ?? ""
                    )
                } catch {
                    throw error
                }
            }
        }

        return IosSession(sessionConfig: sessionConfig.toMk())
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
