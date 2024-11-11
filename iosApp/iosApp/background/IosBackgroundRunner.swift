import composeApp
import AVFAudio
import UIKit

class IosBackgroundRunner : BackgroundRunner {
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    private var audioPlayer: AVAudioPlayer?

    /// Invoke the given task in the background.
    func invoke(background: @escaping ()  -> Void) {
        let serialQueue = DispatchQueue(label: "org.openobservatory.queue", qos: .userInitiated, attributes: [.concurrent])
        serialQueue.async(execute: DispatchWorkItem {
            self.setupBackgroundTask()
            background()
            self.stopBackgroundTask()
        } )
    }

    /// Set up a background task and audio player to keep the app running in the background.
    /// This method should be called at the beginning of the background task.
    func setupBackgroundTask(){
        // Create a minimal WAV file as NSData
        let bytes: [UInt8] = [0x52, 0x49, 0x46, 0x46, 0x26, 0x0, 0x0, 0x0, 0x57, 0x41, 0x56, 0x45,
                              0x66, 0x6d, 0x74, 0x20, 0x10, 0x0, 0x0, 0x0, 0x1, 0x0, 0x1, 0x0,
                              0x44, 0xac, 0x0, 0x0, 0x88, 0x58, 0x1, 0x0, 0x2, 0x0, 0x10, 0x0,
                              0x64, 0x61, 0x74, 0x61, 0x2, 0x0, 0x0, 0x0, 0xfc, 0xff]
        let data = Data(bytes)

        // Get the document directory path
        guard let docsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let filePath = docsDir.appendingPathComponent("background.wav")

        // Write data to file
        try? data.write(to: filePath)

        // Configure the audio session for background playback
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.playback, options: [.mixWithOthers])
            try audioSession.setActive(true)

            // Initialize the audio player with the sound file URL
            self.audioPlayer = try AVAudioPlayer(contentsOf: filePath)
            self.audioPlayer?.numberOfLoops = -1 // Loop indefinitely
            self.audioPlayer?.play()
        } catch {
            print("Failed to set up audio session or audio player: \(error)")
        }

        self.backgroundTask = UIApplication.shared.beginBackgroundTask(expirationHandler: {
            // Clean up when background time expires
            UIApplication.shared.endBackgroundTask(self.backgroundTask)
            self.backgroundTask = .invalid
        })
    }

    /// Stop the background task and audio player.
    /// This method should be called when the background task is complete.
    func stopBackgroundTask() {
        audioPlayer?.stop()
        audioPlayer = nil
        if backgroundTask != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTask)
            backgroundTask = .invalid
        }
    }

}
