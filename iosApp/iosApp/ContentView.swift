import UIKit
import SwiftUI
import composeApp

struct ComposeView: UIViewControllerRepresentable {
    let dependencies: Dependencies
    let deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow

    init(dependencies: Dependencies, deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow) {
        self.dependencies = dependencies
        self.deepLinkFlow = deepLinkFlow
    }

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(dependencies: dependencies, deepLinkFlow: deepLinkFlow)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let dependencies: Dependencies
    let deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow

    init(dependencies: Dependencies, deepLinkFlow: Kotlinx_coroutines_coreMutableSharedFlow) {
        self.dependencies = dependencies
        self.deepLinkFlow = deepLinkFlow
    }

    var body: some View {
        ComposeView(dependencies: dependencies, deepLinkFlow: deepLinkFlow)
                .ignoresSafeArea(edges: .all) // https://stackoverflow.com/a/78053779
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



