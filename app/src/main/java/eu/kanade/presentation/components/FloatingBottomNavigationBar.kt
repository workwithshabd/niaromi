package eu.kanade.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.home.TopLevelRoute
import niaromi.icons.materialsymbols.rounded.Book
import niaromi.icons.materialsymbols.rounded.Explore
import niaromi.icons.materialsymbols.rounded.Settings

private val NavContainerBg = Color(0xFFFAF8F0).copy(alpha = 0.96f)
private val NavContainerBorder = Color(0xFFE8E5DA)
private val ActivePillBg = Color(0xFF1D1D1D)
private val InactiveText = Color(0xFF929292)

data class NavTabItem(
    val route: TopLevelRoute,
    val label: String,
    val icon: ImageVector,
    val matches: (TopLevelRoute) -> Boolean = { it == route },
)

val NAV_TAB_ITEMS = listOf(
    NavTabItem(
        route = TopLevelRoute.Library,
        label = "Home",
        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Book,
        matches = { it is TopLevelRoute.Library },
    ),
    NavTabItem(
        route = TopLevelRoute.Explore,
        label = "Explore",
        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Explore,
        matches = { it is TopLevelRoute.Explore },
    ),
    NavTabItem(
        route = TopLevelRoute.More,
        label = "Settings",
        icon = niaromi.icons.materialsymbols.MaterialSymbols.Rounded.Settings,
        matches = { it is TopLevelRoute.More },
    ),
)

@Composable
fun FloatingBottomNavigationBar(
    currentRoute: TopLevelRoute,
    onSelectRoute: (TopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color(0x18000000),
                spotColor = Color(0x28000000),
            )
            .background(NavContainerBg, RoundedCornerShape(32.dp))
            .border(0.5.dp, NavContainerBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NAV_TAB_ITEMS.forEach { item ->
                val isSelected = item.matches(currentRoute)
                val interactionSource = remember { MutableInteractionSource() }

                val animatedBg = animateColorAsState(
                    targetValue = if (isSelected) ActivePillBg else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "tab_bg",
                )
                val animatedPadStart = animateDpAsState(
                    targetValue = if (isSelected) 14.dp else 10.dp,
                    label = "pad_start",
                )
                val animatedPadEnd = animateDpAsState(
                    targetValue = if (isSelected) 14.dp else 10.dp,
                    label = "pad_end",
                )

                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(animatedBg.value, CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            onSelectRoute(item.route)
                        }
                        .padding(start = animatedPadStart.value, end = animatedPadEnd.value),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = item.label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.White,
                            ),
                        )
                    } else {
                        Text(
                            text = item.label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = InactiveText,
                            ),
                        )
                    }
                }
            }
        }
    }
}
