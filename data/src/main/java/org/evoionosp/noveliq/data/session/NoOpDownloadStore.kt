package org.evoionosp.noveliq.data.session

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.domain.session.DownloadStore

/**
 * Placeholder until downloads ship: there is nothing on device to delete, so
 * logout's download step is a no-op. The future implementation replaces this
 * binding and deletes every downloaded file; the logout flow needs no change.
 */
@Singleton
class NoOpDownloadStore
    @Inject
    constructor() : DownloadStore {
        override suspend fun deleteAll() {
            // No downloads exist yet — nothing to delete.
        }
    }
