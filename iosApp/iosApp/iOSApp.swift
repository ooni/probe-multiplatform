import SwiftUI
import composeApp

@main
struct iOSApp: App {
    let dependencies = SetupDependenciesKt.setupDependencies(bridge: IosOonimkallBridge())

	var body: some Scene {
		WindowGroup {
			ContentView(dependencies: dependencies)
		}
	}
}
