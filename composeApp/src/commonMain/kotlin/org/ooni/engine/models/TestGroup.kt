package org.ooni.engine.models

import kotlin.reflect.KClass

sealed class TestGroup(
    vararg tests: KClass<out TestType>,
) {
    val tests: List<KClass<out TestType>> = tests.toList()

    data object Websites : TestGroup(TestType.WebConnectivity::class)

    data object InstantMessaging : TestGroup(
        TestType.Whatsapp::class,
        TestType.Telegram::class,
        TestType.FacebookMessenger::class,
        TestType.Signal::class,
    )

    data object Circumvention : TestGroup(
        TestType.Psiphon::class,
        TestType.Tor::class,
    )

    data object Performance : TestGroup(
        TestType.Ndt::class,
        TestType.Dash::class,
        TestType.HttpHeaderFieldManipulation::class,
        TestType.HttpInvalidRequestLine::class,
    )

    data object Experimental : TestGroup(TestType.Experimental::class)

    data object Unknown : TestGroup()

    companion object {
        fun fromTests(tests: List<TestType>): TestGroup {
            if (tests.isEmpty()) return Unknown

            return listOf(
                Websites,
                InstantMessaging,
                Circumvention,
                Performance,
                Experimental,
            )
                .firstOrNull { group ->
                    tests.all { test -> group.tests.any { it.isInstance(test) } }
                }
                ?: Unknown
        }
    }
}
