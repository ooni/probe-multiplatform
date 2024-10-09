package org.ooni.probe.test.helpers

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.ooni.probe.AndroidApplication
import org.ooni.probe.MainActivity

val app get() =
    InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as AndroidApplication

val dependencies get() = app.dependencies

val preferences get() = dependencies.preferenceRepository

fun start() = ActivityScenario.launch(MainActivity::class.java)
