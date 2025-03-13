import composeApp
import UIKit
import BackgroundTasks

import UIKit
import os.log

class IosBackgroundRunner : BackgroundRunner {
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    private var longRunningProcess: DispatchWorkItem?
    private var completionHandler: (() -> Void)?
    private var cancelationHandler: (() -> Void)?

    func invoke(
        longRunningProcess: @escaping () -> Void,
        completionHandler: (() -> Void)? = nil,
        cancellationHandler: (() -> Void)? = nil
    ) {
        self.completionHandler = completionHandler
        self.cancelationHandler = cancellationHandler

        // Begin the background task
        backgroundTask = UIApplication.shared.beginBackgroundTask(expirationHandler: {
            // Background task has been canceled or has run out of time
            self.endBackgroundTask()
        })
        // Start the long-running process
        startLongRunningProcess(longRunningProcess)
    }

    private func startLongRunningProcess(_ longRunningProcess: @escaping () -> Void) {
        // Execute the long-running process using a DispatchWorkItem
        self.longRunningProcess = DispatchWorkItem(block: longRunningProcess)
        DispatchQueue.global().async(execute: self.longRunningProcess!)
    }

    private func endBackgroundTask() {
        // End the background task
        if backgroundTask != .invalid {
            // Call the cancelation handler if it exists
            cancelationHandler?()
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }

        // Stop the long-running process
        longRunningProcess?.cancel()
        longRunningProcess = nil

        // Call the completion handler if it exists
        completionHandler?()
    }
}
