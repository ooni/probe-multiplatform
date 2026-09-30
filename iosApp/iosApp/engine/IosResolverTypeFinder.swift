import composeApp
import Foundation
import Network

/// Resolves the active network's `ResolverType` on iOS. Probes DNS transports for
/// `probeDomain` via Network.framework and maps them with the shared `ResolverTypeMapper`.
class IosResolverTypeFinder : ResolverTypeFinder {
    private let networkTypeFinder: NetworkTypeFinder
    private let probeDomain: String?
    private let timeout: DispatchTimeInterval
    private let queue = DispatchQueue(label: "org.ooni.probe.resolver-type-finder", qos: .utility)
    private let mapper: ResolverTypeMapper
    private let log: (String) -> Void

    private let reportTimeout: DispatchTimeInterval

    init(
        networkTypeFinder: NetworkTypeFinder,
        probeDomain: String?,
        timeout: DispatchTimeInterval = .seconds(3),
        reportTimeout: DispatchTimeInterval = .seconds(1),
        mapper: ResolverTypeMapper,
        log: @escaping (String) -> Void = { IosEngineLogger.shared.debug(message: $0) }
    ) {
        self.networkTypeFinder = networkTypeFinder
        self.probeDomain = probeDomain
        self.timeout = timeout
        self.reportTimeout = reportTimeout
        self.mapper = mapper
        self.log = log
    }

    /// Returns the `ResolverType` for the current network.
    func invoke() -> any ResolverType {
        let networkType = networkTypeFinder.invoke()
        self.log("[IosResolverTypeFinder] networkType: \(type(of: networkType))")

        if networkType is NetworkTypeNoInternet {
            self.log("[IosResolverTypeFinder] No internet connection; skipping Private DNS probe")
            return mapper.resolverType(networkType: networkType, dnsProtocols: nil)
        }

        let dnsProtocols = readDnsProtocols()
        let resolverType = mapper.resolverType(networkType: networkType, dnsProtocols: dnsProtocols)
        self.log("[IosResolverTypeFinder] dnsProtocols: \(String(describing: dnsProtocols)) -> resolverType: \(resolverType.value)")
        return resolverType
    }

    /// Resolves `probeDomain` over TCP 443 and returns observed DNS transports from the
    /// connection's establishment report, or nil on failure/timeout/empty report. Cached
    /// answers are returned as "cache". Blocks up to `timeout` plus `reportTimeout` if connected.
    private func readDnsProtocols() -> [String]? {
        guard let probeDomain = probeDomain else {
            self.log("[IosResolverTypeFinder] No probe domain configured; cannot determine Private DNS state")
            return nil
        }

        self.log("[IosResolverTypeFinder] Probing Private DNS using domain: \(probeDomain)")

        let connection = NWConnection(host: NWEndpoint.Host(probeDomain), port: 443, using: .tcp)
        let done = DispatchSemaphore(value: 0)
        // Only accessed on `queue`
        var finished = false
        var isReady = false
        var result: [String]? = nil

        let finish: ([String]?) -> Void = { value in
            guard !finished else { return }
            finished = true
            result = value
            connection.cancel()
            done.signal()
        }

        connection.stateUpdateHandler = { state in
            switch state {
            case .ready:
                self.log("[IosResolverTypeFinder] Connection ready; requesting establishment report")
                isReady = true
                connection.requestEstablishmentReport(queue: self.queue) { report in
                    let resolutions = report?.resolutions ?? []
                    self.log("[IosResolverTypeFinder] Establishment report resolutions: \(resolutions.count)")

                    var names: [String] = []
                    for resolution in resolutions {
                        if resolution.source == .cache || resolution.source == .expiredCache {
                            // Cached answers report no DNS transport
                            names.append("cache")
                        } else {
                            names.append(Self.protocolName(resolution.dnsProtocol))
                        }
                    }
                    self.log("[IosResolverTypeFinder] Observed DNS protocols: \(names)")
                    finish(names.isEmpty ? nil : names)
                }
            case .waiting(let error):
                // A DNS error means the hostname will not resolve and there is no report.
                // Other waiting reasons are transient, so keep waiting for a retry.
                if case .dns = error {
                    self.log("[IosResolverTypeFinder] DNS resolution failed while waiting for connection")
                    finish(nil)
                }
            case .failed(let error):
                self.log("[IosResolverTypeFinder] Connection failed: \(error.localizedDescription)")
                finish(nil)
            case .cancelled:
                // Expected after `finish()`; only log unexpected early cancellation.
                if !finished {
                    self.log("[IosResolverTypeFinder] Connection cancelled before completion")
                }
                finish(nil)
            default:
                break
            }
        }
        connection.start(queue: queue)

        if done.wait(timeout: .now() + timeout) == .timedOut,
           queue.sync(execute: { isReady && !finished }) {
            // Connection is ready; allow extra time for the establishment report.
            self.log("[IosResolverTypeFinder] Initial probe timeout elapsed, waiting \(reportTimeout) for establishment report")
            _ = done.wait(timeout: .now() + reportTimeout)
        }
        // Resolve any race between timeout and late callback on the queue.
        return queue.sync {
            finish(nil)
            self.log("[IosResolverTypeFinder] DNS probe protocols: \(String(describing: result))")
            return result
        }
    }

    private static func protocolName(_ dnsProtocol: NWConnection.EstablishmentReport.Resolution.DNSProtocol) -> String {
        switch dnsProtocol {
        case .https: return "https"
        case .tls: return "tls"
        case .udp: return "udp"
        case .tcp: return "tcp"
        default: return "unknown"
        }
    }
}
