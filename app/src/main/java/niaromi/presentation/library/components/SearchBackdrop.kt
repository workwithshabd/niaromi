package niaromi.presentation.library.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal val SearchHeaderHeight = 76.dp

@Composable
internal fun Modifier.searchBackdrop(scrolled: Boolean, background: Color): Modifier {
    if (!scrolled) return this

    val original = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val radius = androidx.compose.ui.platform.LocalDensity.current.run { 12.dp.toPx() }
    val effect = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    return drawWithContent {
        val band = SearchHeaderHeight.toPx().coerceAtMost(size.height)
        if (band <= 0f || size.width <= 0f) {
            drawContent()
            return@drawWithContent
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            original.record { this@drawWithContent.drawContent() }
            blurred.renderEffect = effect
            blurred.record(size = IntSize(size.width.toInt(), band.toInt().coerceAtLeast(1))) {
                drawLayer(original)
            }
            clipRect(top = band) { drawLayer(original) }
            clipRect(bottom = band) { drawLayer(blurred) }
        } else {
            drawContent()
        }
        drawRect(
            brush = Brush.verticalGradient(
                0f to background.copy(alpha = 0.94f),
                0.76f to background.copy(alpha = 0.72f),
                1f to Color.Transparent,
                endY = band,
            ),
            size = size.copy(height = band),
        )
    }
}
