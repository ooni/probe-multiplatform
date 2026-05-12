package org.ooni.probe.domain.credentials

import org.ooni.passport.models.ParamRange
import org.ooni.probe.data.models.Manifest
import org.ooni.testing.factories.ManifestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveSubmissionPolicyTest {
    private val resolve = ResolveSubmissionPolicy()

    @Test
    fun wildcardDefaultMatchesAnyProbe() {
        val ranges = resolve(
            manifest = ManifestFactory.build(),
            probeCc = "US",
            probeAsn = "AS1",
        )
        assertEquals(ParamRange(0u, 1_000_000u), ranges?.ageRange)
        assertEquals(ParamRange(0u, 10_000_000u), ranges?.measurementCountRange)
    }

    @Test
    fun firstMatchInListWins() {
        val ranges = resolve(
            manifest = ManifestFactory.build(
                submissionPolicy = listOf(
                    entry("IT", "*", age = 1u to 2u, count = 10u to 20u),
                    entry("*", "*", age = 0u to 99u, count = 0u to 99u),
                ),
            ),
            probeCc = "IT",
            probeAsn = "AS456",
        )
        assertEquals(ParamRange(1u, 2u), ranges?.ageRange)
        assertEquals(ParamRange(10u, 20u), ranges?.measurementCountRange)
    }

    @Test
    fun specificCcAndAsnMatchPrecisely() {
        val ranges = resolve(
            manifest = ManifestFactory.build(
                submissionPolicy = listOf(
                    entry("US", "AS1", age = 5u to 6u, count = 50u to 60u),
                    entry("*", "*", age = 0u to 99u, count = 0u to 99u),
                ),
            ),
            probeCc = "US",
            probeAsn = "AS1",
        )
        assertEquals(ParamRange(5u, 6u), ranges?.ageRange)
    }

    @Test
    fun fallsBackToWildcardWhenSpecificDoesNotMatch() {
        val ranges = resolve(
            manifest = ManifestFactory.build(
                submissionPolicy = listOf(
                    entry("DE", "*", age = 1u to 2u, count = 10u to 20u),
                    entry("*", "*", age = 0u to 99u, count = 0u to 99u),
                ),
            ),
            probeCc = "US",
            probeAsn = "AS1",
        )
        assertEquals(ParamRange(0u, 99u), ranges?.ageRange)
    }

    @Test
    fun ccAndAsnMatchCaseInsensitively() {
        val ranges = resolve(
            manifest = ManifestFactory.build(
                submissionPolicy = listOf(
                    entry("it", "as456", age = 1u to 2u, count = 10u to 20u),
                    entry("*", "*", age = 0u to 99u, count = 0u to 99u),
                ),
            ),
            probeCc = "IT",
            probeAsn = "AS456",
        )
        assertEquals(ParamRange(1u, 2u), ranges?.ageRange)
        assertEquals(ParamRange(10u, 20u), ranges?.measurementCountRange)
    }

    @Test
    fun returnsNullWhenNoEntryMatches() {
        val ranges = resolve(
            manifest = ManifestFactory.build(
                submissionPolicy = listOf(
                    entry("DE", "*", age = 1u to 2u, count = 10u to 20u),
                ),
            ),
            probeCc = "US",
            probeAsn = "AS1",
        )
        assertNull(ranges)
    }

    @Test
    fun returnsNullWhenPolicyIsEmpty() {
        val ranges = resolve(
            manifest = ManifestFactory.build(submissionPolicy = emptyList()),
            probeCc = "US",
            probeAsn = "AS1",
        )
        assertNull(ranges)
    }

    private fun entry(
        cc: String,
        asn: String,
        age: Pair<UInt, UInt>,
        count: Pair<UInt, UInt>,
    ) = Manifest.SubmissionPolicyEntry(
        match = Manifest.SubmissionPolicyEntry.Match(probeCc = cc, probeAsn = asn),
        policy = Manifest.SubmissionPolicyEntry.Policy(
            age = listOf(age.first, age.second),
            measurementCount = listOf(count.first, count.second),
        ),
    )
}
