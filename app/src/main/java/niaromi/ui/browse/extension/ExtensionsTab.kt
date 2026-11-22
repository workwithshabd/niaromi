package niaromi.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import niaromi.app.di.appGraph
import niaromi.core.navigation.ExtensionDetailsRoute
import niaromi.core.navigation.ExtensionFilterRoute
import niaromi.core.navigation.ExtensionStoresRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.core.navigation.WebViewRoute
import niaromi.extension.model.Extension
import niaromi.presentation.browse.ExtensionScreen
import niaromi.presentation.browse.PrivateExtensionUninstallDialog
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.TabContent
import niaromi.presentation.core.i18n.stringResource
import niaromi.presentation.more.settings.widget.MultiSelectListPreferenceDialog
import niaromi.util.system.isPackageInstalled
import tachiyomi.i18n.MR

@Composable
fun extensionsTab(
    extensionsViewModel: ExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    val updatesCount by extensionsViewModel.updatesCount.collectAsStateWithLifecycle()
    var showPreferredExtensions by remember { mutableStateOf(false) }
    var privateExtensionToUninstall by remember { mutableStateOf<Extension.Installed?>(null) }

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = updatesCount.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_filter),
                onClick = { navigator.push(ExtensionFilterRoute) },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.extensionStores),
                onClick = { navigator.push(ExtensionStoresRoute) },
            ),
        ),
        content = { contentPadding, _ ->
            val state by extensionsViewModel.state.collectAsStateWithLifecycle()

            BackHandler(enabled = state.searchQuery != null) {
                extensionsViewModel.search(null)
            }

            Column(Modifier.fillMaxSize()) {
                TextButton(onClick = { showPreferredExtensions = true }) {
                    Text("Preferred extensions")
                }
                Box(Modifier.weight(1f)) {
                    ExtensionScreen(
                        state = state,
                        contentPadding = contentPadding,
                        searchQuery = state.searchQuery,
                        onLongClickItem = { extension ->
                            when (extension) {
                                is Extension.Available -> extensionsViewModel.installExtension(extension)
                                is Extension.Installed -> {
                                    if (context.isPackageInstalled(extension.pkgName)) {
                                        extensionsViewModel.uninstallExtension(extension)
                                    } else {
                                        privateExtensionToUninstall = extension
                                    }
                                }
                            }
                        },
                        onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
                        onClickUpdateAll = extensionsViewModel::updateAllExtensions,
                        onOpenWebView = { extension ->
                            extension.sources.getOrNull(0)?.let {
                                navigator.push(
                                    WebViewRoute(
                                        url = it.baseUrl,
                                        initialTitle = it.name,
                                        sourceId = it.id,
                                    ),
                                )
                            }
                        },
                        onInstallExtension = extensionsViewModel::installExtension,
                        onOpenExtension = { navigator.push(ExtensionDetailsRoute(it.pkgName)) },
                        onTrustExtension = { extensionsViewModel.trustExtension(it) },
                        onUninstallExtension = { extensionsViewModel.uninstallExtension(it) },
                        onUpdateExtension = extensionsViewModel::updateExtension,
                        onRefresh = extensionsViewModel::findAvailableExtensions,
                        onTogglePreferred = extensionsViewModel::togglePreferredExtension,
                    )
                }
            }

            val graph = context.appGraph
            val loaded by graph.extensionManager.loadedExtensionsFlow.collectAsStateWithLifecycle(null)
            val extensions = loaded
            if (showPreferredExtensions && extensions != null) {
                val preferred = graph.sourcePreferences.preferredExtensions
                val entries = remember(extensions) {
                    extensions.sortedBy { it.name.lowercase() }.associate { it.pkgName to it.name }
                }
                MultiSelectListPreferenceDialog(
                    title = "Preferred extensions",
                    values = preferred.get(),
                    entries = entries,
                    onValuesChange = { if (entries.isNotEmpty()) preferred.set(it) },
                    onDismissRequest = { showPreferredExtensions = false },
                    message = if (entries.isEmpty()) {
                        "Install an extension to choose it here."
                    } else {
                        "Search these extensions first. Leave all unchecked to start with all sources."
                    },
                )
            }

            privateExtensionToUninstall?.let { extension ->
                PrivateExtensionUninstallDialog(
                    extension = extension,
                    onConfirm = {
                        extensionsViewModel.uninstallExtension(extension)
                    },
                    onDismissRequest = {
                        privateExtensionToUninstall = null
                    },
                )
            }
        },
    )
}
