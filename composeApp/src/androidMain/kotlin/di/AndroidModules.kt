package di

import core.probe.OONIProbeClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import platform.GoOONIProbeClientBridge

val androidModule = module {
    single {
        GoOONIProbeClientBridge(
            context = androidContext()
        )
    }

    factory {
        OONIProbeClient(GoOONIProbeClientBridge(
            context = androidContext()
        ))
    }
}