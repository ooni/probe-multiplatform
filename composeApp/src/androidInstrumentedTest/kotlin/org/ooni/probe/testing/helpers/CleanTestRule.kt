package org.ooni.probe.testing.helpers

import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class CleanTestRule : TestWatcher() {
    override fun finished(description: Description?) {
        super.finished(description)
        cleanUp()
    }

    private fun cleanUp() {
        FileSystem.SYSTEM.list(app.filesDir.absolutePath.toPath()).forEach { path ->
            FileSystem.SYSTEM.deleteRecursively(path)
        }
    }
}
