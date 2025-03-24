package org.ooni.probe.ui.navigation

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ooniprobe.composeapp.generated.resources.Dashboard_Tab_Label
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Tab_Label
import ooniprobe.composeapp.generated.resources.ic_dashboard
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.MAIN_NAVIGATION_SCREENS
import org.ooni.probe.ui.shared.isHeightCompact

@Composable
fun BottomNavigationBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: return

    val customMinHeightModifier =
        Modifier.run { if (isHeightCompact()) defaultMinSize(minHeight = 64.dp) else this }

    NavigationBar(
        modifier = customMinHeightModifier,
    ) {
        MAIN_NAVIGATION_SCREENS.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(screen.iconRes),
                        contentDescription = stringResource(screen.titleRes),
                    )
                },
                label = {
                    Text(
                        stringResource(screen.titleRes),
                        textAlign = TextAlign.Center,
                    )
                },
                selected = currentRoute == screen.route,
                onClick = { navController.navigateToMainScreen(screen) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = customMinHeightModifier.testTag(screen.route),
            )
        }
    }
}

private val Screen.titleRes
    get() =
        when (this) {
            Screen.Dashboard -> Res.string.Dashboard_Tab_Label
            Screen.Results -> Res.string.TestResults_Overview_Tab_Label
            Screen.Settings -> Res.string.Settings_Title
            else -> throw IllegalArgumentException("Only main screens allowed in bottom navigation")
        }

private val Screen.iconRes
    get() =
        when (this) {
            Screen.Dashboard -> Res.drawable.ic_dashboard
            Screen.Results -> Res.drawable.ic_history
            Screen.Settings -> Res.drawable.ic_settings
            else -> throw IllegalArgumentException("Only main screens allowed in bottom navigation")
        }
