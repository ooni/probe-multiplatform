import Siren
import SwiftUI
import composeApp
import Foundation

extension URL {
    subscript(queryParam:String) -> String? {
        guard let url = URLComponents(string: self.absoluteString) else { return nil }
        return url.queryItems?.first(where: { $0.name == queryParam })?.value
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        let siren = Siren.shared
        siren.rulesManager = RulesManager(globalRules: Rules(promptFrequency: .immediately, forAlertType: .option),
                                          showAlertAfterCurrentVersionHasBeenReleasedForDays: 0)
        siren.wail()

        let launchArguments = ProcessInfo.processInfo.arguments

        if (launchArguments.contains("--skipOnboarding")){
            UserDefaults.standard.set(false, forKey: "first_run") // skip onboarding
        }
        return true
    }

}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    @Environment(\.openURL) var openURL

    @Environment(\.scenePhase)var scenePhase

    let appDependencies = SetupDependencies(
        oonimkallBridge: IosOonimkallBridge(),
        nativePassportBridge: IosNativePassportBridge(),
        networkTypeFinder: IosNetworkTypeFinder(),
        backgroundRunner: IosBackgroundRunner()
    )

    let deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow
    init() {
        appDependencies.registerTaskHandlers()
        deepLinkFlow = appDependencies.initializeDeeplink()


        let launchArguments = ProcessInfo.processInfo.arguments

        if launchArguments.contains("--presetDatabase") {
            // Enable logging
            DatabaseHelper.companion.initialize(dependency: appDependencies.dependencies)

            Task { [self] in
                await self.initDatabase()
            }

        }

    }

    func initDatabase() async {
        do {
            try await DatabaseHelper.companion.clear()
            try await DatabaseHelper.companion.setup()
        } catch {
            print("Failed to clear database: \(error)")
        }
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

