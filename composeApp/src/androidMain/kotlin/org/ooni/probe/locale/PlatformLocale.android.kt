package org.ooni.probe.locale

import android.content.res.Resources
import java.util.Locale

actual fun buildPlatformLocale(): PlatformLocale = AndroidPlatformLocale()

private class AndroidPlatformLocale : PlatformLocale {
    override val systemDefaultTag: String
        get() = Resources
            .getSystem()
            .configuration.locales[0]
            .toLanguageTag()

    override fun currentTag(): String = Locale.getDefault().toLanguageTag()

    override fun setDefault(tag: String) {
        Locale.setDefault(Locale.forLanguageTag(tag))
    }
}
