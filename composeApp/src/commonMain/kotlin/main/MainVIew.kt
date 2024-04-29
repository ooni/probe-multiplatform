package main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import io.github.aakira.napier.Napier

import ui.components.AppTab

class MainView: Screen {
    @Composable
    override fun Content() {
        TabNavigator(
            AppTab.HomeTab,
        ) {
            Scaffold(
                content = { CurrentScreen() },
                bottomBar = {
                    BottomNavigation(
                        backgroundColor = MaterialTheme.colorScheme.background,
                    ) {
                        TabNavigationItem(AppTab.HomeTab)
                    }
                },
            )
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    // TODO(art): There doesn't seem to be a nice way to pick selected and
    // unselected icons in Voyager, so we don't implement it for the moment.
    // See:
    // https://github.com/adrielcafe/voyager/issues/141
    // https://github.com/adrielcafe/voyager/issues/313
    val isSelected = tabNavigator.current == tab

    BottomNavigationItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { tab.options.icon?.let {
            Icon(
                painter = it,
                contentDescription = tab.options.title,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
            )
        }
        }
    )
}