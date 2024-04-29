package org.ooni.probe

import di.KoinInit
import di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        KoinInit().init {
            androidLogger(level = Level.DEBUG)
            androidContext(androidContext = this@Application)
            modules(
                listOf(
                    androidModule,
                )
            )
        }
    }
}