package org.ooni.probe.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Tab_Label
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.dashboard
import ooniprobe.composeapp.generated.resources.ic_dashboard
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigationBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: return

    // Only show the bottom app on the main screens
    if (!MAIN_NAVIGATION_SCREENS.map { it.route }.contains(currentRoute)) return

    NavigationBar {
        MAIN_NAVIGATION_SCREENS.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(screen.iconRes),
                        contentDescription = stringResource(screen.titleRes),
                    )
                },
                label = { Text(stringResource(screen.titleRes)) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.findStartDestination().route?.let {
                            popUpTo(it) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                },
            )
        }
    }
}

private val Screen.titleRes
    get() =
        when (this) {
            Screen.Dashboard -> Res.string.dashboard
            Screen.Results -> Res.string.TestResults_Overview_Tab_Label
            Screen.Settings -> Res.string.settings
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

private val MAIN_NAVIGATION_SCREENS = listOf(Screen.Dashboard, Screen.Results, Screen.Settings)
