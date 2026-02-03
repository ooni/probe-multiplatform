package org.ooni.probe.ui.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier,
) {
    VerticalScrollbar(state, rememberScrollbarAdapter(state), modifier)
}

@Composable
actual fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    VerticalScrollbar(state, rememberScrollbarAdapter(state), modifier)
}

@Composable
private fun VerticalScrollbar(
    state: ScrollableState,
    adapter: ScrollbarAdapter,
    modifier: Modifier,
) {
    if (state.canScrollForward || state.canScrollBackward) {
        VerticalScrollbar(
            adapter = adapter,
            modifier = modifier.fillMaxHeight().padding(2.dp),
        )
    }
}
