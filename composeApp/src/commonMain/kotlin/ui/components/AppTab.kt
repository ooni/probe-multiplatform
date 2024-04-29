package ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.rememberVectorPainter

import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

import ui.screens.home.HomeScreen

internal sealed class AppTab {
    internal object HomeTab : Tab {
        override val options: TabOptions
            @Composable
            get() {
                val title = "Home"
                val icon = rememberVectorPainter(image = Icons.Outlined.Home)
                return remember {
                    TabOptions(
                        index = 0u,
                        title = title,
                        icon = icon,
                    )
                }
            }

        @Composable
        override fun Content() {
            HomeScreen()
        }
    }
}