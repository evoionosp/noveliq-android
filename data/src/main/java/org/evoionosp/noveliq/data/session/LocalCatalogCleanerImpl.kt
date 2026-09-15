package org.evoionosp.noveliq.data.session

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner

/**
 * Drops every catalog table on logout. Uses [NoveliqDatabase.clearAllTables]
 * rather than the per-DAO deletes so a table added in future is wiped by
 * default instead of leaking into the next user's session.
 */
@Singleton
class LocalCatalogCleanerImpl
    @Inject
    constructor(
        private val database: NoveliqDatabase,
        @param:Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : LocalCatalogCleaner {
        override suspend fun clear() {
            // Called directly on the database, never from a DAO transaction —
            // Room rejects clearAllTables() inside one.
            withContext(ioDispatcher) {
                database.clearAllTables()
            }
        }
    }
