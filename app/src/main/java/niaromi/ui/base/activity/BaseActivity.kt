package niaromi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import niaromi.app.di.appGraph
import niaromi.ui.base.delegate.SecureActivityDelegate
import niaromi.ui.base.delegate.SecureActivityDelegateImpl
import niaromi.ui.base.delegate.ThemingDelegate
import niaromi.ui.base.delegate.ThemingDelegateImpl
import niaromi.util.system.prepareTabletUiContext

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun attachBaseContext(newBase: Context) {
        val uiPreferences = newBase.appGraph.uiPreferences
        super.attachBaseContext(newBase.prepareTabletUiContext(uiPreferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
