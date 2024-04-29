import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import cafe.adriel.voyager.navigator.Navigator
import io.github.aakira.napier.Napier

import org.jetbrains.compose.ui.tooling.preview.Preview


import org.koin.compose.koinInject
import org.koin.compose.KoinContext

import main.MainView
import main.MainViewModel
import main.OnboardingState
import ui.Theme
import ui.screens.onboarding.OnboardingView

@Composable
@Preview
fun App(
    mainViewModel: MainViewModel = koinInject(),
) {
    KoinContext {
        val onboardingState = mainViewModel.onboardingState.collectAsState().value
        Theme() {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (onboardingState) {
                    is OnboardingState.Complete -> {
                        Navigator(
                            screen = MainView()
                        )
                    }
                    is OnboardingState.Incomplete -> {
                        Navigator(
                            screen = OnboardingView()
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}