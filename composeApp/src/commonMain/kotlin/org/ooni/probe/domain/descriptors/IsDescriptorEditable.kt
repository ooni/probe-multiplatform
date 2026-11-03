package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.SessionState

class IsDescriptorEditable(
    private val observeSessionState: () -> Flow<SessionState>,
) {
    operator fun invoke(descriptor: Descriptor?): Flow<Boolean> =
        observeSessionState().map { sessionState ->
            if (sessionState !is SessionState.LoggedIn || descriptor == null) return@map false

            val sessionEmail = sessionState.session.emailAddress
            val author = descriptor.author
            val isAuthor = sessionEmail.isNotEmpty() && author != null && author == sessionEmail

            val isSingleWebConnectivity = descriptor.netTests.size == 1 &&
                descriptor.netTests.first().test == TestType.WebConnectivity

            isAuthor && isSingleWebConnectivity
        }
}
