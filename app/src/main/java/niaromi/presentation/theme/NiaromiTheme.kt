package niaromi.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import niaromi.app.di.appGraph
import niaromi.domain.ui.model.AppTheme
import niaromi.domain.ui.model.ThemeMode
import niaromi.presentation.core.util.collectAsState
import niaromi.presentation.theme.colorscheme.BaseColorScheme
import niaromi.presentation.theme.colorscheme.CatppuccinColorScheme
import niaromi.presentation.theme.colorscheme.GreenAppleColorScheme
import niaromi.presentation.theme.colorscheme.LavenderColorScheme
import niaromi.presentation.theme.colorscheme.MidnightDuskColorScheme
import niaromi.presentation.theme.colorscheme.MonetColorScheme
import niaromi.presentation.theme.colorscheme.MonochromeColorScheme
import niaromi.presentation.theme.colorscheme.NeutralColorScheme
import niaromi.presentation.theme.colorscheme.NiaromiColorScheme
import niaromi.presentation.theme.colorscheme.NordColorScheme
import niaromi.presentation.theme.colorscheme.StrawberryColorScheme
import niaromi.presentation.theme.colorscheme.TakoColorScheme
import niaromi.presentation.theme.colorscheme.TealTurqoiseColorScheme
import niaromi.presentation.theme.colorscheme.TidalWaveColorScheme
import niaromi.presentation.theme.colorscheme.TokyoNightColorScheme
import niaromi.presentation.theme.colorscheme.YinYangColorScheme
import niaromi.presentation.theme.colorscheme.YotsubaColorScheme

@Composable
fun NiaromiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    val themeMode by uiPreferences.themeMode.collectAsState()
    val appThemePref by uiPreferences.appTheme.collectAsState()
    val amoledPref by uiPreferences.themeDarkAmoled.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    BaseNiaromiTheme(
        appTheme = appTheme ?: appThemePref,
        isDark = isDark,
        isAmoled = amoled ?: amoledPref,
        content = content,
    )
}

@Composable
fun NiaromiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseNiaromiTheme(
    appTheme = appTheme,
    isDark = isSystemInDarkTheme(),
    isAmoled = isAmoled,
    content = content,
)

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) = NiaromiTheme(appTheme, amoled, content)

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = NiaromiPreviewTheme(appTheme, isAmoled, content)

@Composable
private fun BaseNiaromiTheme(
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled) {
            getThemeColorScheme(
                context = context,
                appTheme = appTheme,
                isDark = isDark,
                isAmoled = isAmoled,
            )
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, NiaromiColorScheme)
    }
    val scheme = colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
    val accent = when (appTheme) {
        AppTheme.BLACK -> Color.Black
        AppTheme.WHITE -> Color.White
        AppTheme.DARK_GREY -> Color(0xFF303030)
        AppTheme.OFF_WHITE -> Color(0xFFF5F1E8)
        else -> return scheme
    }
    val onAccent = if (accent.luminance() < 0.5f) Color.White else Color.Black
    return scheme.copy(
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        secondaryContainer = accent,
        onSecondaryContainer = onAccent,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to NiaromiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.BLACK to MonochromeColorScheme,
    AppTheme.WHITE to NeutralColorScheme,
    AppTheme.DARK_GREY to NeutralColorScheme,
    AppTheme.OFF_WHITE to NeutralColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
