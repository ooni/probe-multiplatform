import composeApp
import Foundation
import Network

class IosNetworkTypeFinder : NetworkTypeFinder {
    private let queue = DispatchQueue.global(qos: .utility)

    private var pathMonitor: NWPathMonitor?

    init() {
        _ = ensurePathMonitor()
    }

    @discardableResult
    private func ensurePathMonitor() -> NWPathMonitor {
        if (pathMonitor == nil) {
            let pathMonitor = NWPathMonitor()
            pathMonitor.start(queue: queue)
            self.pathMonitor = pathMonitor
        }
        return self.pathMonitor!
    }

    func invoke() -> any NetworkType {
        if VpnChecker.isVpnActive() {
            return NetworkTypeVPN()
        }

        let path = pathMonitor?.currentPath
        if let path = path, path.status == .satisfied {
            if path.usesInterfaceType(.wifi) {
                return NetworkTypeWifi()
            } else if path.usesInterfaceType(.cellular) {
                return NetworkTypeMobile()
            } else if path.usesInterfaceType(.wiredEthernet) {
                return NetworkTypeUnknown(value: "wired_ethernet")
            } else {
                return NetworkTypeUnknown(value: "")
            }
        }
        return NetworkTypeNoInternet()
    }

}

// https://stackoverflow.com/a/61231348
struct VpnChecker {

    private static let vpnProtocolsKeysIdentifiers = [
        "tap", "tun", "ppp", "ipsec", "utun"
    ]

    static func isVpnActive() -> Bool {
        guard let cfDict = CFNetworkCopySystemProxySettings() else { return false }
        let nsDict = cfDict.takeRetainedValue() as NSDictionary
        guard let keys = nsDict["__SCOPED__"] as? NSDictionary,
            let allKeys = keys.allKeys as? [String] else { return false }

        // Checking for tunneling protocols in the keys
        for key in allKeys {
            for protocolId in vpnProtocolsKeysIdentifiers
                where key.starts(with: protocolId) {
                // I use start(with:), so I can cover also `ipsec4`, `ppp0`, `utun0` etc...
                return true
            }
        }
        return false
    }
}
