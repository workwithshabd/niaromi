package niaromi.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import niaromi.feature.migration.config.MigrationConfigScreen
import niaromi.feature.migration.list.MigrationListScreen
import niaromi.feature.support.SupportUsScreen
import niaromi.feature.upcoming.UpcomingScreen
import niaromi.presentation.category.CategoryListingScreen
import niaromi.presentation.history.HistoryScreen
import niaromi.presentation.more.settings.screen.SettingsAdvancedScreen
import niaromi.presentation.more.settings.screen.SettingsAppearanceScreen
import niaromi.presentation.more.settings.screen.SettingsBrowseScreen
import niaromi.presentation.more.settings.screen.SettingsDataScreen
import niaromi.presentation.more.settings.screen.SettingsDownloadScreen
import niaromi.presentation.more.settings.screen.SettingsLibraryScreen
import niaromi.presentation.more.settings.screen.SettingsMainScreen
import niaromi.presentation.more.settings.screen.SettingsReaderScreen
import niaromi.presentation.more.settings.screen.SettingsSearchScreen
import niaromi.presentation.more.settings.screen.SettingsSecurityScreen
import niaromi.presentation.more.settings.screen.SettingsTrackingScreen
import niaromi.presentation.more.settings.screen.about.AboutScreen
import niaromi.presentation.more.settings.screen.about.OpenSourceLicensesScreen
import niaromi.presentation.more.settings.screen.advanced.ClearDatabaseScreen
import niaromi.presentation.more.settings.screen.appearance.AppLanguageScreen
import niaromi.presentation.more.settings.screen.browse.ExtensionStoresScreen
import niaromi.presentation.more.settings.screen.data.CreateBackupScreen
import niaromi.presentation.more.settings.screen.data.RestoreBackupScreen
import niaromi.presentation.more.settings.screen.debug.BackupSchemaScreen
import niaromi.presentation.more.settings.screen.debug.DebugInfoScreen
import niaromi.presentation.more.settings.screen.debug.WorkerInfoScreen
import niaromi.ui.browse.extension.ExtensionFilterScreen
import niaromi.ui.browse.extension.details.ExtensionDetailsScreen
import niaromi.ui.browse.extension.details.SourcePreferencesScreen
import niaromi.ui.browse.migration.manga.MigrateMangaScreen
import niaromi.ui.browse.migration.search.MigrateSearchScreen
import niaromi.ui.browse.migration.search.MigrateSourceSearchScreen
import niaromi.ui.browse.source.SourcesFilterScreen
import niaromi.ui.browse.source.browse.BrowseSourceScreen
import niaromi.ui.browse.source.globalsearch.GlobalSearchScreen
import niaromi.ui.category.CategoryScreen
import niaromi.ui.deeplink.DeepLinkScreen
import niaromi.ui.download.DownloadQueueScreen
import niaromi.ui.home.HomeScreen
import niaromi.ui.manga.MangaScreen
import niaromi.ui.manga.notes.MangaNotesScreen
import niaromi.ui.manga.track.TrackInfoDialog
import niaromi.ui.more.NewUpdateScreen
import niaromi.ui.more.OnboardingScreen
import niaromi.ui.stats.StatsScreen
import niaromi.ui.webview.WebViewScreen

