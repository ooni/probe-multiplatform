package org.ooni.probe.locale

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

actual fun buildPlatformLocale(): PlatformLocale = IosPlatformLocale()

private class IosPlatformLocale : PlatformLocale {
    override val systemDefaultTag: String = preferredLanguage()

    override fun currentTag(): String = preferredLanguage()

    override fun setDefault(tag: String) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    private fun preferredLanguage() = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
}
