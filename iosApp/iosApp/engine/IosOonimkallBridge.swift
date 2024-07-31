import composeApp
import Oonimkall

class IosOonimkallBridge : OonimkallBridge {
    func startTask(settingsSerialized: String) throws -> OonimkallBridgeTask {
        var error: NSError?
        let task = OonimkallStartTask(settingsSerialized, &error)!

        class Task : OonimkallBridgeTask {
            var task: OonimkallTask
            init(task: OonimkallTask) { self.task = task }
            func isDone() -> Bool { task.isDone() }
            func interrupt() { task.interrupt() }
            func waitForNextEvent() -> String { task.waitForNextEvent() }
        }
    
        return Task(task: task)
    }
    
    func doNewSession(sessionConfig: OonimkallBridgeSessionConfig) throws -> OonimkallBridgeSession {
        fatalError("Not Implemented")
    }
}
