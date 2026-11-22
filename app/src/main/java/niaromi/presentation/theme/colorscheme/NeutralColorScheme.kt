package niaromi.presentation.theme.colorscheme

import androidx.compose.ui.graphics.Color

internal object NeutralColorScheme : BaseColorScheme() {

    override val lightScheme = MonochromeColorScheme.lightScheme.copy(
        background = Color(0xFFF7F7F7),
        surface = Color(0xFFF7F7F7),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F1F1),
        surfaceContainer = Color(0xFFEBEBEB),
        surfaceContainerHigh = Color(0xFFE5E5E5),
        surfaceContainerHighest = Color(0xFFDFDFDF),
        outlineVariant = Color(0xFFC6C6C6),
    )

    override val darkScheme = MonochromeColorScheme.darkScheme.copy(
        background = Color(0xFF171717),
        surface = Color(0xFF171717),
        onBackground = Color(0xFFE6E6E6),
        onSurface = Color(0xFFE6E6E6),
        surfaceVariant = Color(0xFF343434),
        onSurfaceVariant = Color(0xFFC7C7C7),
        surfaceDim = Color(0xFF171717),
        surfaceBright = Color(0xFF3B3B3B),
        surfaceContainerLowest = Color(0xFF111111),
        surfaceContainerLow = Color(0xFF1D1D1D),
        surfaceContainer = Color(0xFF242424),
        surfaceContainerHigh = Color(0xFF2B2B2B),
        surfaceContainerHighest = Color(0xFF353535),
        outline = Color(0xFF909090),
        outlineVariant = Color(0xFF484848),
    )
}
