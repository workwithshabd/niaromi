package niaromi.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import niaromi.presentation.components.AppBar
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                backgroundColor = androidx.compose.ui.graphics.Color(0xFFFFFDF4),
                scrollBehavior = it,
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFFFFFDF4),
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}
