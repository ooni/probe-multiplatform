package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale

@Composable
expect fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier = Modifier,
    allowedDomains: List<String> = listOf("ooni.org"),
)

class OoniWebViewController {
    var state by mutableStateOf<State>(State.Initializing)
    var canGoBack by mutableStateOf(false)

    private var events by mutableStateOf<Set<Event>>(emptySet())

    @Composable
    fun rememberNextEvent() = remember(events) { events.firstOrNull() }

    fun load(
        url: String,
        additionalHttpHeaders: Map<String, String> = mapOf(
            "Enable-Embedded-View" to "true",
            "Accept-Language" to "${Locale.current.toLanguageTag()},${Locale.current.language};q=0.9,en;q=0.8",
        ),
    ) {
        events = events + Event.Load(url, additionalHttpHeaders)
    }

    fun reload() {
        events = events + Event.Reload
    }

    fun goBack() {
        events = events + Event.Back
    }

    fun onEventHandled(event: Event) {
        events = events - event
    }

    sealed interface Event {
        data class Load(
            val url: String,
            val additionalHttpHeaders: Map<String, String>,
        ) : Event

        data object Reload : Event

        data object Back : Event
    }

    sealed interface State {
        data object Initializing : State

        data class Loading(
            val progress: Float,
        ) : State

        data object Failure : State

        data object Successful : State

        val isFinished get() = this is Failure || this is Successful
    }
}
