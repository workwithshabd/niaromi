package niaromi.ui.base.delegate

import android.app.Activity
import niaromi.R
import niaromi.app.di.appGraph
import niaromi.domain.ui.model.AppTheme

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_Niaromi))
                if (isAmoled) add(R.style.ThemeOverlay_Niaromi_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = activity.appGraph.uiPreferences
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme.get(), uiPreferences.themeDarkAmoled.get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Niaromi_Monet,
    AppTheme.CATPPUCCIN to R.style.Theme_Niaromi_Catppuccin,
    AppTheme.TOKYONIGHT to R.style.Theme_Niaromi_TokyoNight,
    AppTheme.GREEN_APPLE to R.style.Theme_Niaromi_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_Niaromi_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_Niaromi_MidnightDusk,
    AppTheme.MONOCHROME to R.style.Theme_Niaromi_Monochrome,
    AppTheme.NORD to R.style.Theme_Niaromi_Nord,
    AppTheme.STRAWBERRY_DAIQUIRI to R.style.Theme_Niaromi_StrawberryDaiquiri,
    AppTheme.TAKO to R.style.Theme_Niaromi_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_Niaromi_TealTurquoise,
    AppTheme.YINYANG to R.style.Theme_Niaromi_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_Niaromi_Yotsuba,
    AppTheme.TIDAL_WAVE to R.style.Theme_Niaromi_TidalWave,
)
