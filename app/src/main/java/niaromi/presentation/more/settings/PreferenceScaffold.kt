package niaromi.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
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
    val bgColor = MaterialTheme.colorScheme.background
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                backgroundColor = bgColor,
                scrollBehavior = it,
            )
        },
        containerColor = bgColor,
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}
