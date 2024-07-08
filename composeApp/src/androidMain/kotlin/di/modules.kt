package di

import org.koin.core.module.Module
import org.koin.dsl.module

import platform.OONIProbeEngine

actual fun platformModule(): Module = module {
    single {
        OONIProbeEngine()
    }
}