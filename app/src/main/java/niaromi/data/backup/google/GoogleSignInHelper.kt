package niaromi.data.backup.google

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.data.backup.google.models.GoogleAccount

object GoogleSignInHelper {

    private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_APPDATA_SCOPE $DRIVE_FILE_SCOPE"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(DRIVE_APPDATA_SCOPE),
                Scope(DRIVE_FILE_SCOPE),
            )
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(context: Context): GoogleAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val email = account.email ?: return null
        return GoogleAccount(
            email = email,
            displayName = account.displayName,
            photoUrl = account.photoUrl?.toString(),
        )
    }

    fun isSignedIn(context: Context): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val email = account.email ?: return@withContext null
        val googleAccount = account.account ?: android.accounts.Account(email, "com.google")
        try {
            GoogleAuthUtil.getToken(context, googleAccount, OAUTH_SCOPE_STRING)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to obtain Google access token" }
            null
        }
    }

    suspend fun clearAndRefreshToken(context: Context, oldToken: String): String? = withContext(Dispatchers.IO) {
        val account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val email = account.email ?: return@withContext null
        val googleAccount = account.account ?: android.accounts.Account(email, "com.google")
        try {
            GoogleAuthUtil.clearToken(context, oldToken)
            GoogleAuthUtil.getToken(context, googleAccount, OAUTH_SCOPE_STRING)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to refresh Google access token" }
            null
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getSignInClient(context).signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
