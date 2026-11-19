package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import niaromi.util.system.toast as niaromiToast

fun Context.toast(
    resource: StringResource,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast = niaromiToast(resource, duration, block)

fun Context.toast(
    text: String?,
    duration: Int = Toast.LENGTH_SHORT,
    block: (Toast) -> Unit = {},
): Toast = niaromiToast(text, duration, block)
