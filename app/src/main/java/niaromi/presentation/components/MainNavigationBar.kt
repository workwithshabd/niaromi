package niaromi.presentation.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import niaromi.ui.home.TopLevelRoute
import kotlin.math.roundToInt

private val routes = listOf(
    TopLevelRoute.Library to "Home",
    TopLevelRoute.Explore to "Explore",
    TopLevelRoute.More to "Settings",
)

@Composable
fun MainNavigationBar(
    currentRoute: TopLevelRoute,
    onSelectRoute: (TopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
    onReselectRoute: (TopLevelRoute) -> Unit = onSelectRoute,
) {
    val selectedIndex = routes.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)
    val currentIndex by rememberUpdatedState(selectedIndex)
    val select by rememberUpdatedState(onSelectRoute)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    NavigationBar(
        modifier = modifier.pointerInput(rtl) {
            var logicalPosition = 0f
            detectHorizontalDragGestures(
                onDragStart = { logicalPosition = if (rtl) size.width - it.x else it.x },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    logicalPosition += if (rtl) -amount else amount
                },
                onDragEnd = {
                    val tabWidth = size.width.toFloat() / routes.size
                    val index = (logicalPosition / tabWidth - 0.5f).roundToInt().coerceIn(routes.indices)
                    if (index != currentIndex) select(routes[index].first)
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        routes.forEachIndexed { index, (route, label) ->
            val selected = index == selectedIndex
            val options = route.options(selected)
            NavigationBarItem(
                selected = selected,
                onClick = { if (selected) onReselectRoute(route) else onSelectRoute(route) },
                icon = { Icon(painter = options.icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}
