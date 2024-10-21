package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = ColorDefaults.topAppBar(),
) {
    TopAppBar(
        title = title,
        modifier = modifier
            .run { if (isHeightCompact()) heightIn(max = 72.dp) else this },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
    )
}
