import composeApp
import UIKit

class IosBackgroundRunner : BackgroundRunner {
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid

    /// Invoke the given task in the background.
    func invoke(onDoInBackground: @escaping ()  -> Void,onCancel: @escaping ()  -> Void) {

        // Perform the task on a background queue.
        DispatchQueue.global().async {
            // Request the task assertion and save the ID.
            self.backgroundTask = UIApplication.shared.beginBackgroundTask (withName:  Bundle.main.bundleIdentifier ?? OrganizationConfig().autorunTaskId) {
                // End the task if time expires.
                UIApplication.shared.endBackgroundTask(self.backgroundTask)
                self.backgroundTask = UIBackgroundTaskIdentifier.invalid
                onCancel()
            }

            // run background operation synchronously.
            onDoInBackground()

            // End the task assertion.
            UIApplication.shared.endBackgroundTask(self.backgroundTask)
            self.backgroundTask = UIBackgroundTaskIdentifier.invalid
        }
    }
}
