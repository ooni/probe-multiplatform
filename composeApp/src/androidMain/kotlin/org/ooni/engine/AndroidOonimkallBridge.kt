package org.ooni.engine

class AndroidOonimkallBridge : OonimkallBridge {
    override fun startTask(settingsSerialized: String): OonimkallBridge.Task {
        val task = oonimkall.Oonimkall.startTask(settingsSerialized)
        return object : OonimkallBridge.Task {
            override fun interrupt() {
                task.interrupt()
            }

            override fun isDone(): Boolean = task.isDone

            override fun waitForNextEvent() = task.waitForNextEvent()
        }
    }
}
