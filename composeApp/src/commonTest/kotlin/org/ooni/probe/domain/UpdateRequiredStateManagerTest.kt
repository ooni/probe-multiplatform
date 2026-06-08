package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateRequiredStateManagerTest {
    @Test
    fun startsNotRequired() =
        runTest {
            val subject = UpdateRequiredStateManager()
            assertFalse(subject.observeUpdateRequired().first())
        }

    @Test
    fun signalSetsRequired() =
        runTest {
            val subject = UpdateRequiredStateManager()

            subject.signalUpdateRequired()

            assertTrue(subject.observeUpdateRequired().first())
        }

    @Test
    fun dismissResets() =
        runTest {
            val subject = UpdateRequiredStateManager()
            subject.signalUpdateRequired()

            subject.dismiss()

            assertEquals(false, subject.observeUpdateRequired().first())
        }
}
