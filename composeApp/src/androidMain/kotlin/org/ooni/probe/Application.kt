package org.ooni.probe

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
    }
}