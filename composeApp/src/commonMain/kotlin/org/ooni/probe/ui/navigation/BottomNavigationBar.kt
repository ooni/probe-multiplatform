package org.ooni.probe.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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

@Composable
fun BottomNavigationBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: return

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
                onClick = { navController.navigateToMainScreen(screen) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

fun NavController.navigateToMainScreen(screen: Screen) {
    navigate(screen.route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        graph.findStartDestination().route?.let {
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
