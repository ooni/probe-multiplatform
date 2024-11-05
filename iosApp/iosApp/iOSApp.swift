import SwiftUI
import composeApp

@main
struct iOSApp: App {

    @Environment(\.openURL) var openURL
    @Environment(\.scenePhase)var scenePhase

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
        if let host = url.host, host == "runv2" || host == appDependencies.ooniRunDomain() {
            let id = url.lastPathComponent
            deepLinkFlow.emit(value: DeepLink.AddDescriptor(id: id), completionHandler: {error in
                print(error ?? "none")
            })
        }
    }
}
