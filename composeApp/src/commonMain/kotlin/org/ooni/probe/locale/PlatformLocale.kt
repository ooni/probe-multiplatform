package org.ooni.probe.locale

/**
 * The locale Compose Resources reads when resolving strings. Every platform resolves
 * `stringResource` through `androidx.compose.ui.text.intl.Locale.current`, but each one backs it
 * with a different value: the JVM default locale on Desktop and Android, the `AppleLanguages`
 * user default on iOS.
 */
interface PlatformLocale {
    /** Language tag of the OS locale, unaffected by [setDefault]. */
    val systemDefaultTag: String

    /** Language tag currently in effect for Compose Resources. */
    fun currentTag(): String

    fun setDefault(tag: String)
}

expect fun buildPlatformLocale(): PlatformLocale
