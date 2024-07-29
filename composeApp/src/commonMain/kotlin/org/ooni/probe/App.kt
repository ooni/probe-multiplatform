package org.ooni.probe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.di.Dependencies
import org.ooni.probe.ui.AppTheme
import org.ooni.probe.ui.main.MainScreen

@Composable
@Preview
fun App(dependencies: Dependencies) {
    LaunchedEffect(Unit) {
        logAppStart(dependencies)
    }

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            MainScreen(
                dependencies.mainViewModel,
            )
        }
    }
}

private fun logAppStart(dependencies: Dependencies) {
    with(dependencies.platformInfo) {
        Logger.i(
            """
            ---APP START---
            Platform: $platform ($osVersion)"
            Version: $version
            Model: $model
            """.trimIndent(),
        )
    }
}
