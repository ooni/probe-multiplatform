package org.ooni.probe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.ooni.probe.di.Dependencies
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.Theme
import org.ooni.probe.ui.main.MainScreen

@Composable
@Preview
fun App(
    dependencies: Dependencies
) {
    Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            MainScreen(
                dependencies.mainViewModel
            )
        }
    }
}