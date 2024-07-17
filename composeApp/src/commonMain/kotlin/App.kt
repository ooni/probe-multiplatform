import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import di.Dependencies
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.Theme
import ui.main.MainScreen

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