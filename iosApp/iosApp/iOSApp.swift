import SwiftUI
import composeApp

extension URL {
    subscript(queryParam:String) -> String? {
        guard let url = URLComponents(string: self.absoluteString) else { return nil }
        return url.queryItems?.first(where: { $0.name == queryParam })?.value
    }
}

@main
struct iOSApp: App {

    @Environment(\.openURL) var openURL

    let appDependencies = SetupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )

    let deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow
    init() {
        appDependencies.registerTaskHandlers()
        deepLinkFlow = appDependencies.initializeDeeplink()

    }

    var body: some Scene {
        WindowGroup {
            ContentView(dependencies: appDependencies.dependencies,  deepLinkFlow: deepLinkFlow)
                .onOpenURL { url in
                    handleDeepLink(url: url)
                }
        }
    }


    private func handleDeepLink(url: URL) {
        if let host = url.host, host == "runv2" || host == appDependencies.ooniRunDomain() {
            let id = url.lastPathComponent
            deepLinkFlow.emit(value: DeepLink.AddDescriptor(id: id), completionHandler: {error in
                print(error ?? "none")
            })
        }

        if let host = url.host, host == "nettest" {
            if let webAddress = url["url"] {
                deepLinkFlow.emit(value: DeepLink.RunUrls(url: webAddress), completionHandler: {error in
                    print(error ?? "none")
                })
            }
        }
    }
}