fun EntryProviderScope<NavKey>.appEntries() {
    entry<HomeRoute> {
        HomeScreen()
    }
    entry<DeepLinkRoute> { route ->
        DeepLinkScreen(route.query)
    }
    entry<WebViewRoute> { route ->
        val navigator = LocalNavigator.current
        LocalAssistContentManager.current.Provide(route) {
            WebViewScreen(route, onNavigateUp = navigator::pop)
        }
    }

    entry<MangaNotesRoute> { route ->
        MangaNotesScreen(route.mangaId)
    }
    entry<MangaRoute> { route ->
        LocalAssistContentManager.current.Provide(route) {
            MangaScreen(route.mangaId, route.fromSource, route.fromSearch, route.categoryName)
        }
    }
    entry<TrackInfoDialogRoute>(metadata = OverlaySheetScene.overlaySheet()) { route ->
        val navigator = LocalNavigator.current
        TrackInfoDialog(
            mangaId = route.mangaId,
            mangaTitle = route.mangaTitle,
            sourceId = route.sourceId,
            onDismissRequest = { navigator.pop() },
        )
    }

    entry<GlobalSearchRoute> { route ->
        GlobalSearchScreen(route.searchQuery, route.extensionFilter)
    }

    entry<CategoryListingRoute> { route ->
        CategoryListingScreen(route.categoryId, route.categoryName)
    }

    entry<UpcomingRoute> {
        UpcomingScreen()
    }

    entry<BrowseSourceRoute> { route ->
        LocalAssistContentManager.current.Provide(route) {
            BrowseSourceScreen(route.sourceId, route.listingQuery)
        }
    }
    entry<ExtensionDetailsRoute> { route ->
        ExtensionDetailsScreen(route.pkgName)
    }
    entry<ExtensionFilterRoute> {
        ExtensionFilterScreen()
    }
    entry<ExtensionMigrationRoute> {
        niaromi.presentation.more.settings.screen.browse.ExtensionMigrationScreen()
    }
    entry<MigrateMangaRoute> { route ->
        MigrateMangaScreen(route.sourceId)
    }
    entry<MigrateSearchRoute> { route ->
        MigrateSearchScreen(route.mangaId)
    }
    entry<MigrateSourceSearchRoute> { route ->
        MigrateSourceSearchScreen(route.currentMangaId, route.sourceId, route.query)
    }
    entry<MigrationConfigRoute> { route ->
        MigrationConfigScreen(route.mangaIds)
    }
    entry<MigrationListRoute> { route ->
        MigrationListScreen(route.mangaIds, route.extraSearchQuery)
    }
    entry<SourcesFilterRoute> {
        SourcesFilterScreen()
    }
    entry<SourcePreferencesRoute> { route ->
        SourcePreferencesScreen(route.sourceId)
    }

    entry<CategoryRoute> {
        CategoryScreen()
    }
    entry<DownloadQueueRoute> {
        DownloadQueueScreen()
    }
    entry<HistoryRoute>(metadata = TwoPaneSettingsScene.fullScreen()) {
        HistoryScreen()
    }
    entry<NewUpdateRoute>(metadata = TwoPaneSettingsScene.fullScreen()) { route ->
        NewUpdateScreen(
            versionName = route.versionName,
            changelogInfo = route.changelogInfo,
            releaseLink = route.releaseLink,
            downloadLink = route.downloadLink,
        )
    }
    entry<OnboardingRoute> {
        OnboardingScreen()
    }
    entry<SettingsMainScreen>(metadata = TwoPaneSettingsScene.listPane()) {
        SettingsMainScreen.Content()
    }
    entry<StatsRoute> {
        StatsScreen()
    }
    entry<SupportUsRoute>(metadata = TwoPaneSettingsScene.fullScreen()) {
        SupportUsScreen()
    }

    entry<AboutRoute> {
        AboutScreen()
    }
    entry<AppLanguageRoute> {
        AppLanguageScreen()
    }
    entry<BackupSchemaRoute> {
        BackupSchemaScreen()
    }
    entry<ClearDatabaseRoute> {
        ClearDatabaseScreen()
    }
    entry<CreateBackupRoute> {
        CreateBackupScreen()
    }
    entry<DebugInfoRoute> {
        DebugInfoScreen()
    }
    entry<ExtensionStoresRoute> {
        ExtensionStoresScreen()
    }
    entry<OpenSourceLicensesRoute> {
        OpenSourceLicensesScreen()
    }
    entry<RestoreBackupRoute> { route ->
        RestoreBackupScreen(route.uri)
    }
    entry<SettingsSearchRoute> {
        SettingsSearchScreen()
    }
    entry<WorkerInfoRoute> {
        WorkerInfoScreen()
    }

    entry<SettingsAdvancedScreen> {
        SettingsAdvancedScreen.Content()
    }
    entry<SettingsAppearanceScreen> {
        SettingsAppearanceScreen.Content()
    }
    entry<SettingsBrowseScreen> {
        SettingsBrowseScreen.Content()
    }
    entry<SettingsDataScreen> {
        SettingsDataScreen.Content()
    }
    entry<SettingsDownloadScreen> {
        SettingsDownloadScreen.Content()
    }
    entry<SettingsLibraryScreen> {
        SettingsLibraryScreen.Content()
    }
    entry<SettingsReaderScreen> {
        SettingsReaderScreen.Content()
    }
    entry<SettingsSecurityScreen> {
        SettingsSecurityScreen.Content()
    }
    entry<SettingsTrackingScreen> {
        SettingsTrackingScreen.Content()
    }
}
