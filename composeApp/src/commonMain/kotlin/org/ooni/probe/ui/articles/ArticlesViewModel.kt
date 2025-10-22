package org.ooni.probe.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.ArticleModel

class ArticlesViewModel(
    onBack: () -> Unit,
    goToArticle: (ArticleModel.Url) -> Unit,
    getArticles: () -> Flow<List<ArticleModel>>,
    refreshArticles: suspend () -> Unit,
    canPullToRefresh: Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(canPullToRefresh = canPullToRefresh))
    val state = _state.asStateFlow()

    init {
        getArticles()
            .onEach { articles -> _state.update { it.copy(articles = articles) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ArticleClicked>()
            .onEach { goToArticle(it.article.url) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.Refresh>()
            .onEach {
                if (state.value.isRefreshing) return@onEach
                _state.update { it.copy(isRefreshing = true) }
                refreshArticles()
                _state.update { it.copy(isRefreshing = false) }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val articles: List<ArticleModel> = emptyList(),
        val isRefreshing: Boolean = false,
        val canPullToRefresh: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class ArticleClicked(
            val article: ArticleModel,
        ) : Event

        data object Refresh : Event
    }
}
