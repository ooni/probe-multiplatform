package org.ooni.probe.locale

import java.util.Locale

actual fun buildPlatformLocale(): PlatformLocale = DesktopPlatformLocale()

private class DesktopPlatformLocale : PlatformLocale {
    // Captured at construction, before applyInitialLocale() overrides the JVM default locale.
    override val systemDefaultTag: String = Locale.getDefault().toLanguageTag()

    override fun currentTag(): String = Locale.getDefault().toLanguageTag()

    override fun setDefault(tag: String) {
        Locale.setDefault(Locale.forLanguageTag(tag))
    }
}
