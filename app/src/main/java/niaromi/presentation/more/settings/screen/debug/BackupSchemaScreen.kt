package niaromi.presentation.more.settings.screen.debug

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import niaromi.core.navigation.BackupSchemaRoute
import niaromi.core.navigation.LocalNavigator
import niaromi.data.backup.models.Backup
import niaromi.icons.materialsymbols.MaterialSymbols
import niaromi.icons.materialsymbols.rounded.ContentCopy
import niaromi.presentation.components.AppBar
import niaromi.presentation.components.AppBarActions
import niaromi.presentation.core.components.material.Scaffold
import niaromi.presentation.core.i18n.stringResource
import niaromi.util.system.copyToClipboard
import tachiyomi.i18n.MR

@Composable
fun BackupSchemaScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val schema = remember { ProtoBufSchemaGenerator.generateSchemaText(Backup.serializer().descriptor) }

    Scaffold(
        topBar = {
            AppBar(
                title = BackupSchemaRoute.TITLE,
                navigateUp = navigator::pop,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_copy_to_clipboard),
                                icon = MaterialSymbols.Rounded.ContentCopy,
                                onClick = {
                                    context.copyToClipboard(BackupSchemaRoute.TITLE, schema)
                                },
                            ),
                        ),
                    )
                },
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        Text(
            text = schema,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(16.dp),
            fontFamily = FontFamily.Monospace,
        )
    }
}
