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

    @Environment(\.scenePhase)var scenePhase

    let appDependencies = SetupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder(),
        backgroundRunner: IosBackgroundRunner()
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
                .onChange(of: scenePhase) { newPhase in
                    if newPhase == .inactive {
                        // no-op
                    } else if newPhase == .active {
                        // no-op
                    } else if newPhase == .background {
                        appDependencies.scheduleNextAutorun()
                    }
                }
        }
    }


    private func handleDeepLink(url: URL) {
        guard let host = url.host else {
            deepLinkFlow.emit(value: DeepLink.Error(), completionHandler: { error in
                print(error ?? "none")
            })
            return
        }

        if host == "runv2" || host == appDependencies.ooniRunDomain() {
            let id = url.lastPathComponent
            deepLinkFlow.emit(value: DeepLink.AddDescriptor(id: id), completionHandler: {error in
                print(error ?? "none")
            })
        } else if host == "nettest" {
            if let webAddress = url["url"] {
                deepLinkFlow.emit(value: DeepLink.RunUrls(url: webAddress), completionHandler: {error in
                    print(error ?? "none")
                })
            } else {

                deepLinkFlow.emit(value: DeepLink.Error(), completionHandler: {error in
                    print(error ?? "none")
                })
            }
        }
    }
}
