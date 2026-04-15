package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizationStringTest {
    @Test
    fun withRegionMatchesWithRegionFallbacksToLanguage() {
        val subject = mapOf("pt" to "pt", "pt_BR" to "pt_BR", "en" to "en")
        assertEquals("pt_BR", subject.getCurrent("pt", "BR"))
        assertEquals("pt", subject.getCurrent("pt", "MZ"))
    }

    @Test
    fun fallbacksToAnotherRegionOfTheSameLanguage() {
        val subject = mapOf("pt_BR" to "pt_BR", "en" to "en")
        assertEquals("pt_BR", subject.getCurrent("pt", "PT"))
    }

    @Test
    fun missingFallbacksToEn() {
        val subject = mapOf("en" to "en")
        assertEquals("en", subject.getCurrent("gl", "ES"))
    }

    @Test
    fun unsupportedFallbacksToEn() {
        val subject = mapOf("gl" to "gl", "en" to "en")
        assertEquals("en", subject.getCurrent("gl", "ES"))
    }
}
