package niaromi.crash

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import niaromi.presentation.crash.CrashScreen
import niaromi.ui.base.activity.BaseActivity
import niaromi.ui.main.MainActivity
import niaromi.util.view.setComposeContent

class CrashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setComposeContent {
            CrashScreen(
                exception = exception,
                onRestartClick = {
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                },
            )
        }
    }
}
