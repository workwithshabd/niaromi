package niaromi.presentation.more.settings.screen.about

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import niaromi.BuildConfig
import niaromi.app.di.appGraph
import niaromi.core.common.Constants
import niaromi.core.common.util.system.logcat
import niaromi.core.navigation.LocalBackButtonVisibility
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.NewUpdateRoute
import niaromi.core.navigation.OpenSourceLicensesRoute
import niaromi.data.updater.RELEASE_URL
import niaromi.domain.release.interactor.GetApplicationRelease
import niaromi.domain.ui.UiPreferences
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.Public
import niaromi.icons.simpleicons.Discord
import niaromi.icons.simpleicons.Facebook
import niaromi.icons.simpleicons.Github
import niaromi.icons.simpleicons.Reddit
import niaromi.icons.simpleicons.SimpleIcons
import niaromi.icons.simpleicons.X
import niaromi.presentation.components.AppBar
import niaromi.presentation.core.components.LinkIcon
import niaromi.presentation.core.components.ScrollbarLazyColumn
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.more.LogoHeader
import niaromi.presentation.more.settings.widget.TextPreferenceWidget
import niaromi.presentation.util.rememberIs24HourSystem
import niaromi.util.lang.toDateTimestampString
import niaromi.util.system.copyToClipboard
import niaromi.util.system.isFossBuildType
import niaromi.util.system.isNightlyBuildType
import niaromi.util.system.toast
import niaromi.util.system.updaterEnabled
import tachiyomi.i18n.MR
import java.util.Locale
import kotlin.time.Instant

@Composable
fun AboutScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val backVisibility = LocalBackButtonVisibility.current
    val navigator = LocalNavigator.current
    var isCheckingUpdates by remember { mutableStateOf(false) }
    val crashLogUtil = remember { context.appGraph.crashLogUtil }
    val is24HourSystem = rememberIs24HourSystem()
    val bgColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.pref_category_about),
                navigateUp = {
                    navigator.pop()
                    Unit
                }.takeIf { backVisibility },
                backgroundColor = bgColor,
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = bgColor,
    ) { contentPadding ->
        ScrollbarLazyColumn(
            contentPadding = contentPadding,
        ) {
            item {
                LogoHeader(
                    iconPadding = PaddingValues(vertical = 24.dp),
                )
            }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.version),
                    // Keyed on the system 12/24h setting, which can change while the app is backgrounded.
                    // Date/time prefs only change in Appearance, which recreates this screen on return.
                    subtitle = remember(is24HourSystem) {
                        getVersionName(buildTime = getFormattedBuildTime(context))
                    },
                    onPreferenceClick = {
                        val deviceInfo = crashLogUtil.getDebugInfo()
                        context.copyToClipboard("Debug information", deviceInfo)
                    },
                )
            }

            if (updaterEnabled) {
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.check_for_updates),
                        widget = {
                            AnimatedVisibility(visible = isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp,
                                )
                            }
                        },
                        onPreferenceClick = {
                            if (!isCheckingUpdates) {
                                scope.launch {
                                    isCheckingUpdates = true

                                    checkVersion(
                                        context = context,
                                        onAvailableUpdate = { result ->
                                            val updateRoute = NewUpdateRoute(
                                                versionName = result.release.version,
                                                changelogInfo = result.release.info,
                                                releaseLink = result.release.releaseLink,
                                                downloadLink = result.release.downloadLink,
                                            )
                                            navigator.push(updateRoute)
                                        },
                                        onFinish = {
                                            isCheckingUpdates = false
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }

            if (!BuildConfig.DEBUG) {
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.whats_new),
                        onPreferenceClick = { uriHandler.openUri(RELEASE_URL) },
                    )
                }
            }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.licenses),
                    onPreferenceClick = { navigator.push(OpenSourceLicensesRoute) },
                )
            }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.privacy_policy),
                    onPreferenceClick = { uriHandler.openUri("https://github.com/workwithshabd/niaromi") },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        20.dp,
                        androidx.compose.ui.Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    LinkIcon(
                        label = "X",
                        icon = SimpleIcons.X,
                        url = "https://x.com/workwithshabd",
                    )
                    LinkIcon(
                        label = "Discord",
                        icon = SimpleIcons.Discord,
                        url = "https://discord.com/users/workwithshabd",
                    )
                    LinkIcon(
                        label = "GitHub",
                        icon = SimpleIcons.Github,
                        url = "https://github.com/workwithshabd",
                    )
                }
            }
        }
    }
}

/**
 * Checks version and shows a user prompt if an update is available.
 */
private suspend fun checkVersion(
    context: Context,
    onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
    onFinish: () -> Unit,
) {
    val updateChecker = context.appGraph.updateChecker
    withContext(Dispatchers.Main) {
        try {
            when (val result = updateChecker.checkForUpdate(forceCheck = true)) {
                is GetApplicationRelease.Result.NewUpdate -> {
                    onAvailableUpdate(result)
                }
                is GetApplicationRelease.Result.NoNewUpdate -> {
                    context.toast(MR.strings.update_check_no_new_updates)
                }
                is GetApplicationRelease.Result.OsTooOld -> {
                    context.toast(MR.strings.update_check_eol)
                }
            }
        } catch (e: Exception) {
            context.toast(e.message)
            logcat(LogPriority.ERROR, e)
        } finally {
            onFinish()
        }
    }
}

fun getVersionName(buildTime: String?): String {
    return when {
        BuildConfig.DEBUG -> {
            "Debug ${BuildConfig.COMMIT_SHA}".let {
                if (buildTime != null) {
                    "$it ($buildTime)"
                } else {
                    it
                }
            }
        }
        isNightlyBuildType -> {
            "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                if (buildTime != null) {
                    "$it (${BuildConfig.COMMIT_SHA}, $buildTime)"
                } else {
                    "$it (${BuildConfig.COMMIT_SHA})"
                }
            }
        }
        else -> {
            val channel = if (isFossBuildType) "FOSS" else "Stable"
            "$channel v${BuildConfig.VERSION_NAME}".let {
                if (buildTime != null) {
                    "$it ($buildTime)"
                } else {
                    it
                }
            }
        }
    }
}

internal fun getFormattedBuildTime(context: Context): String {
    return try {
        val uiPreferences = context.appGraph.uiPreferences
        Instant.parse(BuildConfig.BUILD_TIME)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toDateTimestampString(
                UiPreferences.dateTimeFormat(
                    uiPreferences.dateFormat.get(),
                    uiPreferences.timeFormat.get(),
                    Locale.getDefault(),
                    DateFormat.is24HourFormat(context),
                ),
            )
    } catch (_: Exception) {
        BuildConfig.BUILD_TIME
    }
}
