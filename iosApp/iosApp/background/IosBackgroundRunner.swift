import composeApp
import UIKit

class IosBackgroundRunner : BackgroundRunner {
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    
    /// Invoke the given task in the background.
    func invoke(background: @escaping ()  -> Void,cancel: @escaping ()  -> Void) {
        
        // Perform the task on a background queue.
        DispatchQueue.global().async {
            // Request the task assertion and save the ID.
            self.backgroundTask = UIApplication.shared.beginBackgroundTask (withName:  Bundle.main.bundleIdentifier ?? OrganizationConfig().autorunTaskId) {
                // End the task if time expires.
                UIApplication.shared.endBackgroundTask(self.backgroundTask)
                self.backgroundTask = UIBackgroundTaskIdentifier.invalid
                cancel()
            }
            
            // run background operation synchronously.
            background()
            
            // End the task assertion.
            UIApplication.shared.endBackgroundTask(self.backgroundTask)
            self.backgroundTask = UIBackgroundTaskIdentifier.invalid
        }
    }
}
