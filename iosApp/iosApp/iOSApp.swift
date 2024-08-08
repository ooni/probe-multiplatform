import SwiftUI
import composeApp

@main
struct iOSApp: App {
    let dependencies = SetupDependenciesKt.setupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )

	var body: some Scene {
		WindowGroup {
			ContentView(dependencies: dependencies)
		}
	}
}
