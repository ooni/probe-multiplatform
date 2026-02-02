package org.ooni.probe.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Refresh
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_refresh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar

@Composable
fun ArticlesScreen(
    state: ArticlesViewModel.State,
    onEvent: (ArticlesViewModel.Event) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    Box(
        Modifier
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(ArticlesViewModel.Event.Refresh) },
                state = pullRefreshState,
                enabled = state.canPullToRefresh,
            ).background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            TopBar(
                title = { Text(stringResource(Res.string.Dashboard_Articles_Title)) },
                navigationIcon = {
                    NavigationBackButton({ onEvent(ArticlesViewModel.Event.BackClicked) })
                },
                actions = {
                    if (!state.canPullToRefresh) {
                        IconButton(
                            onClick = { onEvent(ArticlesViewModel.Event.Refresh) },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_refresh),
                                contentDescription = stringResource(Res.string.Common_Refresh),
                            )
                        }
                    }
                },
            )

            Box(Modifier.fillMaxSize()) {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                    state = lazyListState,
                ) {
                    items(state.articles, key = { it.url.value }) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onEvent(ArticlesViewModel.Event.ArticleClicked(article)) },
                        )
                    }
                }
                VerticalScrollbar(
                    state = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = state.isRefreshing,
            state = pullRefreshState,
        )
    }
}
