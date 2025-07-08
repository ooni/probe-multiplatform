package org.ooni.probe.ui.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier,
) {
}

@Composable
actual fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
}
