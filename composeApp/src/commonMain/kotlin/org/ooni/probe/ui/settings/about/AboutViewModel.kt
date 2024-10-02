package org.ooni.probe.ui.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.value

class AboutViewModel(
    onBack: () -> Unit,
    launchUrl: (String) -> Unit,
    platformInfo: PlatformInfo,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    init {
        events.filterIsInstance<Event.BackClicked>().onEach { onBack() }.launchIn(viewModelScope)
        events.filterIsInstance<Event.LaunchUrlClicked>().onEach { url -> launchUrl(url.url) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    val softwareName = "${OrganizationConfig.baseSoftwareName}-${platformInfo.platform.value}"
    val softwareVersion = platformInfo.version

    sealed interface Event {
        data object BackClicked : Event

        data class LaunchUrlClicked(val url: String) : Event
    }
}
