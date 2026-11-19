package niaromi.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import niaromi.core.common.i18n.stringResource
import niaromi.domain.category.model.Category
import niaromi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

fun Category.visualName(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }
