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

            func checkIn(config: OonimkallBridgeCheckInConfig) throws -> any OonimkallBridgeCheckInResults {
                var error: NSError?
                let ses = OonimkallNewSession(sessionConfig, &error)
                // throw error if any
                if error != nil {
                    throw error!
                }
                guard let context = ses?.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw NSError()
                }
                do {
                    let info = try ses?.check(in: context, config: config.toMk())
                    return IosCheckInResults(info: info!)
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
                    throw NSError()
                }
                do {
                    let response = try ses?.httpDo(context, jreq: request.toMk())
                    return IosHTTPResponse(response: response)
                } catch {
                    throw error
                }
            }

            func submitMeasurement(measurement: String) throws -> any OonimkallBridgeSubmitMeasurementResults {
                var error: NSError?
                let ses = OonimkallNewSession(sessionConfig, &error)
                // throw error if any
                if error != nil {
                    throw error!
                }
                guard let context = ses?.newContext(withTimeout: CONTEXT_TIMEOUT) else {
                    throw NSError()
                }
                do {

                    let result: OonimkallSubmitMeasurementResults? = try ses?.submit(context, measurement: measurement)
                    return IosSubmitMeasurementResults(results: result)
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

class IosHTTPResponse: OonimkallBridgeHTTPResponse {
    private let response: OonimkallHTTPResponse?

    init(response: OonimkallHTTPResponse?) {
        self.response = response
    }

    var body: String? {
        response?.body
    }
}

class IosSubmitMeasurementResults: OonimkallBridgeSubmitMeasurementResults {
    private let results: OonimkallSubmitMeasurementResults?

    init(results: OonimkallSubmitMeasurementResults?) {
        self.results = results
    }

    var updatedMeasurement: String? {
        results?.updatedMeasurement
    }

    var updatedReportId: String? {
        results?.updatedReportID
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


class IosCheckInResults: OonimkallBridgeCheckInResults {
    private let info: OonimkallCheckInInfo

    init(info: OonimkallCheckInInfo) {
        self.info = info
    }

    var reportId: String? {
        info.webConnectivity?.reportID
    }

    var urls: [OonimkallBridgeUrlInfo] {

        var responseUrls = [OonimkallBridgeUrlInfo]()

        let size = info.webConnectivity?.size() ?? 0

        for i in 0..<size {

            if info.webConnectivity?.at(i) != nil {
                let urlInfo: OonimkallURLInfo? = info.webConnectivity?.at(i)
                responseUrls.append(IosUrlInfo(urlInfo: urlInfo!))
            }
        }

        return responseUrls
    }
}

class IosUrlInfo: OonimkallBridgeUrlInfo {
    private let urlInfo: OonimkallURLInfo

    init(urlInfo: OonimkallURLInfo) {
        self.urlInfo = urlInfo
    }

    var url: String {
        urlInfo.url
    }

    var categoryCode: String? {
        urlInfo.categoryCode
    }

    var countryCode: String? {
        urlInfo.countryCode
    }
}
