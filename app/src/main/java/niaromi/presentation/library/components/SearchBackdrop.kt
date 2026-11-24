package niaromi.presentation.library.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal val SearchHeaderHeight = 76.dp

@Composable
internal fun Modifier.searchBackdrop(scrolled: Boolean): Modifier {
    if (!scrolled) return this

    val original = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val frost = rememberGraphicsLayer()
    val radius = with(LocalDensity.current) { 28.dp.toPx() }
    val effect = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }

    return drawWithContent {
        val band = (SearchHeaderHeight + 48.dp).toPx().coerceAtMost(size.height)
        if (band < 1f || size.width < 1f) {
            drawContent()
            return@drawWithContent
        }
        val strip = IntSize(size.width.toInt(), band.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            original.compositingStrategy = CompositingStrategy.Offscreen
            original.record { this@drawWithContent.drawContent() }
            drawLayer(original)
            blurred.renderEffect = effect
            blurred.scaleX = 1.025f
            blurred.scaleY = 1.045f
            blurred.pivotOffset = Offset(size.width / 2f, 0f)
            blurred.record(size = strip) { drawLayer(original) }
            frost.compositingStrategy = CompositingStrategy.Offscreen
            frost.record(size = strip) {
                drawLayer(blurred)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White,
                        0.55f to Color.White,
                        1f to Color.Transparent,
                        endY = band,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            drawLayer(frost)
        } else {
            original.compositingStrategy = CompositingStrategy.Offscreen
            original.record {
                this@drawWithContent.drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.White,
                        endY = band,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            drawLayer(original)
        }
    }
}
