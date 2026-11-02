package org.ooni.probe.domain.descriptors

import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor

class IsDescriptorEditable(
    private val getSession: suspend () -> AuthSession?,
) {
    suspend operator fun invoke(descriptor: Descriptor?): Boolean {
        if (descriptor == null) return false

        val author = descriptor.author
        val sessionEmail = getSession()?.emailAddress
        val isAuthor = !sessionEmail.isNullOrEmpty() && author != null && author == sessionEmail

        val isSingleWebConnectivity = descriptor.netTests.size == 1 &&
            descriptor.netTests.first().test == TestType.WebConnectivity

        return isAuthor && isSingleWebConnectivity
    }
}
