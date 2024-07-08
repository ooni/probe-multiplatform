package di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import platform.OONIProbeEngine

val androidModule = module {
    single {
        OONIProbeEngine(
            context = androidContext(),
        )
    }
}