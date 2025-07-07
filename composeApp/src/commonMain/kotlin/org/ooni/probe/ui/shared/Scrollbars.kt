package org.ooni.probe.ui.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
)

@Composable
expect fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)
