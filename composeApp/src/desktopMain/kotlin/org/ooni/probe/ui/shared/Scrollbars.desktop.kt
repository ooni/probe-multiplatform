package org.ooni.probe.ui.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.commonForVertical(),
    )
}

@Composable
actual fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.commonForVertical(),
    )
}

private fun Modifier.commonForVertical() = fillMaxHeight().padding(2.dp)
