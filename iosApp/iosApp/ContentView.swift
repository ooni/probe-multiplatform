import UIKit
import SwiftUI
import composeApp

struct ComposeView: UIViewControllerRepresentable {
    let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
    }

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(dependencies: dependencies)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let dependencies: Dependencies

    init(dependencies: Dependencies) {
        self.dependencies = dependencies
    }

    var body: some View {
        ComposeView(dependencies: dependencies)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



