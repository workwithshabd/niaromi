package niaromi.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import niaromi.core.common.util.system.logcat
import niaromi.core.migration.Migration
import niaromi.core.migration.MigrationContext
import niaromi.domain.extension.repository.ExtensionStoreRepository
import niaromi.domain.source.service.SourcePreferences

@Inject
@ContributesIntoSet(AppScope::class)
class TrustExtensionRepositoryMigration(
    private val sourcePreferences: SourcePreferences,
    private val repository: ExtensionStoreRepository,
) : Migration {
    override val version: Float = 7f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withContext(Dispatchers.IO) {
        for ((index, source) in sourcePreferences.extensionRepos.get().withIndex()) {
            try {
                repository.insertFromPreference(
                    indexUrl = source.removeSuffix("/index.min.json").removeSuffix("/index.json") + "/repo.json",
                    name = "Repo #${index + 1}",
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error Migrating Extension Repo with baseUrl: $source" }
            }
        }
        sourcePreferences.extensionRepos.delete()
        return@withContext true
    }
}
