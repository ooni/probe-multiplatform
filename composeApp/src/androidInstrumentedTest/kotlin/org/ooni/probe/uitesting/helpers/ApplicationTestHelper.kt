package org.ooni.probe.uitesting.helpers

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import org.ooni.probe.AndroidApplication
import org.ooni.probe.MainActivity

val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

val app get() = context.applicationContext as AndroidApplication

val dependencies get() = app.dependencies

val preferences get() = dependencies.preferenceRepository

fun start(): ActivityScenario<MainActivity> {
    dependencies.backgroundContext = Dispatchers.Unconfined
    return ActivityScenario.launch(MainActivity::class.java)
}

fun start(intent: Intent): ActivityScenario<MainActivity> {
    dependencies.backgroundContext = Dispatchers.Unconfined
    return ActivityScenario.launch(intent)
}