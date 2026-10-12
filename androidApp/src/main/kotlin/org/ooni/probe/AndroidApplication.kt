package org.ooni.probe

import android.app.Application
import android.app.LocaleConfig
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.runBlocking

class AndroidApplication : Application() {
    private val mainActivityLifecycleCallbacks = MainActivityLifecycleCallbacks()

    val dependencies by lazy {
        buildDependencies(
            app = this,
            mainActivity = mainActivityLifecycleCallbacks::activity,
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val localeManager = getSystemService(LocaleManager::class.java)
            localeManager.overrideLocaleConfig = LocaleConfig(
                LocaleList.forLanguageTags(SharedBuildConfig.SUPPORTED_LANGUAGES.joinToString(",")),
            )
        }

        runBlocking { dependencies.localeController.applyInitialLocale() }
        registerActivityLifecycleCallbacks(mainActivityLifecycleCallbacks)
        initialization(dependencies)
    }
}
