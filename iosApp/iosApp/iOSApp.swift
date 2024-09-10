import SwiftUI
import composeApp

@main
struct iOSApp: App {

    @Environment(\.openURL) var openURL

    let dependencies = SetupDependenciesKt.setupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )

    let deepLinkFlow = SetupDependenciesKt.initializeDeeplink()

	var body: some Scene {
		WindowGroup {
			ContentView(dependencies: dependencies,  deepLinkFlow: deepLinkFlow)
                .onOpenURL { url in
                    handleDeepLink(url: url)
                }
		}
	}


    private func handleDeepLink(url: URL) {
        // TODO(aanorbel): remove when web send proper link
        deepLinkFlow.emit(value: DeepLink.AddDescriptor(id: "10445"), completionHandler: {error in
            print(error ?? "none")
        })// Handle the deep link here
        print("Opened URL: \(url)")

        if let host = url.host, host == "runv2" || host == "run.test.ooni.org" {
            let id = url.lastPathComponent
            deepLinkFlow.emit(value: DeepLink.AddDescriptor(id: id), completionHandler: {error in
                print(error ?? "none")
            })
        }
    }
}
