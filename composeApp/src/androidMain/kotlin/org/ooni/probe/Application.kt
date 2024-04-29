package org.ooni.probe

import di.KoinInit
import di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
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