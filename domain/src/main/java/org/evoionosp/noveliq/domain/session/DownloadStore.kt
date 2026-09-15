package org.evoionosp.noveliq.domain.session

/**
 * Offline downloads (episodes/audio kept on device). No-op until downloads
 * ship; the future implementation deletes every downloaded file here, and the
 * logout flow needs no change to pick it up.
 */
fun interface DownloadStore {
    suspend fun deleteAll()
}
