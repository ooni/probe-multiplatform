package di

import core.probe.OONIProbeClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import platform.OONIProbeEngine

val androidModule = module {
    single {
        OONIProbeEngine()
    }
}