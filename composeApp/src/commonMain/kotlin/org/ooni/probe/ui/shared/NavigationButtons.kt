package org.ooni.probe.ui.shared

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Common_Close
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_close
import ooniprobe.composeapp.generated.resources.ic_back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NavigationBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag("Back"),
    ) {
        Icon(
            painterResource(Res.drawable.ic_back),
            contentDescription = stringResource(Res.string.Common_Back),
        )
    }
}

@Composable
fun NavigationCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: StringResource = Res.string.Common_Close,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painterResource(Res.drawable.ic_close),
            contentDescription = stringResource(contentDescription),
        )
    }
}
