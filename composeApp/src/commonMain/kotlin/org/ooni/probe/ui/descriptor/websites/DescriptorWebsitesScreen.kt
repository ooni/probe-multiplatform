package org.ooni.probe.ui.descriptor.websites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun DescriptorWebsitesViewModel(
    state: DescriptorWebsitesViewModel.State,
    onEvent: (DescriptorWebsitesViewModel.Event) -> Unit,
) {
    val showState = state as? DescriptorWebsitesViewModel.State.Show

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        val descriptorColor = showState?.color ?: MaterialTheme.colorScheme.primary
        val onDescriptorColor = LocalCustomColors.current.onDescriptor

        TopBar(
            title = { Text(showState?.title().orEmpty()) },
            navigationIcon = {
                NavigationBackButton({ onEvent(DescriptorWebsitesViewModel.Event.BackClicked) })
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = descriptorColor,
                scrolledContainerColor = descriptorColor,
                navigationIconContentColor = onDescriptorColor,
                titleContentColor = onDescriptorColor,
                actionIconContentColor = onDescriptorColor,
            ),
        )

        Box {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 8.dp,
                ),
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(showState?.websites.orEmpty()) { website ->
                    SelectionContainer {
                        Text(
                            text = website,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            VerticalScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
