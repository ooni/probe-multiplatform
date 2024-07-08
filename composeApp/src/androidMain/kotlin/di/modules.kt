package di

import org.koin.core.module.Module
import org.koin.dsl.module

import platform.MultiplatformSettings

actual fun platformModule(): Module = module {
    single {
        MultiplatformSettings(context = get()).createSettings()
    }
}