package org.ooni.probe.test.helpers

import co.touchlab.kermit.Logger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// Source: https://android.googlesource.com/platform/cts/+/master/tests/tests/content/lib/accountaccess/src/com.android.cts.content/FlakyTestRule.java
class FlakyTestRule(
    val attempts: Int = 2,
) : TestRule {
    override fun apply(
        statement: Statement,
        description: Description?,
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                var throwable: Throwable? = null
                repeat(attempts) {
                    try {
                        statement.evaluate()
                        return@evaluate
                    } catch (t: Throwable) {
                        Logger.e("Test failed ", t)
                        throwable = t
                    }
                }
                throw throwable!!
            }
        }
    }
}
