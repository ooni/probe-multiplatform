package org.ooni.probe.domain.articles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ooni.probe.data.models.ArticlesRefreshState

/** Mirrors `DescriptorUpdateStateManager`: one process-wide holder observed by the UI. */
class ArticlesRefreshStateManager {
    private val state = MutableStateFlow<ArticlesRefreshState>(ArticlesRefreshState.Idle)

    fun observe() = state.asStateFlow()

    fun update(newState: ArticlesRefreshState) {
        state.value = newState
    }
}
