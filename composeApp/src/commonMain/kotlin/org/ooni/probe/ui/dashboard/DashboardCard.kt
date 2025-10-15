package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun DashboardCard(
    title: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    startActions: @Composable () -> Unit = {},
    endActions: @Composable () -> Unit = {},
    icon: Painter? = null,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.padding(16.dp),
        onClick = {},
        enabled = false,
    ) {
        Box {
            icon?.let {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.075f),
                    modifier = Modifier.size(88.dp).align(Alignment.TopEnd).padding(top = 4.dp),
                )
            }
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    title()
                }
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    content()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    startActions()
                    Spacer(Modifier.weight(1f))
                    endActions()
                }
            }
        }
    }
}
