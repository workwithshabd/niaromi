package niaromi.presentation.core.components.material

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * @param refreshing Whether the layout is currently refreshing
 * @param onRefresh Lambda which is invoked when a swipe to refresh gesture is completed.
 * @param enabled Whether the the layout should react to swipe gestures or not.
 * @param indicatorPadding Content padding for the indicator, to inset the indicator in if required.
 * @param content The content containing a vertically scrollable composable.
 */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    state: PullToRefreshState = rememberPullToRefreshState(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .pullToRefresh(
                isRefreshing = refreshing,
                state = state,
                enabled = enabled,
                onRefresh = onRefresh,
            ),
    ) {
        content()

        PullToRefreshDefaults.Indicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(indicatorPadding),
            isRefreshing = refreshing,
            state = state,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Stable
class SpringRefreshState internal constructor(
    private val indicatorState: PullToRefreshState,
) : PullToRefreshState by indicatorState {
    internal var dragFraction by mutableFloatStateOf(0f)
        private set

    override suspend fun snapTo(targetValue: Float) {
        dragFraction = targetValue
        indicatorState.snapTo(targetValue)
    }

    override suspend fun animateToThreshold() {
        dragFraction = 0f
        indicatorState.animateToThreshold()
    }

    override suspend fun animateToHidden() {
        dragFraction = 0f
        indicatorState.animateToHidden()
    }
}

@Composable
fun rememberSpringRefreshState(): SpringRefreshState {
    val indicatorState = rememberPullToRefreshState()
    return remember(indicatorState) { SpringRefreshState(indicatorState) }
}

@Composable
fun rememberRefreshPull(state: SpringRefreshState, refreshing: Boolean = false): State<Float> =
    animateFloatAsState(
        targetValue = if (refreshing) 0f else state.dragFraction.coerceIn(0f, 1.35f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "Refresh spring",
    )

fun Modifier.refreshStretch(pull: State<Float>): Modifier = graphicsLayer {
    val distance = pull.value
    translationY = 22.dp.toPx() * distance
    transformOrigin = TransformOrigin(0.5f, 0f)
    scaleX = 1f - 0.025f * distance
    scaleY = 1f + 0.045f * distance
}
