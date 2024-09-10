import SwiftUI
import composeApp

@main
struct iOSApp: App {

    @Environment(\.openURL) var openURL

    let dependencies = SetupDependenciesKt.setupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )

    let deepLinkFlow = Kotlinx_coroutines_coreSharedFlow(replay: 0)

	var body: some Scene {
		WindowGroup {
			ContentView(dependencies: dependencies,  deepLinkFlow: deepLinkFlow)
                .onOpenURL { url in
                  // Handle the deep link here
                    print("Opened URL: \(url)")
                    if let host = url.host, host == "runv2" || host == "run.test.ooni.org" {
                        let id = url.lastPathComponent
                        deepLinkFlow.tryEmit(DeepLink.AddDescriptor(id: id))
                    }
                }
		}
	}
}
