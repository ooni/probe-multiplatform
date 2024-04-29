package ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    screenModel: HomeScreenModel = koinInject(),
) {
    val publicIP = screenModel.publicIP.collectAsState().value
    val httpResponse = screenModel.httpResponse.collectAsState().value
    HomeScreenContent(
        onClickReset={
            screenModel.clearSettings()
        },
        onClickDoRequest = {
            screenModel.doHTTPRequest()
        },
        onClickDoIPLookup = {
            screenModel.lookupIP()
        },
        publicIP=publicIP,
        httpResponse=httpResponse
    )
}

@Composable
private fun HomeScreenContent(
    publicIP: String,
    httpResponse: String,
    onClickReset: () -> Unit,
    onClickDoIPLookup: () -> Unit,
    onClickDoRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Text("This is home.",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = publicIP,
                    onValueChange = {},
                    label = {Text("public ip")},
                    readOnly = true
                )
                Button(
                    onClick = onClickDoIPLookup,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("DoIPLookup")
                }

                TextField(
                    value = httpResponse,
                    onValueChange = {},
                    label = {Text("http response")},
                    readOnly = true
                )
                Button(
                    onClick = onClickDoRequest,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("DoRequest")
                }

                Spacer(modifier = Modifier.height(78.dp))

                Button(
                    onClick = onClickReset,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Reset app")
                }
            }
        }
    }
}