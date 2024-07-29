package org.ooni.probe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.AppTheme
import org.ooni.probe.ui.main.MainScreen

@Composable
@Preview
fun App(dependencies: Dependencies) {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            MainScreen(
                dependencies.mainViewModel,
            )
        }
    }
}
