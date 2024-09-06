import SwiftUI
import composeApp

@main
struct iOSApp: App {

    @Environment(\.openURL) var openURL

    let dependencies = SetupDependenciesKt.setupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )

	var body: some Scene {
		WindowGroup {
			ContentView(dependencies: dependencies)
                .onOpenURL { url in
                    // Handle the deep link here
                    print("Opened URL: \(url)")
                }
		}
	}
}
