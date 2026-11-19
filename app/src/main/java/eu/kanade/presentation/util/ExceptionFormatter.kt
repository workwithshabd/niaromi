package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.MR
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

context(context: Context)
val Throwable.formattedMessage: String
    get() {
        val msg = message.orEmpty()

        if (this is SocketTimeoutException || this is InterruptedIOException ||
            msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true)
        ) {
            return "Too slow internet or connection timed out. Please check your network and try again."
        }

        if (this is UnknownHostException || this is ConnectException) {
            return if (!context.isOnline()) {
                context.stringResource(MR.strings.exception_offline)
            } else {
                "Unable to connect to source server. Too slow internet or server is offline. Try again later."
            }
        }

        if (this is HttpException) {
            return when (code) {
                404, 410 -> "Chapter unavailable or moved on source. Try re-adding title or selecting another source."
                403, 503 -> "Source protected by Cloudflare or captcha. Open in WebView to solve."
                else -> context.stringResource(MR.strings.exception_http, code)
            }
        }

        if (this is NoChaptersException) {
            return "No chapters found for this title. Please re-add title or check other sources."
        }

        if (this is NoResultsException) {
            return context.stringResource(MR.strings.no_results_found)
        }

        if (this is SourceNotInstalledException || msg.contains("Source not installed", ignoreCase = true) ||
            msg.contains("is not installed", ignoreCase = true)
        ) {
            return "Source extension is not installed. Please reinstall the extension or migrate this title."
        }

        if (msg.contains("page_list_empty", ignoreCase = true) ||
            msg.contains("No pages found", ignoreCase = true) ||
            msg.contains("Page list is empty", ignoreCase = true)
        ) {
            return "Chapter unavailable or has no pages on source. Try re-adding title or selecting another source."
        }

        if (this is NoSuchElementException || msg.contains("not found in chapter list", ignoreCase = true) ||
            msg.contains("no element matching", ignoreCase = true)
        ) {
            return "Chapter unavailable. Please re-add title or refresh chapters."
        }

        if (msg.contains("cloudflare", ignoreCase = true) || msg.contains("captcha", ignoreCase = true)) {
            return "Source protected by Cloudflare or captcha. Open in WebView to solve, or select another source."
        }

        if (msg.contains("404", ignoreCase = true) || msg.contains("not found", ignoreCase = true)) {
            return "Chapter unavailable or moved on source. Try re-adding title or selecting another source."
        }

        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> if (msg.isNotBlank()) msg else "Failed to open chapter."
            else -> if (msg.isNotBlank()) "$className: $msg" else "Failed to open chapter: $className"
        }
    }
