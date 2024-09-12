import SwiftUI
import composeApp

@main
struct iOSApp: App {
    
    @Environment(\.openURL) var openURL
    
    @Environment(\.scenePhase) private var scenePhase
    
    let dependencies = SetupDependenciesKt.setupDependencies(
        bridge: IosOonimkallBridge(),
        networkTypeFinder: IosNetworkTypeFinder()
    )
    
    let deepLinkFlow = SetupDependenciesKt.initializeDeeplink()
    
    init() {
        SetupDependenciesKt.registerTaskHandlers(dependencies: dependencies)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView(dependencies: dependencies,  deepLinkFlow: deepLinkFlow)
                .onOpenURL { url in
                    handleDeepLink(url: url)
                }
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                // App became active
                break
            case .inactive:
                // App became inactive
                break
            case .background:
                // App is running in the background
                break
            @unknown default:
                // Fallback for future cases
                break
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
