package niaromi.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.Serializable
import niaromi.app.di.appGraph
import niaromi.core.common.i18n.stringResource
import niaromi.core.navigation.ExtensionStoresRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.domain.extension.model.ContentWarning
import niaromi.presentation.core.i18n.pluralStringResource
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.more.settings.Preference
import niaromi.util.system.AuthenticatorUtil.authenticate
import tachiyomi.i18n.MR

@Serializable
data object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.current

        val sourcePreferences = remember { context.appGraph.sourcePreferences }
        val getExtensionStoreCountAsFlow = remember { context.appGraph.getExtensionStoreCountAsFlow }

        val reposCount by getExtensionStoreCountAsFlow().collectAsState(0)

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems,
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.extensionStores),
                        subtitle = pluralStringResource(MR.plurals.num_repos, reposCount.toInt(), reposCount),
                        onClick = {
                            navigator.push(ExtensionStoresRoute)
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_extensions),
                preferenceItems = listOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = sourcePreferences.enabledContentWarnings,
                        entries = mapOf(
                            ContentWarning.SAFE to stringResource(MR.strings.ext_content_warning_safe),
                            ContentWarning.MIXED to stringResource(MR.strings.ext_content_warning_mixed),
                            ContentWarning.NSFW to stringResource(MR.strings.ext_content_warning_nsfw),
                        ),
                        title = stringResource(MR.strings.pref_allowed_content_warnings),
                        subtitleProvider = { value, entries ->
                            remember(value, entries) {
                                entries.filterKeys { it in value }.values.joinToString()
                            }
                                .takeUnless { it.isBlank() }
                                ?: stringResource(MR.strings.none)
                        },
                        onValueChanged = { newValue ->
                            val added = newValue - sourcePreferences.enabledContentWarnings.get()
                            if (added.any { it != ContentWarning.SAFE }) {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(MR.strings.pref_allowed_content_warnings),
                                )
                            } else {
                                true
                            }
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.applyContentWarningsToInstalled,
                        title = stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                        subtitle = stringResource(MR.strings.pref_apply_content_warnings_to_installed_summary),
                        onValueChanged = { newValue ->
                            if (newValue) {
                                true
                            } else {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(
                                        MR.strings.pref_apply_content_warnings_to_installed,
                                    ),
                                )
                            }
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.content_warnings_info)),
                ),
            ),
        )
    }
}
