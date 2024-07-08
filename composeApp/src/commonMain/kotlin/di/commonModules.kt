package di

import core.probe.OONIProbeClient
import core.settings.SettingsManager
import core.settings.SettingsStore
import core.settings.SettingsStoreImpl
import org.koin.core.module.Module
import org.koin.dsl.module
import ui.screens.home.HomeScreenModel
import main.MainViewModel
import ui.screens.onboarding.OnboardingViewModel

fun commonModule() = module {
    single<OONIProbeClient> {
        OONIProbeClient(
            ooniProbeEngine = get()
        )
    }

    single<SettingsManager> {
        SettingsManager(settings = get())
    }

    /**
     * Stores or repositories in Android speak
     */
    single<SettingsStore> {
        SettingsStoreImpl(
            settingsManager = get(),
        )
    }

    /**
     * ScreenModels
     */
    single<HomeScreenModel> {
        HomeScreenModel(
            settingsStore = get(),
            ooniProbeClient = get()
        )
    }
    single<OnboardingViewModel> {
        OnboardingViewModel(
            settingsStore = get()
        )
    }

    single<MainViewModel> {
        MainViewModel(
            settingsStore = get()
        )
    }
}
expect fun platformModule(): Module