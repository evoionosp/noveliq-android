package org.evoionosp.noveliq.domain.session

/**
 * Cover-art image caches. Covers are fetched per-user (authenticated), so both
 * the memory and the disk cache must be dropped on logout. Implemented where
 * the image loader lives (`:app`).
 */
fun interface CoverArtCache {
    suspend fun clear()
}
