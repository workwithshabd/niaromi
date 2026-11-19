package niaromi.core.migration.migrations

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import niaromi.core.migration.Migration
import niaromi.core.migration.MigrationContext
import niaromi.data.library.LibraryUpdateWorker

@Inject
@ContributesIntoSet(AppScope::class)
class SetupLibraryUpdateMigration(
    private val context: Context,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        LibraryUpdateWorker.setupTask(context)
        return true
    }
}
