package niaromi.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import niaromi.app.di.appGraph
import niaromi.data.track.TrackerManager
import niaromi.presentation.core.screens.LoadingScreen
import niaromi.ui.base.activity.BaseActivity
import niaromi.ui.main.MainActivity
import niaromi.util.view.setComposeContent

@HasMemberInjections
abstract class BaseOAuthLoginActivity : BaseActivity() {

    @Inject protected lateinit var trackerManager: TrackerManager

    abstract fun handleResult(uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appGraph.inject(this)

        setComposeContent {
            LoadingScreen()
        }

        val data = intent.data
        if (data == null) {
            returnToSettings()
        } else {
            handleResult(data)
        }
    }

    internal fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
