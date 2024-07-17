package ui.main

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val result by viewModel.result.collectAsState()
    Text(result ?: "Waiting result")
}
