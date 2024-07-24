package org.ooni.engine

interface OonimkallBridge {
    fun startTask(settingsSerialized: String): Task

    interface Task {
        fun interrupt()
        fun isDone(): Boolean
        fun waitForNextEvent(): String
    }
}