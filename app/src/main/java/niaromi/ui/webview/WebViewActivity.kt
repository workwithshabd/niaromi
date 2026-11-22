package niaromi.ui.webview

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import niaromi.R
import niaromi.core.navigation.LocalAssistUrl
import niaromi.core.navigation.WebViewRoute
import niaromi.ui.base.activity.BaseActivity
import niaromi.util.view.setComposeContent

class WebViewActivity : BaseActivity() {

    private var assistUrl: String? = null

    init {
        registerSecureActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }
        super.onCreate(savedInstanceState)

        val route = intent.getStringExtra(ROUTE_KEY)?.let { Json.decodeFromString<WebViewRoute>(it) } ?: return

        setComposeContent {
            CompositionLocalProvider(LocalAssistUrl provides { assistUrl = it }) {
                WebViewScreen(route, onNavigateUp = ::finish)
            }
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    companion object {
        private const val ROUTE_KEY = "route_key"

        fun newIntent(context: Context, url: String, sourceId: Long? = null, title: String? = null): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(ROUTE_KEY, Json.encodeToString(WebViewRoute(url, title, sourceId)))
            }
        }
    }
}
