package niaromi.app.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import niaromi.App
import niaromi.core.metro.IsDebugBuild
import niaromi.core.security.PrivacyPreferences
import niaromi.core.security.SecurityPreferences
import niaromi.data.backup.create.BackupCreateWorker
import niaromi.data.backup.google.GoogleDriveService
import niaromi.data.backup.google.GoogleDriveSyncHelper
import niaromi.data.backup.restore.BackupRestoreWorker
import niaromi.data.cache.ChapterCache
import niaromi.data.cache.CoverCache
import niaromi.data.download.DownloadCache
import niaromi.data.download.DownloadManager
import niaromi.data.download.DownloadWorker
import niaromi.data.library.LibraryUpdateWorker
import niaromi.data.library.MetadataUpdateWorker
import niaromi.data.notification.NotificationReceiver
import niaromi.data.track.TrackerManager
import niaromi.data.updater.AppUpdateChecker
import niaromi.domain.backup.service.BackupPreferences
import niaromi.domain.base.BasePreferences
import niaromi.domain.category.interactor.GetCategories
import niaromi.domain.category.interactor.ResetCategoryFlags
import niaromi.domain.chapter.interactor.GetChaptersByMangaId
import niaromi.domain.download.service.DownloadPreferences
import niaromi.domain.extension.interactor.GetExtensionStoreCountAsFlow
import niaromi.domain.extension.interactor.TrustExtension
import niaromi.domain.extension.repository.ExtensionStoreRepository
import niaromi.domain.library.service.LibraryPreferences
import niaromi.domain.manga.interactor.GetFavorites
import niaromi.domain.manga.interactor.NetworkToLocalManga
import niaromi.domain.manga.interactor.ResetViewerFlags
import niaromi.domain.source.service.SourceManager
import niaromi.domain.source.service.SourcePreferences
import niaromi.domain.storage.service.StoragePreferences
import niaromi.domain.track.interactor.AddTracks
import niaromi.domain.track.interactor.UpsertTrack
import niaromi.domain.track.service.DelayedTrackingUpdateWorker
import niaromi.domain.track.service.TrackPreferences
import niaromi.domain.ui.UiPreferences
import niaromi.extension.ExtensionManager
import niaromi.extension.util.ExtensionInstallActivity
import niaromi.ui.base.delegate.SecureActivityDelegateImpl
import niaromi.ui.main.MainActivity
import niaromi.ui.reader.ReaderActivity
import niaromi.ui.reader.setting.ReaderPreferences
import niaromi.ui.setting.track.BaseOAuthLoginActivity
import niaromi.util.CrashLogUtil
import nl.adaptivity.xmlutil.serialization.XML

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [AppBindings::class],
)
interface AppGraph : ViewModelGraph {
    fun inject(app: App)
    fun inject(mainActivity: MainActivity)
    fun inject(readerActivity: ReaderActivity)
    fun inject(baseOAuthLoginActivity: BaseOAuthLoginActivity)
    fun inject(libraryUpdateWorker: LibraryUpdateWorker)
    fun inject(metadataUpdateWorker: MetadataUpdateWorker)
    fun inject(backupRestoreWorker: BackupRestoreWorker)
    fun inject(backupCreateWorker: BackupCreateWorker)
    fun inject(delayedTrackingUpdateWorker: DelayedTrackingUpdateWorker)
    fun inject(downloadWorker: DownloadWorker)
    fun inject(notificationReceiver: NotificationReceiver)
    fun inject(notificationReceiver: SecureActivityDelegateImpl)
    fun inject(extensionInstallActivity: ExtensionInstallActivity)

    val context: Context

    val viewModelFactory: MetroViewModelFactory

    val basePreferences: BasePreferences
    val uiPreferences: UiPreferences
    val readerPreferences: ReaderPreferences
    val networkPreferences: NetworkPreferences
    val libraryPreferences: LibraryPreferences
    val sourcePreferences: SourcePreferences
    val trackPreferences: TrackPreferences
    val backupPreferences: BackupPreferences
    val storagePreferences: StoragePreferences
    val storageManager: niaromi.domain.storage.service.StorageManager
    val privacyPreferences: PrivacyPreferences
    val securityPreferences: SecurityPreferences
    val downloadPreferences: DownloadPreferences

    val crashLogUtil: CrashLogUtil

    val downloadManager: DownloadManager

    val updateChecker: AppUpdateChecker

    val trustExtension: TrustExtension
    val extensionStoreRepository: ExtensionStoreRepository

    val sourceManager: SourceManager
    val trackerManager: TrackerManager
    val extensionManager: ExtensionManager
    val chapterCache: ChapterCache
    val coverCache: CoverCache
    val downloadCache: DownloadCache

    val json: Json
    val protoBuf: ProtoBuf
    val xml: XML
    val networkHelper: NetworkHelper
    val javaScriptEngine: JavaScriptEngine

    val googleDriveService: GoogleDriveService
    val googleDriveSyncHelper: GoogleDriveSyncHelper

    val getFavorites: GetFavorites
    val getCategories: GetCategories
    val getChaptersByMangaId: GetChaptersByMangaId
    val networkToLocalManga: NetworkToLocalManga
    val resetViewerFlags: ResetViewerFlags
    val resetCategoryFlags: ResetCategoryFlags
    val addTracks: AddTracks
    val upsertTrack: UpsertTrack

    val getExtensionStoreCountAsFlow: GetExtensionStoreCountAsFlow

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context, @Provides @IsDebugBuild isDebugBuild: Boolean): AppGraph
    }
}
