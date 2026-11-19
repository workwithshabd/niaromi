package tachiyomi.domain.storage.service

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import niaromi.core.metro.AppCoroutineScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Inject
@SingleIn(AppScope::class)
class StorageManager(
    @AppCoroutineScope private val scope: CoroutineScope,
    private val context: Context,
    storagePreferences: StoragePreferences,
) {

    @Volatile
    private var baseDirLazy: Lazy<UniFile?> = lazy {
        var uri = storagePreferences.baseStorageDirectory.get()
        if (uri.isBlank()) {
            val autoDir = autoResolveOrCreateDirectory(context)
            if (autoDir != null) {
                uri = Uri.fromFile(autoDir).toString()
                storagePreferences.baseStorageDirectory.set(uri)
            }
        }
        getBaseDir(uri)
    }
    private val baseDir: UniFile?
        get() = baseDirLazy.value

    // On SAF, createDirectory lists the whole base directory to find an existing child, so a resolved child
    // is reused while it's under the current base directory and still exists; it can be deleted outside the app
    private val childDirectories = ConcurrentHashMap<String, Pair<UniFile, UniFile>>()

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        if (storagePreferences.baseStorageDirectory.get().isBlank()) {
            val autoDir = autoResolveOrCreateDirectory(context)
            if (autoDir != null) {
                storagePreferences.baseStorageDirectory.set(Uri.fromFile(autoDir).toString())
            }
        }

        storagePreferences.baseStorageDirectory.changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach { uri ->
                baseDirLazy = lazyOf(getBaseDir(uri))
                baseDir?.let { parent ->
                    (parent.findFile(AUTOMATIC_BACKUPS_PATH) ?: parent.createDirectory(AUTOMATIC_BACKUPS_PATH))
                    (parent.findFile(LOCAL_SOURCE_PATH) ?: parent.createDirectory(LOCAL_SOURCE_PATH))
                    (parent.findFile(DOWNLOADS_PATH) ?: parent.createDirectory(DOWNLOADS_PATH))?.also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun getBaseDir(uri: String): UniFile? {
        val parsedUri = uri.toUri()
        val file = resolveToFile(context, parsedUri)
        if (file != null && file.canWrite()) {
            return UniFile.fromFile(file)
        }
        return UniFile.fromUri(context, parsedUri)
            ?.takeIf { it.exists() }
    }

    fun getAutomaticBackupsDirectory(): UniFile? {
        return getChildDirectory(AUTOMATIC_BACKUPS_PATH)
    }

    fun getDownloadsDirectory(): UniFile? {
        return getChildDirectory(DOWNLOADS_PATH)
    }

    fun getLocalSourceDirectory(): UniFile? {
        return getChildDirectory(LOCAL_SOURCE_PATH)
    }

    private fun getChildDirectory(name: String): UniFile? {
        val parent = baseDir ?: return null
        childDirectories[name]
            ?.takeIf { (cachedParent, directory) -> cachedParent === parent && directory.exists() }
            ?.let { (_, directory) -> return directory }
        val directory = (parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name))
            ?.let { ensureFileBacked(it, name) }
            ?.also { childDirectories[name] = parent to it }
        return directory
    }

    private fun ensureFileBacked(uniFile: UniFile, name: String): UniFile {
        val file = resolveToFile(context, uniFile.uri)
        if (file != null && file.canWrite()) {
            return UniFile.fromFile(file) ?: uniFile
        }
        val parentFile = baseDir?.let { resolveToFile(context, it.uri) }
        if (parentFile != null && parentFile.canWrite()) {
            val child = File(parentFile, name)
            if (child.exists() && child.canWrite()) {
                return UniFile.fromFile(child) ?: uniFile
            }
        }
        return uniFile
    }

    private fun resolveToFile(context: Context, uri: Uri): File? {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path ?: return null
            return File(path).takeIf { it.exists() && it.canWrite() }
        }
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val docId = try {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    DocumentsContract.getDocumentId(uri)
                } else {
                    DocumentsContract.getTreeDocumentId(uri)
                }
            } catch (_: Exception) {
                uri.lastPathSegment
            } ?: return null

            val decodedDocId = Uri.decode(docId)
            if (decodedDocId.startsWith("raw:")) {
                val path = decodedDocId.removePrefix("raw:")
                val file = File(path)
                if (file.exists() && file.canWrite()) return file
            }
            if (uri.authority == "com.android.providers.downloads.documents") {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists() && downloadDir.canWrite()) {
                    if (decodedDocId == "downloads") return downloadDir
                    val subName = decodedDocId.substringAfterLast("/")
                    val subFile = File(downloadDir, subName)
                    if (subFile.exists() && subFile.canWrite()) return subFile
                    return downloadDir
                }
            }
            if (uri.authority == "com.android.externalstorage.documents") {
                val parts = decodedDocId.split(":")
                if (parts.size >= 2) {
                    val type = parts[0]
                    val relPath = parts[1]
                    if (type.equals("primary", ignoreCase = true)) {
                        val file = File(Environment.getExternalStorageDirectory(), relPath)
                        if (file.exists() && file.canWrite()) return file
                    }
                }
            }
        }
        return null
    }
}

private const val AUTOMATIC_BACKUPS_PATH = "autobackup"
private const val DOWNLOADS_PATH = "downloads"
private const val LOCAL_SOURCE_PATH = "local"

fun autoResolveOrCreateDirectory(context: Context): File? {
    val candidates = listOf(
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Niaromi"),
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Niaromi"),
        File(Environment.getExternalStorageDirectory(), "Niaromi"),
        File(Environment.getExternalStorageDirectory(), "Mihon"),
        File(Environment.getExternalStorageDirectory(), "Tachiyomi"),
        context.getExternalFilesDir(null)?.let { File(it, "Niaromi") },
    ).filterNotNull()

    for (candidate in candidates) {
        if (candidate.exists() && candidate.isDirectory && candidate.canWrite()) {
            return candidate
        }
    }

    val createTargets = listOf(
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Niaromi"),
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Niaromi"),
        File(Environment.getExternalStorageDirectory(), "Niaromi"),
        context.getExternalFilesDir(null)?.let { File(it, "Niaromi") },
    ).filterNotNull()

    for (target in createTargets) {
        try {
            if (!target.exists()) {
                target.mkdirs()
            }
            if (target.exists() && target.isDirectory && target.canWrite()) {
                File(target, AUTOMATIC_BACKUPS_PATH).mkdirs()
                File(target, DOWNLOADS_PATH).mkdirs()
                File(target, LOCAL_SOURCE_PATH).mkdirs()
                return target
            }
        } catch (_: Exception) {
        }
    }
    return null
}
