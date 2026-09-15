package org.evoionosp.noveliq.logout

import android.content.Context
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.domain.session.CoverArtCache

/**
 * Drops the cover-art caches on logout. Covers are fetched per-user
 * (authenticated), so both the memory and the disk cache go — otherwise the
 * next user would see the previous user's artwork.
 */
@Singleton
class CoilCoverArtCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @param:Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) : CoverArtCache {
        override suspend fun clear() {
            withContext(ioDispatcher) {
                val imageLoader = context.imageLoader
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            }
        }
    }
